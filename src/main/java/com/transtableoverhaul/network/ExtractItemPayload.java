package com.transtableoverhaul.network;

import com.transtableoverhaul.TransTableOverhaul;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Client asks the server to transmute {@code amount} of a learned item out of the player's EMC.
 *
 * @param toInventory {@code true} to insert into the player's inventory, {@code false} to put it on the cursor
 */
public record ExtractItemPayload(int containerId, ItemStack stack, int amount, boolean toInventory) implements CustomPacketPayload {

    public static final Type<ExtractItemPayload> TYPE = new Type<>(TransTableOverhaul.rl("extract"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ExtractItemPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ExtractItemPayload::containerId,
            ItemStack.STREAM_CODEC, ExtractItemPayload::stack,
            ByteBufCodecs.VAR_INT, ExtractItemPayload::amount,
            ByteBufCodecs.BOOL, ExtractItemPayload::toInventory,
            ExtractItemPayload::new
    );

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TransmutationContainer menu
                && menu.containerId == containerId) {
            TransmutationExtraction.extract(player, menu, stack, amount, toInventory);
        }
    }
}
