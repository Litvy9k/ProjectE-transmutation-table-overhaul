package com.transtableoverhaul.crafting;

import com.mojang.serialization.MapCodec;
import moze_intel.projecte.gameObjs.blocks.TransmutationStone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * ProjectE's transmutation table (same shape, facing and waterlogging) that opens the crafting terminal instead.
 */
public class TransmutationCraftingTableBlock extends TransmutationStone {

    private static final MapCodec<TransmutationCraftingTableBlock> CODEC = simpleCodec(TransmutationCraftingTableBlock::new);

    public TransmutationCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @NotNull
    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @NotNull
    @Override
    protected InteractionResult useWithoutItem(@NotNull BlockState state, Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hit) {
        if (!level.isClientSide) {
            player.openMenu(new SimpleMenuProvider((windowId, inventory, p) -> new TransmutationCraftingMenu(windowId, inventory, ContainerLevelAccess.create(level, pos)),
                    getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
