package com.transtableoverhaul.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<SortOrder> SORT_ORDER;
    public static final ModConfigSpec.EnumValue<SortDirection> SORT_DIRECTION;
    public static final ModConfigSpec.EnumValue<ViewMode> VIEW_MODE;
    public static final ModConfigSpec.EnumValue<TerminalSize> TERMINAL_SIZE;
    public static final ModConfigSpec.BooleanValue SYNC_SEARCH;
    public static final ModConfigSpec.BooleanValue AUTO_FOCUS_SEARCH;
    public static final ModConfigSpec.BooleanValue REMEMBER_SEARCH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Transmutation terminal settings. The sort/view/size values are also changed by the buttons in the terminal.")
                .push("terminal");
        SORT_ORDER = builder.comment("What the item grid is sorted by.")
                .defineEnum("sortOrder", SortOrder.EMC);
        SORT_DIRECTION = builder.comment("Sort direction of the item grid.")
                .defineEnum("sortDirection", SortDirection.DESCENDING);
        VIEW_MODE = builder.comment("ALL shows every learned item (unaffordable ones greyed out); AFFORDABLE only shows what you can make right now.")
                .defineEnum("viewMode", ViewMode.ALL);
        TERMINAL_SIZE = builder.comment("Height of the item grid. TALL fills the screen.")
                .defineEnum("terminalSize", TerminalSize.TALL);
        SYNC_SEARCH = builder.comment("Keep the terminal search box in sync with JEI / EMI's search box.")
                .define("syncSearchWithRecipeViewer", false);
        AUTO_FOCUS_SEARCH = builder.comment("Focus the search box as soon as the terminal opens.")
                .define("autoFocusSearch", false);
        REMEMBER_SEARCH = builder.comment("Keep the last search text when reopening the terminal.")
                .define("rememberSearch", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {
    }

    public static <E extends Enum<E>> void set(ModConfigSpec.ConfigValue<E> value, E newValue) {
        value.set(newValue);
        value.save();
    }

    public enum SortOrder {
        NAME, EMC, MOD;

        public SortOrder next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum SortDirection {
        ASCENDING, DESCENDING;

        public SortDirection next() {
            return this == ASCENDING ? DESCENDING : ASCENDING;
        }
    }

    public enum ViewMode {
        ALL, AFFORDABLE;

        public ViewMode next() {
            return this == ALL ? AFFORDABLE : ALL;
        }
    }

    public enum TerminalSize {
        SMALL(4), MEDIUM(6), TALL(Integer.MAX_VALUE);

        public final int maxRows;

        TerminalSize(int maxRows) {
            this.maxRows = maxRows;
        }

        public TerminalSize next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }
}
