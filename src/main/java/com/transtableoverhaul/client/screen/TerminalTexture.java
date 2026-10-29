package com.transtableoverhaul.client.screen;

import com.transtableoverhaul.TransTableOverhaul;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Sprite layout of {@code textures/gui/terminal.png}. Keep in sync with {@code tools/gen_textures.py}.
 */
public final class TerminalTexture {

    public static final ResourceLocation LOCATION = TransTableOverhaul.rl("textures/gui/terminal.png");
    private static final int SIZE = 256;

    public record Sprite(int u, int v, int width, int height) {
    }

    public static final Sprite HEADER = new Sprite(0, 0, 195, 17);
    public static final Sprite ROW = new Sprite(0, 17, 195, 18);
    public static final Sprite BOTTOM = new Sprite(0, 35, 195, 118);
    public static final Sprite PANEL = new Sprite(196, 0, 50, 112);
    public static final Sprite THUMB = new Sprite(0, 160, 12, 15);
    public static final Sprite THUMB_DISABLED = new Sprite(12, 160, 12, 15);
    public static final Sprite BUTTON = new Sprite(24, 160, 16, 16);
    public static final Sprite BUTTON_HOVERED = new Sprite(40, 160, 16, 16);

    private static final int ICON_V = 176;
    public static final int ICON_SORT_NAME = 0;
    public static final int ICON_SORT_EMC = 1;
    public static final int ICON_SORT_MOD = 2;
    public static final int ICON_ASCENDING = 3;
    public static final int ICON_DESCENDING = 4;
    public static final int ICON_VIEW_ALL = 5;
    public static final int ICON_VIEW_AFFORDABLE = 6;
    public static final int ICON_SIZE_SMALL = 7;
    public static final int ICON_SIZE_MEDIUM = 8;
    public static final int ICON_SIZE_TALL = 9;
    public static final int ICON_SYNC_OFF = 10;
    public static final int ICON_SYNC_ON = 11;

    private TerminalTexture() {
    }

    public static void blit(GuiGraphics graphics, Sprite sprite, int x, int y) {
        graphics.blit(LOCATION, x, y, sprite.u(), sprite.v(), sprite.width(), sprite.height(), SIZE, SIZE);
    }

    public static void blitIcon(GuiGraphics graphics, int icon, int x, int y) {
        graphics.blit(LOCATION, x, y, icon * 16, ICON_V, 16, 16, SIZE, SIZE);
    }
}
