package dev.smarteasybuilder.core;

import dev.smarteasybuilder.mixin.BlockItemAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.BedPart;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import dev.smarteasybuilder.schematic.SchematicSource;

import java.util.Set;

/**
 * Bestimmt (1) welcher Schematic-Block anvisiert wird und (2) wie er mit einem ganz normalen
 * Vanilla-Klick platziert werden kann.
 *
 * Kernidee fuer Rotation/Hälften: Wir SIMULIEREN lokal den Vanilla-Platzierungsvorgang
 * (BlockItem#getPlacementState) fuer mehrere zulässige Klickpositionen und senden nur dann,
 * wenn das Ergebnis dem Soll-Zustand entspricht. Es werden keine Rotationen "erzwungen",
 * keine Spezialpakete gesendet und keine Serverregeln umgangen. Passt keine Variante, wird
 * gewarnt und NICHT platziert.
 */
public final class Planner {
    private Planner() {}

    public record Target(BlockPos pos, BlockState wanted) {}

    public record Plan(BlockHitResult hit, Text error) {
        static Plan ok(BlockHitResult h) { return new Plan(h, null); }
        static Plan fail(Text t) { return new Plan(null, t); }
    }

    public static double reach(ClientPlayerEntity p) {
        // Vanilla-Reichweite des Spielers (Attribut), kleiner Sicherheitsabzug.
        return Math.max(1.0, p.getBlockInteractionRange() - 0.05);
    }

    /** Teile, die automatisch mit dem Hauptblock entstehen (obere Türhälfte, Bettkopf). */
    static boolean isCompanionPart(BlockState s) {
        if (s.contains(Properties.DOUBLE_BLOCK_HALF)
                && s.get(Properties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return true;
        }
        return s.contains(Properties.BED_PART) && s.get(Properties.BED_PART) == BedPart.HEAD;
    }

    /** Erster noch fehlender Schematic-Block entlang des Blickstrahls, nie durch echte Blöcke. */
    public static Target findTarget(MinecraftClient mc, SchematicSource src, double reach, Set<String> ignored) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1.0F);
        Vec3d end = eye.add(look.multiply(reach));
        HitResult real = w.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
        double max = real.getType() == HitResult.Type.MISS ? reach : eye.distanceTo(real.getPos());

        BlockPos last = null;
        for (double d = 0.0; d <= max + 0.05; d += 0.05) {
            BlockPos pos = BlockPos.ofFloored(eye.add(look.multiply(d)));
            if (pos.equals(last)) {
                continue;
            }
            last = pos;
            BlockState wanted = src.getState(pos);
            if (wanted == null || wanted.isAir() || isCompanionPart(wanted)) {
                continue;
            }
            BlockState actual = w.getBlockState(pos);
            if (StateMatcher.matches(actual, wanted, ignored)) {
                continue; // bereits korrekt: nichts senden
            }
            if (actual.isAir() || actual.isReplaceable()) {
                return new Target(pos, wanted);
            }
            // anderer, fester Block: wird nie ersetzt
        }
        return null;
    }

    /** Sucht eine regelkonforme Klickposition, die den Soll-Zustand ergibt. */
    public static Plan plan(MinecraftClient mc, Target t, ItemStack held, double reach, Set<String> ignored) {
        ClientPlayerEntity p = mc.player;
        ClientWorld w = mc.world;
        if (!(held.getItem() instanceof BlockItem bi)) {
            return Plan.fail(Text.translatable("seb.error.not_block"));
        }
        Vec3d eye = p.getEyePos();
        boolean anySupport = false;
        boolean anyReachable = false;
        boolean anyMismatch = false;

        for (Direction d : Direction.values()) {
            BlockPos n = t.pos().offset(d);
            BlockState ns = w.getBlockState(n);
            if (ns.isAir() || ns.isReplaceable() || !ns.getFluidState().isEmpty() || isInteractive(ns)) {
                continue;
            }
            anySupport = true;
            Direction face = d.getOpposite(); // Seite von n, die zum Ziel zeigt
            double[] offsets = face.getAxis().isHorizontal() ? new double[] {0.0, 0.25, -0.25} : new double[] {0.0};
            for (double oy : offsets) {
                Vec3d hit = Vec3d.ofCenter(n).add(face.getOffsetX() * 0.5,
                        face.getOffsetY() * 0.5 + oy, face.getOffsetZ() * 0.5);
                if (eye.distanceTo(hit) > reach) {
                    continue;
                }
                anyReachable = true;
                if (!lineOfSight(w, p, eye, hit, n)) {
                    continue;
                }
                BlockHitResult bhr = new BlockHitResult(hit, face, n, false);
                ItemPlacementContext ctx = new ItemPlacementContext(p, Hand.MAIN_HAND, held, bhr);
                if (!ctx.canPlace()) {
                    continue;
                }
                BlockState result = ((BlockItemAccessor) bi).seb$getPlacementState(ctx);
                if (result == null) {
                    continue;
                }
                if (!StateMatcher.matches(result, t.wanted(), ignored)) {
                    anyMismatch = true;
                    continue;
                }
                if (!result.canPlaceAt(w, t.pos()) || !w.canPlace(result, t.pos(), ShapeContext.of(p))) {
                    continue;
                }
                return Plan.ok(bhr);
            }
        }
        if (!anySupport) {
            return Plan.fail(Text.translatable("seb.error.no_support"));
        }
        if (!anyReachable) {
            return Plan.fail(Text.translatable("seb.error.unreachable"));
        }
        if (anyMismatch) {
            return Plan.fail(Text.translatable("seb.error.rotation", t.wanted().getBlock().getName()));
        }
        return Plan.fail(Text.translatable("seb.error.blocked"));
    }

    /** Kein Platzieren durch Wände: der Strahl Auge -> Klickpunkt muss zuerst den Stützblock treffen. */
    private static boolean lineOfSight(ClientWorld w, ClientPlayerEntity p, Vec3d eye, Vec3d hit, BlockPos n) {
        Vec3d dir = hit.subtract(eye).normalize();
        Vec3d end = hit.add(dir.multiply(0.05));
        BlockHitResult r = w.raycast(new RaycastContext(eye, end,
                RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p));
        return r.getType() == HitResult.Type.BLOCK && r.getBlockPos().equals(n);
    }

    /** Blöcke, bei denen ein Rechtsklick eine Aktion/GUI auslöst statt zu platzieren. */
    private static boolean isInteractive(BlockState s) {
        return s.hasBlockEntity()
                || s.isIn(BlockTags.DOORS) || s.isIn(BlockTags.TRAPDOORS) || s.isIn(BlockTags.BUTTONS)
                || s.isIn(BlockTags.FENCE_GATES) || s.isIn(BlockTags.BEDS) || s.isIn(BlockTags.ANVIL)
                || s.isOf(Blocks.LEVER) || s.isOf(Blocks.CRAFTING_TABLE) || s.isOf(Blocks.NOTE_BLOCK)
                || s.isOf(Blocks.REPEATER) || s.isOf(Blocks.COMPARATOR);
    }
}
