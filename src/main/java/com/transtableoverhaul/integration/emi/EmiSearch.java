package com.transtableoverhaul.integration.emi;

import dev.emi.emi.api.EmiApi;

public final class EmiSearch {

    private EmiSearch() {
    }

    public static String get() {
        return EmiApi.getSearchText();
    }

    public static void set(String text) {
        EmiApi.setSearchText(text);
    }
}
