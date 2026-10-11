package dev.smarteasybuilder.schematic;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

/** Wählt (einmalig) die Schematic-Quelle. */
public final class Schematics {
    private static SchematicSource source;

    private Schematics() {}

    public static SchematicSource get() {
        if (source == null) {
            if (!FabricLoader.getInstance().isModLoaded("litematica")) {
                source = new MissingSource(Text.translatable("seb.error.litematica_missing"));
            } else {
                SchematicSource s = LitematicaSource.create();
                source = s != null ? s
                        : new MissingSource(Text.translatable("seb.error.litematica_incompatible"));
            }
        }
        return source;
    }
}
