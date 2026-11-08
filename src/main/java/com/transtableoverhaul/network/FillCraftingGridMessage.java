package com.transtableoverhaul.network;

import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * JEI recipe transfer into the crafting terminal's grid.
 *
 * @param slots for each of the 9 grid slots, the items that would satisfy it (empty list for an empty slot)
 * @param max   fill as many sets as possible instead of one
 */
public record FillCraftingGridMessage(int containerId, List<List<ItemStack>> slots, boolean max) {

    public static final int MAX_ALTERNATIVES = 64;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeVarInt(slots.size());
        for (List<ItemStack> alternatives : slots) {
            buf.writeVarInt(alternatives.size());
            alternatives.forEach(buf::writeItem);
        }
        buf.writeBoolean(max);
    }

    public static FillCraftingGridMessage decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int size = Math.min(buf.readVarInt(), TransmutationCraftingMenu.GRID_SIZE);
        List<List<ItemStack>> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int count = Math.min(buf.readVarInt(), MAX_ALTERNATIVES);
            List<ItemStack> alternatives = new ArrayList<>(count);
            for (int j = 0; j < count; j++) {
                ItemStack stack = buf.readItem();
                if (!stack.isEmpty()) {
                    alternatives.add(stack);
                }
            }
            slots.add(alternatives);
        }
        return new FillCraftingGridMessage(containerId, slots, buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.containerMenu instanceof TransmutationCraftingMenu menu && menu.containerId == containerId) {
                menu.fillGrid(player, slots, max);
            }
        });
        context.get().setPacketHandled(true);
    }
}
