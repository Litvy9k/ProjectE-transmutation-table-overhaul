package com.transtableoverhaul.crafting;

import java.util.List;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * The two places crafting ingredients come from: the player's learned items (paid with EMC per craft) and the
 * player's inventory.
 */
public final class CraftingSources {

    private CraftingSources() {
    }

    /**
     * @return EMC value of {@code template} if the player has learned it, otherwise 0
     */
    public static long learnedValue(TransmutationInventory inv, ItemStack template) {
        if (template.isEmpty()) {
            return 0;
        }
        ItemInfo info = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(template));
        return inv.provider.hasKnowledge(info) ? IEMCProxy.INSTANCE.getValue(info) : 0;
    }

    /**
     * Single item standing in for a learned ingredient in the grid.
     */
    public static ItemStack placeholder(ItemStack template) {
        return IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(template)).createStack();
    }

    public static boolean inInventory(Player player, ItemStack template) {
        return countInInventory(player, template) > 0;
    }

    public static int countInInventory(Player player, ItemStack template) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Removes up to {@code max} of {@code template} from the player's main inventory.
     */
    public static ItemStack takeFromInventory(Player player, ItemStack template, int max) {
        ItemStack taken = ItemStack.EMPTY;
        List<ItemStack> items = player.getInventory().items;
        for (int i = 0; i < items.size() && taken.getCount() < max; i++) {
            ItemStack stack = items.get(i);
            if (ItemStack.isSameItemSameComponents(stack, template)) {
                ItemStack split = stack.split(max - taken.getCount());
                if (taken.isEmpty()) {
                    taken = split;
                } else {
                    taken.grow(split.getCount());
                }
            }
        }
        return taken;
    }

    public static boolean isAvailable(Player player, TransmutationInventory inv, ItemStack template) {
        return inInventory(player, template) || learnedValue(inv, template) > 0;
    }
}
