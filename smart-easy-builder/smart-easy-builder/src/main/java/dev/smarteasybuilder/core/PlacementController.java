package dev.smarteasybuilder.core;

import dev.smarteasybuilder.config.ModConfig;
import dev.smarteasybuilder.gui.ConfigScreen;
import dev.smarteasybuilder.input.Keys;
import dev.smarteasybuilder.schematic.SchematicSource;
import dev.smarteasybuilder.schematic.Schematics;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Zustandsmaschine des Platzierens. Läuft einmal pro Client-Tick (max. 1 Versuch pro Tick).
 *
 * Schutzmechanismen: max. N ausstehende Versuche (Standard 1), Intervall, Wartezeit nach Fehlern,
 * Sperrliste für wiederholt abgelehnte Positionen, Pause bei GUI/Tod/Welt-Wechsel/manuellem Abbauen.
 * Die Server-"Bestaetigung" ist heuristisch: Der Client sagt Platzierungen voraus; wird der Block
 * nach Ablauf der Settle-Zeit (abhaengig vom Ping) wieder zurückgesetzt, gilt das als Ablehnung.
 */
public final class PlacementController {
    private static final class Pending {
        final BlockPos pos;
        final BlockState wanted;
        final long sentTick;
        final int settle;

        Pending(BlockPos pos, BlockState wanted, long sentTick, int settle) {
            this.pos = pos;
            this.wanted = wanted;
            this.sentTick = sentTick;
            this.settle = settle;
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<BlockPos, Integer> FAILURES = new HashMap<>();
    private static final Map<BlockPos, Long> BLACKLIST = new HashMap<>();
    private static long tick;
    private static long lastAttemptNanos;
    private static int waitTicks;
    private static ClientPlayerEntity lastPlayer;
    private static ClientWorld lastWorld;
    private static boolean suppress;

    private PlacementController() {}

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(PlacementController::onTick);
    }

    /** Wird vom Mixin abgefragt (Vanilla-Rechtsklick unterdrücken). */
    public static boolean suppressVanillaUse() {
        return suppress;
    }

    private static void reset() {
        PENDING.clear();
        FAILURES.clear();
        BLACKLIST.clear();
        waitTicks = 0;
        lastAttemptNanos = 0;
        suppress = false;
        Status.clearError();
        Status.needed = null;
    }

