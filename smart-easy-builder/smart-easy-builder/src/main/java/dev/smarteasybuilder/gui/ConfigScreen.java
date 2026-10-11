package dev.smarteasybuilder.gui;

import dev.smarteasybuilder.config.ModConfig;
import dev.smarteasybuilder.core.Status;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Supplier;

/** Einfaches Einstellungsmenü (Vanilla-Widgets, keine Zusatz-Bibliothek). Änderungen wirken sofort. */
public final class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("seb.title"));
        this.parent = parent;
    }

    private static Text onOff(boolean v) {
        return Text.translatable(v ? "seb.on" : "seb.off").formatted(v ? Formatting.GREEN : Formatting.RED);
    }

    private void button(int x, int y, int w, Supplier<Text> label, Runnable action) {
        addDrawableChild(ButtonWidget.builder(label.get(), b -> {
            action.run();
            ModConfig.save();
            b.setMessage(label.get());
        }).dimensions(x, y, w, 20).build());
    }

    @Override
    protected void init() {
        ModConfig c = ModConfig.get();
        int colW = 190;
        int gap = 6;
        int left = width / 2 - colW - gap / 2;
        int right = width / 2 + gap / 2;
        int y = 28;
        int step = 24;

        button(left, y, colW, () -> Text.translatable("seb.opt.enabled", onOff(c.enabled)), () -> c.enabled = !c.enabled);
        button(right, y, colW, () -> Text.translatable("seb.opt.auto", onOff(c.autoPlace)), () -> c.autoPlace = !c.autoPlace);
        y += step;
        button(left, y, colW, () -> Text.translatable("seb.opt.hold", onOff(c.onlyWhileUseKeyHeld)),
                () -> c.onlyWhileUseKeyHeld = !c.onlyWhileUseKeyHeld);
        button(right, y, colW, () -> Text.translatable("seb.opt.interval", c.intervalMs + " ms"), c::cycleInterval);
        y += step;
        button(left, y, colW, () -> Text.translatable("seb.opt.pending", c.maxPendingPlacements),
                () -> c.maxPendingPlacements = c.maxPendingPlacements % 4 + 1);
        button(right, y, colW, () -> Text.translatable("seb.opt.hud", onOff(c.hudEnabled)), () -> c.hudEnabled = !c.hudEnabled);
        y += step;
        button(left, y, colW, () -> Text.translatable("seb.opt.custom", onOff(c.allowCustomItems)),
                () -> c.allowCustomItems = !c.allowCustomItems);
        button(right, y, colW, () -> Text.translatable("seb.opt.yield", onOff(c.yieldToLitematicaEasyPlace)),
                () -> c.yieldToLitematicaEasyPlace = !c.yieldToLitematicaEasyPlace);
        y += step;
        button(left, y, colW, () -> Text.translatable("seb.opt.suppress", onOff(c.suppressVanillaUse)),
                () -> c.suppressVanillaUse = !c.suppressVanillaUse);
        button(right, y, colW, () -> Text.translatable("seb.opt.invprotect", onOff(allMainProtected(c))), () -> {
            boolean target = !allMainProtected(c);
            for (int i = 9; i < 36; i++) {
                c.protectedSlots[i] = target;
            }
        });
        y += step + 12; // Platz fuer Beschriftung

        // Hotbar-Schutz: 9 kleine Buttons, rot = geschützt
        int bw = 20;
        int hx = width / 2 - (9 * (bw + 2)) / 2;
        for (int i = 0; i < 9; i++) {
            final int idx = i;
            button(hx + i * (bw + 2), y, bw,
                    () -> Text.literal(String.valueOf(idx + 1)).formatted(c.protectedSlots[idx] ? Formatting.RED : Formatting.GREEN),
                    () -> c.protectedSlots[idx] = !c.protectedSlots[idx]);
        }
        y += step + 4;

        addDrawableChild(ButtonWidget.builder(Text.translatable("seb.opt.reset"), b -> {
            ModConfig.reset();
            clearAndInit();
        }).dimensions(left, y, colW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("seb.opt.done"), b -> close())
                .dimensions(right, y, colW, 20).build());
    }

    private static boolean allMainProtected(ModConfig c) {
        for (int i = 9; i < 36; i++) {
            if (!c.protectedSlots[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("seb.opt.hotbar"), width / 2, 28 + 4 * 24 + 2, 0xFFCCCCCC);
        Text none = Text.translatable("seb.cfg.none");
        int y = height - 42;
        ctx.drawTextWithShadow(textRenderer, Text.translatable("seb.cfg.state", Status.state), 10, y, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.translatable("seb.cfg.needed", Status.needed != null ? Status.needed : none), 10, y + 11, 0xFFFFFFFF);
        ctx.drawTextWithShadow(textRenderer,
                Text.translatable("seb.cfg.error", Status.lastError != null ? Status.lastError : none), 10, y + 22, 0xFFFF7777);
    }

    @Override
    public void close() {
        ModConfig.save();
        client.setScreen(parent);
    }
}
