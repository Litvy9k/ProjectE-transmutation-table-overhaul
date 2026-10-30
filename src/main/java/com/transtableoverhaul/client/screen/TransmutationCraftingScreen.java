package com.transtableoverhaul.client.screen;

import com.transtableoverhaul.client.ClientConfig;
import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The terminal with an AE2 crafting terminal style 3x3 grid between the EMC strip and the player inventory.
 */
public class TransmutationCraftingScreen extends TransmutationTerminalScreen {

    private static final int CRAFTING_HEIGHT = 58;
    private static final int CRAFT_GRID_X = 26;
    private static final int RESULT_X = 124;
    private static final int BUTTON_X = 7;
    private static final int COST_X = 86;
    private static final int COST_Y = 44;
    private static final int COST_WIDTH = 102;
    private static final int COST_COLOR = 0x7A5A00;
    private static final int COST_TOO_HIGH_COLOR = 0xAA0000;
    private static final int PLACEHOLDER_TINT = 0x808B8B8B;

    private final TransmutationCraftingMenu craftingMenu;
    private final Slot[] gridSlots = new Slot[TransmutationCraftingMenu.GRID_SIZE];
    private boolean sentSourcePreference;

    // Recipe lookups are too slow for every frame, only redo them when the grid changes
    private final ItemStack[] recipeCacheKey = new ItemStack[TransmutationCraftingMenu.GRID_SIZE];
    private int recipeCacheMask = -1;
    private boolean recipeCacheValue;

    public TransmutationCraftingScreen(TransmutationCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.craftingMenu = menu;
        for (Slot slot : menu.slots) {
            if (menu.isCraftingGrid(slot.container)) {
                gridSlots[slot.getContainerSlot()] = slot;
            }
        }
    }

    @Override
    protected int craftingHeight() {
        return CRAFTING_HEIGHT;
    }

    @Override
    public void init() {
        super.init();
        int top = topPos + craftingTop();
        addRenderableWidget(new TerminalButton(leftPos + BUTTON_X, top + 1, () -> TerminalTexture.ICON_CLEAR,
                () -> Component.translatable(LANG + "crafting.clear"), this::clearGrid));
        addRenderableWidget(new TerminalButton(leftPos + BUTTON_X, top + 19,
                () -> craftingMenu.isPreferInventory() ? TerminalTexture.ICON_SOURCE_INVENTORY : TerminalTexture.ICON_SOURCE_TABLE,
                this::sourceTooltip, this::toggleSource));
        if (!sentSourcePreference) {
            sentSourcePreference = true;
            setSource(ClientConfig.CRAFTING_PREFER_INVENTORY.get());
        }
    }

