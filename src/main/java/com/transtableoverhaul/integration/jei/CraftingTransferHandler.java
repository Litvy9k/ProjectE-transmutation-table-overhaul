package com.transtableoverhaul.integration.jei;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import com.transtableoverhaul.network.FillCraftingGridPayload;
import com.transtableoverhaul.registry.ModRegistries;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * JEI's "+" button for the crafting terminal. Ingredients can come from learned items (EMC) or the player's inventory,
 * so JEI's slot based transfer can't be used; the server picks and pulls the items in {@link TransmutationCraftingMenu#fillGrid}.
 */
public class CraftingTransferHandler implements IRecipeTransferHandler<TransmutationCraftingMenu, RecipeHolder<CraftingRecipe>> {

    private final IRecipeTransferHandlerHelper helper;

    public CraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @NotNull
    @Override
    public Class<? extends TransmutationCraftingMenu> getContainerClass() {
        return TransmutationCraftingMenu.class;
    }

    @NotNull
    @Override
    public Optional<MenuType<TransmutationCraftingMenu>> getMenuType() {
        return Optional.of(ModRegistries.CRAFTING_MENU.get());
    }

    @NotNull
    @Override
    public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(@NotNull TransmutationCraftingMenu menu, @NotNull RecipeHolder<CraftingRecipe> recipe,
            @NotNull IRecipeSlotsView recipeSlots, @NotNull Player player, boolean maxTransfer, boolean doTransfer) {
        List<IRecipeSlotView> inputs = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        if (inputs.size() > TransmutationCraftingMenu.GRID_SIZE) {
            return helper.createInternalError();
        }
        List<List<ItemStack>> grid = new ArrayList<>();
        List<IRecipeSlotView> missing = new ArrayList<>();
        for (IRecipeSlotView view : inputs) {
            List<ItemStack> alternatives = alternatives(view);
            if (!alternatives.isEmpty() && alternatives.stream().noneMatch(menu::canSupply)) {
                missing.add(view);
            }
            grid.add(alternatives);
        }
        if (!missing.isEmpty()) {
            return helper.createUserErrorForMissingSlots(Component.translatable("gui." + TransTableOverhaul.MODID + ".crafting.missing"), missing);
        }
        if (doTransfer) {
            PacketDistributor.sendToServer(new FillCraftingGridPayload(menu.containerId, grid, maxTransfer));
        }
        return null;
    }

    /**
     * The displayed item first (what the player is looking at), then the other items the slot cycles through.
     */
    private static List<ItemStack> alternatives(IRecipeSlotView view) {
        List<ItemStack> alternatives = new ArrayList<>();
        view.getDisplayedItemStack().ifPresent(alternatives::add);
        view.getItemStacks().forEach(stack -> {
            if (alternatives.size() < FillCraftingGridPayload.MAX_ALTERNATIVES && !stack.isEmpty()
                    && alternatives.stream().noneMatch(existing -> ItemStack.isSameItemSameComponents(existing, stack))) {
                alternatives.add(stack);
            }
        });
        return alternatives.stream().filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1)).toList();
    }
}
