package dev.smarteasybuilder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.smarteasybuilder.SmartEasyBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistente Konfiguration (JSON in config/smart_easy_builder.json).
 * Enthält bewusst keine Minecraft-Klassen, damit sanitize() ohne Spiel testbar ist.
 */
public final class ModConfig {
    /** Auswählbare Intervalle in ms. 0 = so schnell wie ein kontrollierter Tick erlaubt. */
    public static final int[] INTERVALS_MS = {0, 20, 50, 100, 250, 500, 1000};

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModConfig instance = new ModConfig();

    // Sicherheits-orientierte Standardwerte: standardmäßig AUS, nur bei gehaltener Taste, 100 ms.
    public boolean enabled = false;
    public boolean autoPlace = false;
    public boolean onlyWhileUseKeyHeld = true;
    public int intervalMs = 100;
    public int maxPendingPlacements = 1;
    public int maxRetriesPerPos = 3;
    public int blacklistTicks = 100;
    public boolean hudEnabled = true;
    public boolean allowCustomItems = false;
    public boolean yieldToLitematicaEasyPlace = true;
    public boolean suppressVanillaUse = true;
    public boolean respectLayerRange = true;
    /** Index 0-8 = Hotbar, 9-35 = Hauptinventar. true = geschützt (nie verschieben/befüllen). */
    public boolean[] protectedSlots = new boolean[36];
    /**
     * Blockzustands-Properties, die beim Soll/Ist-Vergleich ignoriert werden, weil sie vom
     * Spiel nachbarschaftsabhängig berechnet werden oder nicht beim Platzieren wählbar sind.
     */
    public List<String> ignoredProperties = defaultIgnored();

    private transient Set<String> ignoredCache;

    private static List<String> defaultIgnored() {
        return new ArrayList<>(List.of("waterlogged", "powered", "lit", "triggered", "power", "shape",
                "north", "east", "south", "west", "up", "down", "distance", "persistent"));
    }

    public static ModConfig get() {
        return instance;
    }

    public Set<String> ignoredSet() {
        if (ignoredCache == null) {
            ignoredCache = new HashSet<>(ignoredProperties);
        }
        return ignoredCache;
    }

    /** Fängt ungültige Werte ab (manuell editierte oder beschädigte Dateien). */
    public void sanitize() {
        int best = INTERVALS_MS[0];
        for (int v : INTERVALS_MS) {
            if (Math.abs(v - intervalMs) < Math.abs(best - intervalMs)) {
                best = v;
            }
        }
        intervalMs = best;
        maxPendingPlacements = Math.max(1, Math.min(4, maxPendingPlacements));
        maxRetriesPerPos = Math.max(1, Math.min(10, maxRetriesPerPos));
        blacklistTicks = Math.max(20, Math.min(1200, blacklistTicks));
        if (protectedSlots == null || protectedSlots.length != 36) {
            boolean[] fixed = new boolean[36];
            if (protectedSlots != null) {
                System.arraycopy(protectedSlots, 0, fixed, 0, Math.min(36, protectedSlots.length));
            }
            protectedSlots = fixed;
        }
        if (ignoredProperties == null) {
            ignoredProperties = defaultIgnored();
        }
        ignoredCache = null;
    }

    /** Nächstes Intervall aus der Preset-Liste (zyklisch). */
    public void cycleInterval() {
        for (int i = 0; i < INTERVALS_MS.length; i++) {
            if (INTERVALS_MS[i] == intervalMs) {
                intervalMs = INTERVALS_MS[(i + 1) % INTERVALS_MS.length];
                return;
            }
        }
        intervalMs = INTERVALS_MS[0];
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("smart_easy_builder.json");
    }

    public static void load() {
        Path p = path();
        if (Files.exists(p)) {
            try {
                ModConfig loaded = GSON.fromJson(Files.readString(p), ModConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (IOException | RuntimeException e) {
                SmartEasyBuilder.LOG.error("Konfiguration unlesbar, verwende Standardwerte.", e);
                instance = new ModConfig();
            }
        }
        instance.sanitize();
        save();
    }

    public static void save() {
        try {
            instance.sanitize();
            Files.writeString(path(), GSON.toJson(instance));
        } catch (IOException e) {
            SmartEasyBuilder.LOG.error("Konfiguration konnte nicht gespeichert werden.", e);
        }
    }

    public static void reset() {
        instance = new ModConfig();
        save();
    }
}
