package com.transtableoverhaul.client;

import com.transtableoverhaul.integration.emi.EmiSearch;
import com.transtableoverhaul.integration.jei.TerminalJeiPlugin;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * Talks to whichever recipe viewer is installed. The integration classes are only touched when their mod is loaded.
 */
public final class RecipeViewerSync {

    private static final boolean EMI = ModList.get().isLoaded("emi");
    private static final boolean JEI = ModList.get().isLoaded("jei");

    private RecipeViewerSync() {
    }

    public static boolean isAvailable() {
        return EMI || JEI;
    }

    @Nullable
    public static String getSearchText() {
        if (EMI) {
            return EmiSearch.get();
        } else if (JEI) {
            return TerminalJeiPlugin.getSearchText();
        }
        return null;
    }

    public static void setSearchText(String text) {
        if (EMI) {
            EmiSearch.set(text);
        } else if (JEI) {
            TerminalJeiPlugin.setSearchText(text);
        }
    }
}
