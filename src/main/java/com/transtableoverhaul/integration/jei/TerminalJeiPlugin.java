package com.transtableoverhaul.integration.jei;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.client.screen.TransmutationTerminalScreen;
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
        registration.addGuiContainerHandler(TransmutationTerminalScreen.class, new IGuiContainerHandler<>() {
            @NotNull
            @Override
            public List<Rect2i> getGuiExtraAreas(@NotNull TransmutationTerminalScreen screen) {
                return screen.getExtraAreas();
            }

            @NotNull
            @Override
            public Optional<IClickableIngredient<?>> getClickableIngredientUnderMouse(@NotNull TransmutationTerminalScreen screen, double mouseX, double mouseY) {
                TransmutationTerminalScreen.GridHit hit = screen.getGridHit(mouseX, mouseY);
                if (hit == null) {
                    return Optional.empty();
                }
                return ingredients.createClickableIngredient(VanillaTypes.ITEM_STACK, hit.stack(), hit.area(), true).map(ingredient -> ingredient);
            }
        });
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
