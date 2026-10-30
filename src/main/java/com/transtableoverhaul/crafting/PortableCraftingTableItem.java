package com.transtableoverhaul.crafting;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Handheld Transmutation Crafting Table, like ProjectE's Transmutation Tablet is to its table.
 */
public class PortableCraftingTableItem extends Item {

    public PortableCraftingTableItem(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int selected = player.getInventory().selected;
            player.openMenu(new SimpleMenuProvider((windowId, inventory, p) -> new TransmutationCraftingMenu(windowId, inventory, hand, selected),
                    stack.getHoverName()), buf -> TransmutationCraftingMenu.writeOpenData(buf, hand, selected));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
