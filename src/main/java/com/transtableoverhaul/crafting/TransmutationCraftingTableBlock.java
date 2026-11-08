package com.transtableoverhaul.crafting;

import moze_intel.projecte.gameObjs.blocks.TransmutationStone;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

/**
 * ProjectE's transmutation table (same shape, facing and waterlogging) that opens the crafting terminal instead.
 */
public class TransmutationCraftingTableBlock extends TransmutationStone {

    public TransmutationCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
            @NotNull BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider((windowId, inventory, p) -> new TransmutationCraftingMenu(windowId, inventory, ContainerLevelAccess.create(level, pos)),
                    getName()), buf -> TransmutationCraftingMenu.writeOpenData(buf, null, 0));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
