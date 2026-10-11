package dev.smarteasybuilder.schematic;

import dev.smarteasybuilder.SmartEasyBuilder;
import dev.smarteasybuilder.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Method;

/**
 * Litematica-Anbindung per Reflection (kein Compile-Zeit-Bezug zu Litematica/MaLiLib).
 *
 * UNGEPRUEFT gegen die konkrete Litematica-Version fuer 1.21.10. Verwendete Stellen:
 *  - fi.dy.masa.litematica.world.SchematicWorldHandler#getSchematicWorld() (static) -> World
 *  - fi.dy.masa.litematica.config.Configs$Generic.EASY_PLACE_MODE (IConfigBoolean#getBooleanValue)
 *  - fi.dy.masa.litematica.data.DataManager#getRenderLayerRange() -> LayerRange#isPositionWithinRange(BlockPos)
 * Schlaegt eine optionale Stelle fehl, wird sie ignoriert; schlaegt die erste fehl, meldet die Mod
 * "inkompatibel" statt abzustuerzen.
 */
public final class LitematicaSource implements SchematicSource {
    private final Method getSchematicWorld;
    private Object easyPlaceConfig;
    private Method getBooleanValue;
    private Method getLayerRange;

    private LitematicaSource(Method getSchematicWorld) {
        this.getSchematicWorld = getSchematicWorld;
    }

    public static LitematicaSource create() {
        try {
            Class<?> handler = Class.forName("fi.dy.masa.litematica.world.SchematicWorldHandler");
            LitematicaSource s = new LitematicaSource(handler.getMethod("getSchematicWorld"));
            try {
                Class<?> generic = Class.forName("fi.dy.masa.litematica.config.Configs$Generic");
                s.easyPlaceConfig = generic.getField("EASY_PLACE_MODE").get(null);
                s.getBooleanValue = Class.forName("fi.dy.masa.malilib.config.IConfigBoolean")
                        .getMethod("getBooleanValue");
            } catch (ReflectiveOperationException | LinkageError e) {
                SmartEasyBuilder.LOG.warn("Litematica EASY_PLACE_MODE nicht lesbar: {}", e.toString());
                s.easyPlaceConfig = null;
            }
            try {
                s.getLayerRange = Class.forName("fi.dy.masa.litematica.data.DataManager")
                        .getMethod("getRenderLayerRange");
            } catch (ReflectiveOperationException | LinkageError e) {
                SmartEasyBuilder.LOG.warn("Litematica LayerRange nicht lesbar: {}", e.toString());
            }
            return s;
        } catch (ReflectiveOperationException | LinkageError e) {
            SmartEasyBuilder.LOG.error("Litematica-Schematic-API nicht gefunden.", e);
            return null;
        }
    }

    private World world() {
        try {
            return (World) getSchematicWorld.invoke(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @Override
    public boolean isReady() {
        return world() != null;
    }

    @Override
    public Text problem() {
        return Text.translatable("seb.error.no_schematic");
    }

    @Override
    public BlockState getState(BlockPos pos) {
        World w = world();
        if (w == null) {
            return null;
        }
        if (ModConfig.get().respectLayerRange && !inLayerRange(pos)) {
            return null;
        }
        return w.getBlockState(pos);
    }

    private boolean inLayerRange(BlockPos pos) {
        if (getLayerRange == null) {
            return true;
        }
        try {
            Object range = getLayerRange.invoke(null);
            if (range == null) {
                return true;
            }
            Method m = range.getClass().getMethod("isPositionWithinRange", BlockPos.class);
            return (Boolean) m.invoke(range, pos);
        } catch (ReflectiveOperationException | RuntimeException e) {
            getLayerRange = null; // nicht mehr versuchen
            return true;
        }
    }

    @Override
    public boolean litematicaEasyPlaceActive() {
        if (easyPlaceConfig == null || getBooleanValue == null) {
            return false;
        }
        try {
            return (Boolean) getBooleanValue.invoke(easyPlaceConfig);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }
}
