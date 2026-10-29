package com.transtableoverhaul.client.screen;

import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Square 16x16 toolbar button with an icon from the terminal texture, like the setting buttons on an AE2 terminal.
 */
public class TerminalButton extends AbstractButton {

    private final IntSupplier icon;
    private final Supplier<Component> tooltip;
    private final Runnable action;

    public TerminalButton(int x, int y, IntSupplier icon, Supplier<Component> tooltip, Runnable action) {
        super(x, y, 16, 16, Component.empty());
        this.icon = icon;
        this.tooltip = tooltip;
        this.action = action;
        refreshTooltip();
    }

    private void refreshTooltip() {
        Component text = tooltip.get();
        setMessage(text);
        setTooltip(Tooltip.create(text));
    }

    @Override
    public void onPress() {
        action.run();
        refreshTooltip();
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        TerminalTexture.blit(graphics, isHoveredOrFocused() ? TerminalTexture.BUTTON_HOVERED : TerminalTexture.BUTTON, getX(), getY());
        TerminalTexture.blitIcon(graphics, icon.getAsInt(), getX(), getY());
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
