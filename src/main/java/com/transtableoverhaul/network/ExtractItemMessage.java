package com.transtableoverhaul.network;

import java.util.function.Supplier;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/**
 * Client asks the server to transmute {@code amount} of a learned item out of the player's EMC.
 *
 * @param toInventory {@code true} to insert into the player's inventory, {@code false} to put it on the cursor
 */
public record ExtractItemMessage(int containerId, ItemStack stack, int amount, boolean toInventory) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(containerId);
        buf.writeItem(stack);
        buf.writeVarInt(amount);
        buf.writeBoolean(toInventory);
    }

    public static ExtractItemMessage decode(FriendlyByteBuf buf) {
        return new ExtractItemMessage(buf.readVarInt(), buf.readItem(), buf.readVarInt(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player != null && player.containerMenu instanceof TransmutationContainer menu && menu.containerId == containerId && !stack.isEmpty()) {
                TransmutationExtraction.extract(player, menu, stack, amount, toInventory);
            }
        });
        context.get().setPacketHandled(true);
    }
}
