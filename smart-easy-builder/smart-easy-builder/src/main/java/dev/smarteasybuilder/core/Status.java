package dev.smarteasybuilder.core;

import net.minecraft.text.Text;

/** Anzeigewerte für HUD und Einstellungsmenü (nur Client-Thread). */
public final class Status {
    public static Text state = Text.translatable("seb.state.off");
    public static Text needed = null;
    public static Text lastError = null;

    private Status() {}

    public static void state(String key) {
        state = Text.translatable(key);
    }

    public static void error(Text t) {
        lastError = t;
    }

    public static void clearError() {
        lastError = null;
    }
}
