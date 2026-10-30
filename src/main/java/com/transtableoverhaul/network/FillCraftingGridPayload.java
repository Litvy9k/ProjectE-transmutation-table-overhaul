package com.transtableoverhaul.network;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * JEI recipe transfer into the crafting terminal's grid.
 *
 * @param slots for each of the 9 grid slots, the items that would satisfy it (empty list for an empty slot)
 * @param max   fill as many sets as possible instead of one
 */
public record FillCraftingGridPayload(int containerId, List<List<ItemStack>> slots, boolean max) implements CustomPacketPayload {

    public static final int MAX_ALTERNATIVES = 64;

    public static final Type<FillCraftingGridPayload> TYPE = new Type<>(TransTableOverhaul.rl("fill_crafting_grid"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FillCraftingGridPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FillCraftingGridPayload::containerId,
            ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_ALTERNATIVES)).apply(ByteBufCodecs.list(TransmutationCraftingMenu.GRID_SIZE)), FillCraftingGridPayload::slots,
            ByteBufCodecs.BOOL, FillCraftingGridPayload::max,
            FillCraftingGridPayload::new
    );

    @NotNull
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof TransmutationCraftingMenu menu
                && menu.containerId == containerId) {
            menu.fillGrid(player, slots, max);
        }
    }
}