    private Component sourceTooltip() {
        String value = craftingMenu.isPreferInventory() ? "inventory" : "table";
        return Component.translatable(LANG + "crafting.source")
                .append("\n")
                .append(Component.translatable(LANG + "crafting.source." + value).withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.translatable(LANG + "crafting.source.desc").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void toggleSource() {
        boolean preferInventory = !craftingMenu.isPreferInventory();
        setSource(preferInventory);
        ClientConfig.CRAFTING_PREFER_INVENTORY.set(preferInventory);
        ClientConfig.CRAFTING_PREFER_INVENTORY.save();
    }

    private void setSource(boolean preferInventory) {
        craftingMenu.setPreferInventory(preferInventory);
        sendButton(preferInventory ? TransmutationCraftingMenu.BUTTON_PREFER_INVENTORY : TransmutationCraftingMenu.BUTTON_PREFER_TABLE);
    }

    private void clearGrid() {
        sendButton(TransmutationCraftingMenu.BUTTON_CLEAR_GRID);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void layoutExtraSlot(Slot slot) {
        int top = craftingTop();
        if (craftingMenu.isCraftingGrid(slot.container)) {
            int index = slot.getContainerSlot();
            move(slot, CRAFT_GRID_X + (index % 3) * 18, top + 1 + (index / 3) * 18);
        } else if (craftingMenu.isResult(slot.container)) {
            move(slot, RESULT_X, top + 19);
        }
    }

    @Override
    protected void renderCraftingBackground(GuiGraphics graphics, int x, int y) {
        TerminalTexture.blit(graphics, TerminalTexture.CRAFTING, x, y);
    }

    /**
     * EMC paid per craft, or {@code null} when there is nothing to show: no placeholders, or no recipe.
     */
    @Nullable
    private CostState costState() {
        BigInteger cost = craftingMenu.placeholderCost();
        boolean anyPlaceholder = false;
        for (int i = 0; i < TransmutationCraftingMenu.GRID_SIZE; i++) {
            anyPlaceholder |= craftingMenu.isPlaceholder(i) && gridSlots[i].hasItem();
        }
        if (!anyPlaceholder || !gridHasRecipe()) {
            return null;
        }
        boolean affordable = cost != null && menu.transmutationInventory.getAvailableEmc().compareTo(cost) >= 0;
        return new CostState(cost == null ? BigInteger.ZERO : cost, affordable);
    }

    private record CostState(BigInteger emc, boolean affordable) {
    }

    private boolean gridHasRecipe() {
        int mask = 0;
        boolean changed = false;
        for (int i = 0; i < TransmutationCraftingMenu.GRID_SIZE; i++) {
            mask |= craftingMenu.isPlaceholder(i) ? 1 << i : 0;
            ItemStack stack = gridSlots[i].getItem();
            if (recipeCacheKey[i] == null || !ItemStack.matches(recipeCacheKey[i], stack)) {
                recipeCacheKey[i] = stack.copy();
                changed = true;
            }
        }
        if (changed || mask != recipeCacheMask) {
            recipeCacheMask = mask;
            recipeCacheValue = minecraft != null && minecraft.level != null && craftingMenu.hasRecipe(minecraft.level);
        }
        return recipeCacheValue;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        // Source marker on every filled grid slot, and placeholders drawn faded like AE2's ghost items
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        for (int i = 0; i < TransmutationCraftingMenu.GRID_SIZE; i++) {
            Slot slot = gridSlots[i];
            if (!slot.hasItem()) {
                continue;
            }
            boolean placeholder = craftingMenu.isPlaceholder(i);
            if (placeholder) {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, PLACEHOLDER_TINT);
            }
            TerminalTexture.blit(graphics, placeholder ? TerminalTexture.MARKER_EMC : TerminalTexture.MARKER_INVENTORY, slot.x - 1, slot.y - 1);
        }
        graphics.pose().popPose();

        CostState cost = costState();
        if (cost != null) {
            MutableComponent value = TransmutationEMCFormatter.formatEMC(cost.emc()).copy()
                    .withColor(cost.affordable() ? COST_COLOR : COST_TOO_HIGH_COLOR);
            Component text = Component.translatable(LANG + "crafting.cost", value);
            graphics.drawString(font, clip(text, COST_WIDTH), COST_X, craftingTop() + COST_Y, 0x404040, false);
        }
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty()) {
            CostState cost = costState();
            if (cost != null && isHovering(COST_X, craftingTop() + COST_Y - 1, COST_WIDTH, 10, mouseX, mouseY)) {
                List<FormattedCharSequence> lines = new ArrayList<>();
                lines.add(Component.translatable(LANG + "crafting.cost.exact", EMCHelper.formatEmc(cost.emc())).getVisualOrderText());
                if (!cost.affordable()) {
                    lines.add(Component.translatable(LANG + "crafting.cost.too_high").withStyle(ChatFormatting.RED).getVisualOrderText());
                }
                lines.addAll(font.split(Component.translatable(LANG + "crafting.cost.desc").withStyle(ChatFormatting.DARK_GRAY), 180));
                graphics.renderTooltip(font, lines, mouseX, mouseY);
                return;
            }
            if (hoveredSlot != null && hoveredSlot.hasItem() && craftingMenu.isCraftingGrid(hoveredSlot.container)) {
                renderGridSlotTooltip(graphics, hoveredSlot, mouseX, mouseY);
                return;
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderGridSlotTooltip(GuiGraphics graphics, Slot slot, int mouseX, int mouseY) {
        ItemStack stack = slot.getItem();
        List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(stack));
        if (craftingMenu.isPlaceholder(slot.getContainerSlot())) {
            lines.add(Component.translatable(LANG + "crafting.placeholder").withStyle(ChatFormatting.GOLD));
            lines.add(Component.translatable(LANG + "crafting.placeholder.desc").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            lines.add(Component.translatable(LANG + "crafting.from_inventory").withStyle(ChatFormatting.GRAY));
        }
        graphics.renderTooltip(font, lines, stack.getTooltipImage(), stack, mouseX, mouseY);
    }
}
