package dev.smarteasybuilder.mixin;

import dev.smarteasybuilder.core.PlacementController;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Unterdrückt den normalen Rechtsklick, solange das Fadenkreuz auf ein Schematic-Ziel zeigt.
 * Sonst würde Vanilla zusätzlich einen (falschen) Block an der echten Blockfläche setzen.
 * VERSIONSABHAENGIG: Methodenname "doItemUse" laut Yarn.
 */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void seb$suppressVanillaUse(CallbackInfo ci) {
        if (PlacementController.suppressVanillaUse()) {
            ci.cancel();
        }
    }
}
