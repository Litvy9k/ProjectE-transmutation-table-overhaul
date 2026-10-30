package com.transtableoverhaul.integration.jei;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.client.screen.TransmutationCraftingScreen;
import com.transtableoverhaul.client.screen.TransmutationTerminalScreen;
import com.transtableoverhaul.registry.ModRegistries;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IClickableIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@JeiPlugin
public class TerminalJeiPlugin implements IModPlugin {

    @Nullable
    private static IJeiRuntime runtime;

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return TransTableOverhaul.rl("jei_plugin");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        IIngredientManager ingredients = registration.getJeiHelpers().getIngredientManager();
        registration.addGuiContainerHandler(TransmutationTerminalScreen.class, new TerminalHandler<>(ingredients));
        registration.addGuiContainerHandler(TransmutationCraftingScreen.class, new TerminalHandler<>(ingredients));
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new CraftingTransferHandler(registration.getTransferHelper()), RecipeTypes.CRAFTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModRegistries.CRAFTING_TABLE_ITEM.get(), RecipeTypes.CRAFTING);
    }

    private record TerminalHandler<T extends TransmutationTerminalScreen>(IIngredientManager ingredients) implements IGuiContainerHandler<T> {

        @NotNull
        @Override
        public List<Rect2i> getGuiExtraAreas(@NotNull T screen) {
            return screen.getExtraAreas();
        }

        @NotNull
        @Override
        public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(@NotNull T screen, double mouseX, double mouseY) {
            TransmutationTerminalScreen.GridHit hit = screen.getGridHit(mouseX, mouseY);
            if (hit == null) {
                return Optional.empty();
            }
            return ingredients.createClickableIngredient(VanillaTypes.ITEM_STACK, hit.stack(), hit.area(), true).map(ingredient -> ingredient);
        }
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }

    @Nullable
    public static String getSearchText() {
        return runtime == null ? null : runtime.getIngredientFilter().getFilterText();
    }

    public static void setSearchText(String text) {
        if (runtime != null) {
            runtime.getIngredientFilter().setFilterText(text);
        }
    }
}
