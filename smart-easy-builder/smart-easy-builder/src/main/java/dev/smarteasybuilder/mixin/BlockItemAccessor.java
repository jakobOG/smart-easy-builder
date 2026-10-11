package dev.smarteasybuilder.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Macht BlockItem#getPlacementState (protected) aufrufbar. Damit simulieren wir lokal, welchen
 * Blockzustand Vanilla bei einem bestimmten Klick erzeugen WUERDE, ohne etwas zu senden.
 * VERSIONSABHAENGIG: Methodenname laut Yarn; bei anderer Version pruefen.
 */
@Mixin(BlockItem.class)
public interface BlockItemAccessor {
    @Invoker("getPlacementState")
    BlockState seb$getPlacementState(ItemPlacementContext context);
}
