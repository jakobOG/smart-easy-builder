package dev.smarteasybuilder.schematic;

import net.minecraft.block.BlockState;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Platzhalter, wenn Litematica fehlt oder inkompatibel ist. Kein Absturz, nur Meldung. */
public final class MissingSource implements SchematicSource {
    private final Text problem;

    public MissingSource(Text problem) {
        this.problem = problem;
    }

    @Override public boolean isReady() { return false; }
    @Override public Text problem() { return problem; }
    @Override public BlockState getState(BlockPos pos) { return null; }
    @Override public boolean litematicaEasyPlaceActive() { return false; }
}
