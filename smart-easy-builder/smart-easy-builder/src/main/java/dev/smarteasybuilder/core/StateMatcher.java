package dev.smarteasybuilder.core;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

import java.util.Objects;
import java.util.Set;

/** Vergleicht Blockzustände unter Ignorieren konfigurierter Properties. */
public final class StateMatcher {
    private StateMatcher() {}

    public static boolean matches(BlockState actual, BlockState wanted, Set<String> ignored) {
        if (actual.getBlock() != wanted.getBlock()) {
            return false;
        }
        for (Property<?> p : wanted.getProperties()) {
            if (ignored.contains(p.getName())) {
                continue;
            }
            if (!actual.contains(p) || !Objects.equals(actual.get(p), wanted.get(p))) {
                return false;
            }
        }
        return true;
    }
}
