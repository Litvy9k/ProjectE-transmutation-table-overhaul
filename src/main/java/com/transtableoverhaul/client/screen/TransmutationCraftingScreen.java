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
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

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

    private static final int RECIPE_BUTTON_X = 149;
    private static final int POPUP_COLUMNS = 9;
    private static final int POPUP_PADDING = 3;
    private static final int SELECTED_COLOR = 0xFF00A040;

    private final TransmutationCraftingMenu craftingMenu;
    private final Slot[] gridSlots = new Slot[TransmutationCraftingMenu.GRID_SIZE];
    private Slot resultSlot;
    private boolean sentSourcePreference;
    private TerminalButton recipeButton;
    private boolean recipePopupOpen;

    // Recipe lookups are too slow for every frame, only redo them when the grid changes
    private final ItemStack[] recipeCacheKey = new ItemStack[TransmutationCraftingMenu.GRID_SIZE];
    private List<RecipeOption> recipeOptions = List.of();

    /**
     * One recipe matching the grid, {@code index} being its position in {@link TransmutationCraftingMenu#matchingRecipes}.
     */
    private record RecipeOption(int index, ItemStack output) {
    }

    public TransmutationCraftingScreen(TransmutationCraftingMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.craftingMenu = menu;
        for (Slot slot : menu.slots) {
            if (menu.isCraftingGrid(slot.container)) {
                gridSlots[slot.getContainerSlot()] = slot;
            } else if (menu.isResult(slot.container)) {
                resultSlot = slot;
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
        recipeButton = addRenderableWidget(new TerminalButton(leftPos + RECIPE_BUTTON_X, top + 19, () -> TerminalTexture.ICON_RECIPES,
                () -> Component.translatable(LANG + "crafting.recipes")
                        .append("\n")
                        .append(Component.translatable(LANG + "crafting.recipes.desc").withStyle(ChatFormatting.GRAY)),
                () -> recipePopupOpen = !recipePopupOpen));
        updateRecipeButton();
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
        if (!anyPlaceholder || recipeOptions().isEmpty()) {
            return null;
        }
        boolean affordable = cost != null && menu.transmutationInventory.getAvailableEmc().compareTo(cost) >= 0;
        return new CostState(cost == null ? BigInteger.ZERO : cost, affordable);
    }

    private record CostState(BigInteger emc, boolean affordable) {
    }

    /**
     * Recipes matching the grid, recomputed only when the grid changes.
     */
    private List<RecipeOption> recipeOptions() {
        boolean changed = false;
        for (int i = 0; i < TransmutationCraftingMenu.GRID_SIZE; i++) {
            ItemStack stack = gridSlots[i].getItem();
            if (recipeCacheKey[i] == null || !ItemStack.matches(recipeCacheKey[i], stack)) {
                recipeCacheKey[i] = stack.copy();
                changed = true;
            }
        }
        if (changed) {
            List<RecipeOption> options = new ArrayList<>();
            if (minecraft != null && minecraft.level != null) {
                Level level = minecraft.level;
                CraftingInput input = craftingMenu.craftInput();
                List<RecipeHolder<CraftingRecipe>> recipes = craftingMenu.matchingRecipes(level);
                for (int i = 0; i < recipes.size(); i++) {
                    options.add(new RecipeOption(i, recipes.get(i).value().assemble(input, level.registryAccess())));
                }
            }
            recipeOptions = options;
        }
        return recipeOptions;
    }

    private void updateRecipeButton() {
        boolean several = recipeOptions().size() > 1;
        recipeButton.visible = several;
        if (!several) {
            recipePopupOpen = false;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        updateRecipeButton();
    }

    // region Recipe popup

    private int popupColumns() {
        return Math.min(recipeOptions.size(), POPUP_COLUMNS);
    }

    private int popupWidth() {
        return popupColumns() * 18 + 2 * POPUP_PADDING;
    }

    private int popupHeight() {
        return (recipeOptions.size() + POPUP_COLUMNS - 1) / POPUP_COLUMNS * 18 + 2 * POPUP_PADDING;
    }

    private int popupX() {
        return Math.max(leftPos + 2, recipeButton.getX() + 16 - popupWidth());
    }

    private int popupY() {
        return recipeButton.getY() - 3 - popupHeight();
    }

    private boolean isOverPopup(double mouseX, double mouseY) {
        return recipePopupOpen && mouseX >= popupX() && mouseX < popupX() + popupWidth() && mouseY >= popupY() && mouseY < popupY() + popupHeight();
    }

    private int popupCellAt(double mouseX, double mouseY) {
        if (!isOverPopup(mouseX, mouseY)) {
            return -1;
        }
        int column = (int) (mouseX - popupX() - POPUP_PADDING) / 18;
        int row = (int) (mouseY - popupY() - POPUP_PADDING) / 18;
        if (mouseX < popupX() + POPUP_PADDING || mouseY < popupY() + POPUP_PADDING || column >= popupColumns()) {
            return -1;
        }
        int index = row * POPUP_COLUMNS + column;
        return index < recipeOptions.size() ? index : -1;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (recipePopupOpen) {
            renderRecipePopup(graphics, mouseX, mouseY);
        }
    }

    private void renderRecipePopup(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = popupX();
        int y = popupY();
        int width = popupWidth();
        int height = popupHeight();
        int hovered = popupCellAt(mouseX, mouseY);
        ItemStack current = resultSlot.getItem();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFC6C6C6);
        for (int i = 0; i < recipeOptions.size(); i++) {
            ItemStack output = recipeOptions.get(i).output();
            int cellX = x + POPUP_PADDING + (i % POPUP_COLUMNS) * 18;
            int cellY = y + POPUP_PADDING + (i / POPUP_COLUMNS) * 18;
            graphics.fill(cellX, cellY, cellX + 18, cellY + 18, 0xFF373737);
            graphics.fill(cellX + 1, cellY + 1, cellX + 18, cellY + 18, 0xFFFFFFFF);
            graphics.fill(cellX + 1, cellY + 1, cellX + 17, cellY + 17, 0xFF8B8B8B);
            if (!current.isEmpty() && ItemStack.matches(output, current)) {
                graphics.renderOutline(cellX, cellY, 18, 18, SELECTED_COLOR);
            }
            graphics.renderItem(output, cellX + 1, cellY + 1);
            graphics.renderItemDecorations(font, output, cellX + 1, cellY + 1);
            if (i == hovered) {
                renderSlotHighlight(graphics, cellX + 1, cellY + 1, 0);
            }
        }
        if (hovered != -1) {
            graphics.renderTooltip(font, recipeOptions.get(hovered).output(), mouseX, mouseY);
        }
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (recipePopupOpen) {
            int cell = popupCellAt(mouseX, mouseY);
            if (cell != -1) {
                sendButton(TransmutationCraftingMenu.BUTTON_SELECT_RECIPE + recipeOptions.get(cell).index());
                recipePopupOpen = false;
                return true;
            }
            if (isOverPopup(mouseX, mouseY)) {
                return true;
            }
            if (!recipeButton.isMouseOver(mouseX, mouseY)) {
                recipePopupOpen = false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (recipePopupOpen && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            recipePopupOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // endregion

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
        if (isOverPopup(mouseX, mouseY)) {
            // The popup draws its own tooltip on top
            return;
        }
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
