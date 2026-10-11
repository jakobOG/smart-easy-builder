package dev.smarteasybuilder.core;

import dev.smarteasybuilder.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

/**
 * Item-Suche und Hotbar-Verwaltung. Es werden nur ungeschützte Slots verändert; nichts wird
 * gedroppt. Das Verschieben ist ein normaler Slot-Tausch (SWAP) im Spielerinventar.
 */
public final class InventoryManager {
    public enum Result { READY, SWITCHED, SWAPPED, MISSING, NO_SLOT }

    private InventoryManager() {}

    /** Passendes Item ohne wichtige Komponenten (Name, Verzauberungen, Custom-Daten ...). */
    public static boolean usable(ItemStack s, Item item, ModConfig cfg) {
        if (s.isEmpty() || !s.isOf(item)) {
            return false;
        }
        return cfg.allowCustomItems || s.getComponentChanges().isEmpty();
    }

    /**
     * Stellt sicher, dass das Item in der Haupthand liegt. Bei SWITCHED/SWAPPED muss der Aufrufer
     * mindestens einen Tick warten, bis der Server den Slotwechsel kennt.
     */
    public static Result prepare(MinecraftClient mc, Item item, ModConfig cfg) {
        ClientPlayerEntity p = mc.player;
        PlayerInventory inv = p.getInventory();
        int sel = inv.getSelectedSlot(); // 1.21.5+: Getter/Setter statt oeffentlichem Feld

        if (usable(inv.getStack(sel), item, cfg)) {
            return Result.READY; // kein unnötiger Wechsel
        }
        for (int i = 0; i < 9; i++) {
            if (!cfg.protectedSlots[i] && usable(inv.getStack(i), item, cfg)) {
                inv.setSelectedSlot(i);
                return Result.SWITCHED;
            }
        }
        int from = -1;
        for (int i = 9; i < 36; i++) {
            if (!cfg.protectedSlots[i] && usable(inv.getStack(i), item, cfg)) {
                from = i;
                break;
            }
        }
        if (from < 0) {
            return Result.MISSING;
        }
        int to = -1;
        for (int i = 0; i < 9; i++) { // bevorzugt leerer, ungeschützter Slot
            if (!cfg.protectedSlots[i] && inv.getStack(i).isEmpty()) {
                to = i;
                break;
            }
        }
        if (to < 0 && !cfg.protectedSlots[sel]) {
            to = sel;
        }
        for (int i = 0; to < 0 && i < 9; i++) {
            if (!cfg.protectedSlots[i]) {
                to = i;
            }
        }
        if (to < 0) {
            return Result.NO_SLOT;
        }
        // Slot-IDs des PlayerScreenHandler: 9-35 Hauptinventar (= Inventar-Index), Button = Hotbar-Index.
        mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, from, to, SlotActionType.SWAP, p);
        inv.setSelectedSlot(to);
        return Result.SWAPPED;
    }
}
