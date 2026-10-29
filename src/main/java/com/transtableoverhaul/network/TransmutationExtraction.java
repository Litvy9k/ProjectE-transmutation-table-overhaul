package com.transtableoverhaul.network;

import java.math.BigInteger;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side counterpart of clicking an item in the terminal grid. Mirrors the checks ProjectE's own output slots make:
 * the item must be learned, have an EMC value, and the player must be able to afford it.
 */
public final class TransmutationExtraction {

    private TransmutationExtraction() {
    }

    public static void extract(ServerPlayer player, TransmutationContainer menu, ItemStack requested, int amount, boolean toInventory) {
        TransmutationInventory inv = menu.transmutationInventory;
        ItemInfo info = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(requested));
        if (!inv.provider.hasKnowledge(info)) {
            return;
        }
        long emc = IEMCProxy.INSTANCE.getValue(info);
        if (emc <= 0) {
            return;
        }
        ItemStack stack = info.createStack();
        amount = Mth.clamp(amount, 1, stack.getMaxStackSize());

        ItemStack carried = menu.getCarried();
        if (toInventory) {
            amount = Math.min(amount, roomInMainInventory(player.getInventory(), stack));
        } else if (!carried.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(carried, stack)) {
                return;
            }
            amount = Math.min(amount, carried.getMaxStackSize() - carried.getCount());
        }
        if (amount <= 0) {
            return;
        }

        BigInteger unit = BigInteger.valueOf(emc);
        BigInteger affordable = inv.getAvailableEmc().divide(unit);
        if (affordable.compareTo(BigInteger.valueOf(amount)) < 0) {
            amount = affordable.intValue();
            if (amount <= 0) {
                return;
            }
        }

        inv.removeEmc(unit.multiply(BigInteger.valueOf(amount)));
        stack.setCount(amount);
        if (toInventory) {
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                // Shouldn't happen as we checked for room, but never eat EMC for items we failed to hand out
                inv.addEmc(unit.multiply(BigInteger.valueOf(stack.getCount())));
            }
        } else if (carried.isEmpty()) {
            menu.setCarried(stack);
        } else {
            carried.grow(amount);
        }
        menu.broadcastChanges();
    }

    private static int roomInMainInventory(Inventory inventory, ItemStack stack) {
        int room = 0;
        for (ItemStack existing : inventory.items) {
            if (existing.isEmpty()) {
                room += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                room += Math.max(0, Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize(existing)) - existing.getCount());
            }
        }
        return room;
    }
}
