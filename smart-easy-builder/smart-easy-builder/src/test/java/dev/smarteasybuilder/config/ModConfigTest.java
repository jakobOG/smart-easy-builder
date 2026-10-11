package dev.smarteasybuilder.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Reine Java-Tests ohne Minecraft-Start (Config-Validierung). */
class ModConfigTest {
    @Test
    void sanitizeSnapsIntervalAndClampsValues() {
        ModConfig c = new ModConfig();
        c.intervalMs = 70;
        c.maxPendingPlacements = 99;
        c.maxRetriesPerPos = -5;
        c.protectedSlots = new boolean[3];
        c.ignoredProperties = null;
        c.sanitize();
        assertEquals(50, c.intervalMs);
        assertEquals(4, c.maxPendingPlacements);
        assertEquals(1, c.maxRetriesPerPos);
        assertEquals(36, c.protectedSlots.length);
        assertTrue(c.ignoredSet().contains("waterlogged"));
    }

    @Test
    void cycleIntervalWalksThroughAllPresets() {
        ModConfig c = new ModConfig();
        c.intervalMs = 0;
        for (int i = 1; i < ModConfig.INTERVALS_MS.length; i++) {
            c.cycleInterval();
            assertEquals(ModConfig.INTERVALS_MS[i], c.intervalMs);
        }
        c.cycleInterval();
        assertEquals(0, c.intervalMs);
    }

    @Test
    void defaultsAreConservative() {
        ModConfig c = new ModConfig();
        assertFalse(c.enabled);
        assertFalse(c.autoPlace);
        assertTrue(c.onlyWhileUseKeyHeld);
        assertEquals(100, c.intervalMs);
    }
}
