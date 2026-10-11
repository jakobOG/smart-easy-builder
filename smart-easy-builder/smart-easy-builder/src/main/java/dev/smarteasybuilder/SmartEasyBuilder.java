package dev.smarteasybuilder;

import dev.smarteasybuilder.config.ModConfig;
import dev.smarteasybuilder.core.PlacementController;
import dev.smarteasybuilder.gui.Hud;
import dev.smarteasybuilder.input.Keys;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Einstiegspunkt (nur Client). */
public final class SmartEasyBuilder implements ClientModInitializer {
    public static final String MOD_ID = "smart_easy_builder";
    public static final Logger LOG = LoggerFactory.getLogger("SmartEasyBuilder");

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        Keys.register();
        PlacementController.init();
        Hud.init();
        LOG.info("Smart Easy Builder initialisiert.");
    }
}
