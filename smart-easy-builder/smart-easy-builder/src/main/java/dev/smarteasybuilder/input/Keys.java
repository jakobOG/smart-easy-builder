package dev.smarteasybuilder.input;

import dev.smarteasybuilder.SmartEasyBuilder;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Tastenbelegung (im Vanilla-Steuerungsmenü änderbar).
 * VERSIONSABHAENGIG: Seit 1.21.9 nimmt KeyBinding eine KeyBinding.Category statt eines Strings.
 */
public final class Keys {
    public static KeyBinding TOGGLE;
    public static KeyBinding AUTO;
    public static KeyBinding CONFIG;

    private Keys() {}

    public static void register() {
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of(SmartEasyBuilder.MOD_ID, "main"));
        TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smart_easy_builder.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, cat));
        AUTO = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smart_easy_builder.auto", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, cat));
        CONFIG = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.smart_easy_builder.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, cat));
    }
}