    private static void onTick(MinecraftClient mc) {
        tick++;
        suppress = false;
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;

        // Welt-/Spielerwechsel (Dimension, Respawn, Verbindungsabbruch/-aufbau): Zustand verwerfen.
        if (p != lastPlayer || w != lastWorld) {
            reset();
            lastPlayer = p;
            lastWorld = w;
        }
        if (p == null || w == null || mc.getNetworkHandler() == null || mc.interactionManager == null) {
            Status.state("seb.state.off");
            return;
        }

        ModConfig cfg = ModConfig.get();
        handleKeys(mc, p, cfg);

        if (!cfg.enabled) {
            PENDING.clear();
            Status.state("seb.state.off");
            Status.needed = null;
            return;
        }
        updatePending(w, cfg);
        if (tick % 200 == 0) {
            BLACKLIST.values().removeIf(until -> until <= tick);
        }

        if (!p.isAlive()) {
            reset();
            Status.state("seb.state.dead");
            return;
        }
        if (mc.currentScreen != null) {
            Status.state("seb.state.paused_gui");
            return;
        }
        SchematicSource src = Schematics.get();
        if (!src.isReady()) {
            Status.state("seb.state.no_schematic");
            Status.error(src.problem());
            Status.needed = null;
            return;
        }
        if (cfg.yieldToLitematicaEasyPlace && src.litematicaEasyPlaceActive()) {
            Status.state("seb.state.yield");
            return;
        }

        Planner.Target t = Planner.findTarget(mc, src, Planner.reach(p), cfg.ignoredSet());
        if (t == null) {
            Status.state("seb.state.idle");
            Status.needed = null;
            return;
        }
        Item item = t.wanted().getBlock().asItem();
        if (!(item instanceof BlockItem)) {
            Status.needed = null;
            Status.error(Text.translatable("seb.error.no_item", t.wanted().getBlock().getName()));
            return;
        }
        Status.needed = item.getName();
        if (cfg.suppressVanillaUse && p.getMainHandStack().getItem() instanceof BlockItem) {
            suppress = true;
        }

        boolean useHeld = mc.options.useKey.isPressed();
        boolean triggered = useHeld || (cfg.autoPlace && !cfg.onlyWhileUseKeyHeld);
        if (!triggered) {
            Status.state("seb.state.ready");
            return;
        }
        // Manuelle Aktion hat Vorrang (Abbauen, Essen/Bogen).
        if (mc.options.attackKey.isPressed() || p.isUsingItem()) {
            Status.state("seb.state.paused_manual");
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (PENDING.size() >= cfg.maxPendingPlacements) {
            Status.state("seb.state.waiting");
            return;
        }
        for (Pending pd : PENDING) {
            if (pd.pos.equals(t.pos())) {
                Status.state("seb.state.waiting");
                return;
            }
        }
        Long until = BLACKLIST.get(t.pos());
        if (until != null && until > tick) {
            return;
        }
        long now = System.nanoTime();
        if (now - lastAttemptNanos < cfg.intervalMs * 1_000_000L) {
            return;
        }

        switch (InventoryManager.prepare(mc, item, cfg)) {
            case SWITCHED, SWAPPED -> {
                waitTicks = 1; // Server soll den Slotwechsel sehen, bevor wir platzieren
                return;
            }
            case MISSING -> {
                Status.error(Text.translatable("seb.error.missing", item.getName()));
                return;
            }
            case NO_SLOT -> {
                Status.error(Text.translatable("seb.error.no_slot"));
                return;
            }
            case READY -> { }
        }

        Planner.Plan plan = Planner.plan(mc, t, p.getMainHandStack(), Planner.reach(p), cfg.ignoredSet());
        if (plan.hit() == null) {
            Status.error(plan.error());
            return;
        }

        ActionResult res = mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, plan.hit());
        lastAttemptNanos = now;
        if (res.isAccepted()) {
            p.swingHand(Hand.MAIN_HAND);
            PENDING.add(new Pending(t.pos(), t.wanted(), tick, settleTicks(mc, p)));
            Status.clearError();
            Status.state("seb.state.placing");
        } else {
            Status.error(Text.translatable("seb.error.local_reject"));
            registerFailure(t.pos(), cfg);
        }
    }

    private static int settleTicks(MinecraftClient mc, ClientPlayerEntity p) {
        int latencyMs = 0;
        PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(p.getUuid());
        if (e != null) {
            latencyMs = e.getLatency();
        }
        return Math.min(40, 3 + latencyMs / 50);
    }

    private static void updatePending(ClientWorld w, ModConfig cfg) {
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext();) {
            Pending pd = it.next();
            if (tick - pd.sentTick < pd.settle) {
                continue;
            }
            it.remove();
            BlockState now = w.getBlockState(pd.pos);
            if (StateMatcher.matches(now, pd.wanted, cfg.ignoredSet())) {
                FAILURES.remove(pd.pos);
            } else {
                registerFailure(pd.pos, cfg); // Server hat zurückgesetzt bzw. abgelehnt
            }
        }
    }

    private static void registerFailure(BlockPos pos, ModConfig cfg) {
        int f = FAILURES.merge(pos, 1, Integer::sum);
        waitTicks = Math.max(waitTicks, Math.min(20, 2 * f)); // zunehmender Abstand statt Spam
        if (f >= cfg.maxRetriesPerPos) {
            BLACKLIST.put(pos, tick + cfg.blacklistTicks);
            FAILURES.remove(pos);
            Status.error(Text.translatable("seb.error.rejected", pos.toShortString()));
        }
    }

    private static void handleKeys(MinecraftClient mc, ClientPlayerEntity p, ModConfig cfg) {
        while (Keys.TOGGLE.wasPressed()) {
            cfg.enabled = !cfg.enabled;
            PENDING.clear();
            ModConfig.save();
            p.sendMessage(Text.translatable(cfg.enabled ? "seb.msg.on" : "seb.msg.off"), true);
            if (cfg.enabled && !Schematics.get().isReady()) {
                Status.error(Schematics.get().problem());
            }
        }
        while (Keys.AUTO.wasPressed()) {
            cfg.autoPlace = !cfg.autoPlace;
            ModConfig.save();
            p.sendMessage(Text.translatable(cfg.autoPlace ? "seb.msg.auto_on" : "seb.msg.auto_off"), true);
        }
        while (Keys.CONFIG.wasPressed()) {
            mc.setScreen(new ConfigScreen(null));
        }
    }
}
