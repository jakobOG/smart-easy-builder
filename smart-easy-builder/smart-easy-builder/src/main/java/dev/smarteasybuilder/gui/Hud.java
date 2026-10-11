package dev.smarteasybuilder.gui;

import dev.smarteasybuilder.config.ModConfig;
import dev.smarteasybuilder.core.Status;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/**
 * Kleine Status-Anzeige.
 * VERSIONSABHAENGIG: HudRenderCallback ist in neueren Fabric-API-Versionen als veraltet markiert
 * (Nachfolger: HudElementRegistry); es funktioniert aber weiterhin. Bei Bedarf umstellen.
 */
public final class Hud {
    private Hud() {}

    public static void init() {
        HudRenderCallback.EVENT.register(Hud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ModConfig cfg = ModConfig.get();
        if (!cfg.hudEnabled || mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        Text none = Text.translatable("seb.cfg.none");
        int x = 4;
        int y = 4;
        ctx.drawTextWithShadow(mc.textRenderer, Text.translatable("seb.title").append(": ")
                .append(Text.translatable(cfg.enabled ? "seb.on" : "seb.off")).append(" | "
                        + cfg.intervalMs + " ms" + (cfg.autoPlace && !cfg.onlyWhileUseKeyHeld ? " | auto" : "")),
                x, y, 0xFFFFFFFF);
        ctx.drawTextWithShadow(mc.textRenderer, Text.translatable("seb.cfg.state", Status.state), x, y + 10, 0xFFCCCCCC);
        ctx.drawTextWithShadow(mc.textRenderer,
                Text.translatable("seb.cfg.needed", Status.needed != null ? Status.needed : none), x, y + 20, 0xFFCCCCCC);
        if (Status.lastError != null) {
            ctx.drawTextWithShadow(mc.textRenderer, Status.lastError, x, y + 30, 0xFFFF5555);
        }
    }
}
