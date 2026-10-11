package dev.smarteasybuilder.schematic;

import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Abstraktion der Soll-Blockzustände. Verändert die Schematic niemals. */
public interface SchematicSource {
    /** true, wenn aktuell eine nutzbare Schematic vorliegt. */
    boolean isReady();

    /** Erklärung, falls {@link #isReady()} false ist. */
    Text problem();

    /** Soll-Zustand oder null (keine Schematic / ausserhalb des aktiven Bereichs). */
    BlockState getState(BlockPos pos);

    /** true, wenn Litematicas eigener Easy-Place-Modus aktiv ist (Doppelplatzierung vermeiden). */
    boolean litematicaEasyPlaceActive();
}
