package com.transtableoverhaul.client.screen;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.client.ClientConfig;
import com.transtableoverhaul.client.ClientConfig.SortDirection;
import com.transtableoverhaul.client.ClientConfig.ViewMode;
import com.transtableoverhaul.client.RecipeViewerSync;
import com.transtableoverhaul.client.search.SearchQuery;
import com.transtableoverhaul.network.ExtractItemPayload;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.gameObjs.container.inventory.TransmutationInventory;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotConsume;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotInput;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotLock;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotOutput;
import moze_intel.projecte.gameObjs.container.slots.transmutation.SlotUnlearn;
import moze_intel.projecte.gameObjs.gui.PEContainerScreen;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.TransmutationEMCFormatter;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * AE2 terminal style replacement for ProjectE's transmutation screen. Works on top of ProjectE's own
 * {@link TransmutationContainer}: its real slots (Klein stars, lock, burn, unlearn, player inventory) are moved into this
 * layout, its 16 paged output slots are hidden, and the scrollable grid takes items out through {@link ExtractItemPayload}.
 */
public class TransmutationTerminalScreen extends PEContainerScreen<TransmutationContainer> {

    private static final int WIDTH = 195;
    private static final int COLUMNS = 9;
    private static final int SLOT = 18;
    private static final int HEADER_HEIGHT = 17;
    private static final int BOTTOM_HEIGHT = 118;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 18;
    private static final int SCROLLBAR_X = 175;
    private static final int SCROLLBAR_Y = 17;
    private static final int SCROLLBAR_WIDTH = 12;
    private static final int THUMB_HEIGHT = 15;
    private static final int PANEL_X = WIDTH + 2;
    private static final int TOOLBAR_X = -18;
    private static final int TOOLBAR_Y = 8;
    private static final int EMC_BOX_X = 7;
    private static final int EMC_BOX_WIDTH = 124;
    private static final int MIN_ROWS = 3;
    private static final int SCREEN_MARGIN = 8;
    private static final int KNOWLEDGE_POLL_TICKS = 20;
    private static final int MESSAGE_FADE_PER_TICK = 4;
    private static final String LANG = "gui." + TransTableOverhaul.MODID + ".";

    private static String lastSearch = "";

    private final TransmutationInventory inv;
    private final List<TerminalButton> toolbar = new ArrayList<>();
    private EditBox searchBox;
    @Nullable
    private Slot consumeSlot;
    @Nullable
    private Slot lockSlot;
    private int rows = MIN_ROWS;
    private int scrollRow;
    private boolean draggingScrollbar;
    private boolean swallowRelease;
    private boolean firstInit = true;

    private Map<ItemInfo, TerminalEntry> entries = new HashMap<>();
    private List<TerminalEntry> visible = List.of();
    private SearchQuery query = SearchQuery.EMPTY;
    private boolean needsRefilter = true;
    private int knowledgeSize = -1;
    private int ticksSinceKnowledgeCheck;
    private int lastLearnFlag;
    private int lastUnlearnFlag;
    private BigInteger availableEmc;
    private ItemStack lastLock = ItemStack.EMPTY;

    public TransmutationTerminalScreen(TransmutationContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.inv = container.transmutationInventory;
        this.titleLabelX = 8;
        this.titleLabelY = 5;
        this.inventoryLabelX = 8;
        this.availableEmc = inv.getAvailableEmc();
        rebuildEntries(inv.provider.getKnowledge());
    }

    // region Layout

    private int stripTop() {
        return HEADER_HEIGHT + rows * SLOT;
    }

    @Override
    public void init() {
        int fittingRows = (height - HEADER_HEIGHT - BOTTOM_HEIGHT - 2 * SCREEN_MARGIN) / SLOT;
        rows = Math.max(MIN_ROWS, Math.min(ClientConfig.TERMINAL_SIZE.get().maxRows, fittingRows));
        imageWidth = WIDTH;
        imageHeight = HEADER_HEIGHT + rows * SLOT + BOTTOM_HEIGHT;
        inventoryLabelY = stripTop() + 25;
        String search = searchBox != null ? searchBox.getValue() : ClientConfig.REMEMBER_SEARCH.get() ? lastSearch : "";
        if (firstInit && ClientConfig.SYNC_SEARCH.get()) {
            String viewerText = RecipeViewerSync.getSearchText();
            if (viewerText != null) {
                search = viewerText;
            }
        }

        super.init();
        layoutSlots();

        searchBox = new EditBox(font, leftPos + 83, topPos + 5, 86, 9, Component.translatable(LANG + "search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(256);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setHint(Component.translatable(LANG + "search.hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setValue(search);
        searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(searchBox);
        query = SearchQuery.parse(search);
        needsRefilter = true;

        toolbar.clear();
        addToolbarButton(() -> switch (ClientConfig.SORT_ORDER.get()) {
                    case NAME -> TerminalTexture.ICON_SORT_NAME;
                    case EMC -> TerminalTexture.ICON_SORT_EMC;
                    case MOD -> TerminalTexture.ICON_SORT_MOD;
                }, () -> settingTooltip("sort_order", ClientConfig.SORT_ORDER.get()),
                () -> {
                    ClientConfig.set(ClientConfig.SORT_ORDER, ClientConfig.SORT_ORDER.get().next());
                    needsRefilter = true;
                });
        addToolbarButton(() -> ClientConfig.SORT_DIRECTION.get() == SortDirection.ASCENDING ? TerminalTexture.ICON_ASCENDING : TerminalTexture.ICON_DESCENDING,
                () -> settingTooltip("sort_direction", ClientConfig.SORT_DIRECTION.get()),
                () -> {
                    ClientConfig.set(ClientConfig.SORT_DIRECTION, ClientConfig.SORT_DIRECTION.get().next());
                    needsRefilter = true;
                });
        addToolbarButton(() -> ClientConfig.VIEW_MODE.get() == ViewMode.ALL ? TerminalTexture.ICON_VIEW_ALL : TerminalTexture.ICON_VIEW_AFFORDABLE,
                () -> settingTooltip("view_mode", ClientConfig.VIEW_MODE.get()),
                () -> {
                    ClientConfig.set(ClientConfig.VIEW_MODE, ClientConfig.VIEW_MODE.get().next());
                    needsRefilter = true;
                });
        addToolbarButton(() -> switch (ClientConfig.TERMINAL_SIZE.get()) {
                    case SMALL -> TerminalTexture.ICON_SIZE_SMALL;
                    case MEDIUM -> TerminalTexture.ICON_SIZE_MEDIUM;
                    case TALL -> TerminalTexture.ICON_SIZE_TALL;
                }, () -> settingTooltip("terminal_size", ClientConfig.TERMINAL_SIZE.get()),
                () -> {
                    ClientConfig.set(ClientConfig.TERMINAL_SIZE, ClientConfig.TERMINAL_SIZE.get().next());
                    rebuildWidgets();
                });
        if (RecipeViewerSync.isAvailable()) {
            addToolbarButton(() -> ClientConfig.SYNC_SEARCH.get() ? TerminalTexture.ICON_SYNC_ON : TerminalTexture.ICON_SYNC_OFF,
                    () -> settingTooltip("sync_search", ClientConfig.SYNC_SEARCH.get() ? "on" : "off"),
                    () -> {
                        ClientConfig.SYNC_SEARCH.set(!ClientConfig.SYNC_SEARCH.get());
                        ClientConfig.SYNC_SEARCH.save();
                        if (ClientConfig.SYNC_SEARCH.get()) {
                            RecipeViewerSync.setSearchText(searchBox.getValue());
                        }
                    });
        }

        if (firstInit) {
            firstInit = false;
            if (ClientConfig.AUTO_FOCUS_SEARCH.get()) {
                setInitialFocus(searchBox);
            }
        }
    }

    private void addToolbarButton(IntSupplier icon, Supplier<Component> tooltip, Runnable action) {
        int y = topPos + TOOLBAR_Y + toolbar.size() * SLOT;
        toolbar.add(addRenderableWidget(new TerminalButton(leftPos + TOOLBAR_X, y, icon, tooltip, action)));
    }

    private static Component settingTooltip(String setting, Object value) {
        String valueKey = value instanceof Enum<?> e ? e.name().toLowerCase(Locale.ROOT) : value.toString();
        return Component.translatable(LANG + "setting." + setting)
                .append("\n")
                .append(Component.translatable(LANG + "setting." + setting + "." + valueKey).withStyle(ChatFormatting.GRAY));
    }

    private void layoutSlots() {
        int stripTop = stripTop();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory) {
                int index = slot.getContainerSlot();
                if (index < Inventory.getSelectionSize()) {
                    move(slot, GRID_X + index * SLOT, stripTop + 94);
                } else if (index < Inventory.INVENTORY_SIZE) {
                    move(slot, GRID_X + (index % COLUMNS) * SLOT, stripTop + 36 + (index / COLUMNS - 1) * SLOT);
                }
            } else if (slot instanceof SlotInput) {
                int index = slot.getSlotIndex();
                move(slot, PANEL_X + 8 + (index % 2) * SLOT, 8 + (index / 2) * SLOT);
            } else if (slot instanceof SlotLock) {
                lockSlot = slot;
                move(slot, PANEL_X + 17, 88);
            } else if (slot instanceof SlotConsume) {
                consumeSlot = slot;
                move(slot, 134, stripTop + 4);
            } else if (slot instanceof SlotUnlearn) {
                move(slot, 152, stripTop + 4);
            } else if (slot instanceof SlotOutput) {
                // Replaced by the grid
                move(slot, -10_000, -10_000);
            }
        }
    }

    private static void move(Slot slot, int x, int y) {
        slot.x = x;
        slot.y = y;
    }

    /**
     * Areas outside the main panel that belong to this screen, for recipe viewers to stay clear of.
     */
    public List<Rect2i> getExtraAreas() {
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(leftPos + PANEL_X, topPos, TerminalTexture.PANEL.width(), TerminalTexture.PANEL.height()));
        if (!toolbar.isEmpty()) {
            areas.add(new Rect2i(leftPos + TOOLBAR_X, topPos + TOOLBAR_Y, 16, toolbar.size() * SLOT - 2));
        }
        return areas;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button)
                && getExtraAreas().stream().noneMatch(area -> area.contains((int) mouseX, (int) mouseY));
    }

    // endregion

    // region Data

    @Override
    protected void containerTick() {
        super.containerTick();
        boolean learned = inv.learnFlag > lastLearnFlag || inv.unlearnFlag > lastUnlearnFlag;
        if (learned || ++ticksSinceKnowledgeCheck >= KNOWLEDGE_POLL_TICKS) {
            ticksSinceKnowledgeCheck = 0;
            Set<ItemInfo> knowledge = inv.provider.getKnowledge();
            if (learned || knowledge.size() != knowledgeSize) {
                rebuildEntries(knowledge);
            }
        }
        // ProjectE's own screen counts these down per frame, we use them for the "learned" message in the title bar
        inv.learnFlag = Math.max(0, inv.learnFlag - MESSAGE_FADE_PER_TICK);
        inv.unlearnFlag = Math.max(0, inv.unlearnFlag - MESSAGE_FADE_PER_TICK);
        lastLearnFlag = inv.learnFlag;
        lastUnlearnFlag = inv.unlearnFlag;

        BigInteger emc = inv.getAvailableEmc();
        if (!emc.equals(availableEmc)) {
            availableEmc = emc;
            if (ClientConfig.VIEW_MODE.get() == ViewMode.AFFORDABLE) {
                needsRefilter = true;
            }
        }
        ItemStack lock = lockSlot == null ? ItemStack.EMPTY : lockSlot.getItem();
        if (!ItemStack.matches(lock, lastLock)) {
            lastLock = lock.copy();
            needsRefilter = true;
        }
        if (ClientConfig.SYNC_SEARCH.get() && !searchBox.isFocused()) {
            String viewerText = RecipeViewerSync.getSearchText();
            if (viewerText != null && !viewerText.equals(searchBox.getValue())) {
                searchBox.setValue(viewerText);
            }
        }
    }

    private void rebuildEntries(Set<ItemInfo> knowledge) {
        knowledgeSize = knowledge.size();
        Map<ItemInfo, TerminalEntry> rebuilt = new HashMap<>(knowledgeSize);
        for (ItemInfo info : knowledge) {
            long emc = IEMCProxy.INSTANCE.getValue(info);
            if (emc > 0) {
                TerminalEntry existing = entries.get(info);
                rebuilt.put(info, existing != null && existing.emc() == emc ? existing : new TerminalEntry(info, emc));
            }
        }
        entries = rebuilt;
        needsRefilter = true;
    }

    private void onSearchChanged(String text) {
        lastSearch = text;
        SearchQuery parsed = SearchQuery.parse(text);
        query = parsed;
        scrollRow = 0;
        needsRefilter = true;
        if (ClientConfig.SYNC_SEARCH.get()) {
            RecipeViewerSync.setSearchText(text);
        }
    }

    private void refilter() {
        needsRefilter = false;
        long lockEmc = lastLock.isEmpty() ? 0 : IEMCProxy.INSTANCE.getValue(IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(lastLock)));
        boolean affordableOnly = ClientConfig.VIEW_MODE.get() == ViewMode.AFFORDABLE;
        visible = entries.values().stream()
                .filter(entry -> lockEmc <= 0 || entry.emc() <= lockEmc)
                .filter(entry -> !affordableOnly || affordable(entry).signum() > 0)
                .filter(query)
                .sorted(comparator())
                .toList();
        scrollRow = Mth.clamp(scrollRow, 0, maxScroll());
    }

    private static Comparator<TerminalEntry> comparator() {
        Comparator<TerminalEntry> byName = Comparator.comparing(TerminalEntry::name);
        Comparator<TerminalEntry> comparator = switch (ClientConfig.SORT_ORDER.get()) {
            case NAME -> byName.thenComparingLong(TerminalEntry::emc);
            case EMC -> Comparator.comparingLong(TerminalEntry::emc).thenComparing(byName);
            case MOD -> Comparator.comparing(TerminalEntry::modName).thenComparing(byName);
        };
        comparator = comparator.thenComparing(TerminalEntry::id);
        return ClientConfig.SORT_DIRECTION.get() == SortDirection.DESCENDING ? comparator.reversed() : comparator;
    }

    private BigInteger affordable(TerminalEntry entry) {
        if (availableEmc.bitLength() < Long.SIZE) {
            return BigInteger.valueOf(availableEmc.longValue() / entry.emc());
        }
        return availableEmc.divide(BigInteger.valueOf(entry.emc()));
    }

    private int maxScroll() {
        return Math.max(0, (visible.size() + COLUMNS - 1) / COLUMNS - rows);
    }

    // endregion

    // region Grid lookup

    private boolean isOverGrid(double mouseX, double mouseY) {
        return isHovering(GRID_X - 1, GRID_Y - 1, COLUMNS * SLOT, rows * SLOT, mouseX, mouseY);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return isHovering(SCROLLBAR_X, SCROLLBAR_Y, SCROLLBAR_WIDTH, rows * SLOT, mouseX, mouseY);
    }

    private int cellAt(double mouseX, double mouseY) {
        if (!isOverGrid(mouseX, mouseY)) {
            return -1;
        }
        int column = (int) (mouseX - leftPos - GRID_X + 1) / SLOT;
        int row = (int) (mouseY - topPos - GRID_Y + 1) / SLOT;
        return Mth.clamp(row, 0, rows - 1) * COLUMNS + Mth.clamp(column, 0, COLUMNS - 1);
    }

    @Nullable
    private TerminalEntry entryAt(double mouseX, double mouseY) {
        int cell = cellAt(mouseX, mouseY);
        if (cell == -1) {
            return null;
        }
        int index = scrollRow * COLUMNS + cell;
        return index < visible.size() ? visible.get(index) : null;
    }

    public record GridHit(ItemStack stack, Rect2i area) {
    }

    /**
     * For recipe viewer integrations: the grid item under the mouse, if any.
     */
    @Nullable
    public GridHit getGridHit(double mouseX, double mouseY) {
        TerminalEntry entry = entryAt(mouseX, mouseY);
        if (entry == null) {
            return null;
        }
        int cell = cellAt(mouseX, mouseY);
        int x = leftPos + GRID_X + (cell % COLUMNS) * SLOT;
        int y = topPos + GRID_Y + (cell / COLUMNS) * SLOT;
        return new GridHit(entry.stack().copy(), new Rect2i(x, y, 16, 16));
    }

    // endregion

    // region Rendering

    @Override
    protected void renderBg(@NotNull GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (needsRefilter) {
            refilter();
        }
        int x = leftPos;
        int y = topPos;
        TerminalTexture.blit(graphics, TerminalTexture.HEADER, x, y);
        for (int row = 0; row < rows; row++) {
            TerminalTexture.blit(graphics, TerminalTexture.ROW, x, y + HEADER_HEIGHT + row * SLOT);
        }
        TerminalTexture.blit(graphics, TerminalTexture.BOTTOM, x, y + stripTop());
        TerminalTexture.blit(graphics, TerminalTexture.PANEL, x + PANEL_X, y);

        int maxScroll = maxScroll();
        int thumbOffset = maxScroll == 0 ? 0 : scrollRow * (rows * SLOT - THUMB_HEIGHT) / maxScroll;
        TerminalTexture.blit(graphics, maxScroll == 0 ? TerminalTexture.THUMB_DISABLED : TerminalTexture.THUMB,
                x + SCROLLBAR_X, y + SCROLLBAR_Y + thumbOffset);

        renderGrid(graphics, mouseX, mouseY);
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int hoveredCell = cellAt(mouseX, mouseY);
        int first = scrollRow * COLUMNS;
        for (int cell = 0; cell < rows * COLUMNS; cell++) {
            int index = first + cell;
            if (index >= visible.size()) {
                break;
            }
            TerminalEntry entry = visible.get(index);
            int x = leftPos + GRID_X + (cell % COLUMNS) * SLOT;
            int y = topPos + GRID_Y + (cell / COLUMNS) * SLOT;
            graphics.renderItem(entry.stack(), x, y);

            BigInteger count = affordable(entry);
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 200);
            if (count.signum() == 0) {
                graphics.fill(x, y, x + 16, y + 16, 0x998B8B8B);
            } else {
                renderCount(graphics, CountFormatter.compact(count), x, y);
            }
            graphics.pose().popPose();
            if (cell == hoveredCell) {
                renderSlotHighlight(graphics, x, y, 0);
            }
        }
    }

    private void renderCount(GuiGraphics graphics, String text, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + 16, y + 16, 0);
        graphics.pose().scale(0.5F, 0.5F, 1);
        graphics.drawString(font, text, -font.width(text), -8, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        Component header = title;
        if (inv.learnFlag > 0) {
            header = Component.translatable(LANG + "learned").withStyle(ChatFormatting.DARK_GREEN);
        } else if (inv.unlearnFlag > 0) {
            header = Component.translatable(LANG + "unlearned").withStyle(ChatFormatting.DARK_RED);
        }
        graphics.drawString(font, clip(header, 72), titleLabelX, titleLabelY, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0x404040, false);

        int emcY = stripTop() + 9;
        Component label = Component.translatable(LANG + "emc");
        graphics.drawString(font, label, EMC_BOX_X + 4, emcY, 0x9FB4CF, false);
        int valueX = EMC_BOX_X + 4 + font.width(label) + 4;
        graphics.drawString(font, clip(TransmutationEMCFormatter.formatEMC(availableEmc), EMC_BOX_X + EMC_BOX_WIDTH - 3 - valueX), valueX, emcY, 0xFFE070, false);
    }

    private FormattedCharSequence clip(Component text, int width) {
        return Language.getInstance().getVisualOrder(font.substrByWidth(text, width));
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics graphics, int mouseX, int mouseY) {
        if (menu.getCarried().isEmpty()) {
            TerminalEntry entry = entryAt(mouseX, mouseY);
            if (entry != null) {
                List<Component> lines = new ArrayList<>(getTooltipFromContainerItem(entry.stack()));
                lines.add(Component.translatable(LANG + "tooltip.affordable", EMCHelper.formatEmc(affordable(entry))).withStyle(ChatFormatting.GRAY));
                lines.add(Component.translatable(LANG + "tooltip.controls").withStyle(ChatFormatting.DARK_GRAY));
                graphics.renderTooltip(font, lines, entry.stack().getTooltipImage(), entry.stack(), mouseX, mouseY);
                return;
            }
            if (isHovering(EMC_BOX_X, stripTop() + 3, EMC_BOX_WIDTH, SLOT, mouseX, mouseY)) {
                graphics.renderTooltip(font, PELang.EMC_TOOLTIP.translate(EMCHelper.formatEmc(availableEmc)), mouseX, mouseY);
                return;
            }
            if (hoveredSlot != null && !hoveredSlot.hasItem()) {
                String hint = slotHint(hoveredSlot);
                if (hint != null) {
                    List<FormattedCharSequence> lines = new ArrayList<>();
                    lines.add(Component.translatable(LANG + "slot." + hint).getVisualOrderText());
                    MutableComponent description = Component.translatable(LANG + "slot." + hint + ".desc").withStyle(ChatFormatting.GRAY);
                    lines.addAll(font.split(description, 160));
                    graphics.renderTooltip(font, lines, mouseX, mouseY);
                    return;
                }
            }
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Nullable
    private static String slotHint(Slot slot) {
        return switch (slot) {
            case SlotConsume ignored -> "burn";
            case SlotUnlearn ignored -> "unlearn";
            case SlotLock ignored -> "lock";
            case SlotInput ignored -> "klein_star";
            default -> null;
        };
    }

    // endregion

    // region Input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox.isMouseOver(mouseX, mouseY)) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                searchBox.setValue("");
            }
            setFocused(searchBox);
            searchBox.mouseClicked(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT);
            return true;
        }
        if (searchBox.isFocused()) {
            setFocused(null);
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollTo(mouseY);
            return true;
        }
        if (isOverGrid(mouseX, mouseY)) {
            onGridClicked(entryAt(mouseX, mouseY), button);
            swallowRelease = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void onGridClicked(@Nullable TerminalEntry entry, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT && button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty() && (entry == null || !ItemStack.isSameItemSameComponents(carried, entry.stack()))) {
            // Holding something else: drop it into ProjectE's burn slot, same as AE2 inserting into the network
            if (consumeSlot != null && consumeSlot.mayPlace(carried)) {
                slotClicked(consumeSlot, consumeSlot.index, button, ClickType.PICKUP);
            }
            return;
        }
        if (entry == null || affordable(entry).signum() == 0) {
            return;
        }
        int maxStack = entry.stack().getMaxStackSize();
        boolean toInventory = hasShiftDown();
        int amount;
        if (toInventory || hasControlDown()) {
            amount = maxStack;
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            amount = (maxStack + 1) / 2;
        } else {
            amount = 1;
        }
        PacketDistributor.sendToServer(new ExtractItemPayload(menu.containerId, entry.stack().copyWithCount(1), amount, toInventory));
    }

    private void scrollTo(double mouseY) {
        double progress = (mouseY - topPos - SCROLLBAR_Y - THUMB_HEIGHT / 2.0) / (rows * SLOT - THUMB_HEIGHT);
        scrollRow = Mth.clamp((int) Math.round(progress * maxScroll()), 0, maxScroll());
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            scrollTo(mouseY);
            return true;
        }
        if (swallowRelease) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScrollbar || swallowRelease) {
            draggingScrollbar = false;
            swallowRelease = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && isHovering(0, 0, WIDTH, stripTop(), mouseX, mouseY)) {
            int step = hasShiftDown() ? rows : 1;
            scrollRow = Mth.clamp(scrollRow - (int) Math.signum(scrollY) * step, 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                setFocused(null);
                return true;
            }
            // Swallow everything else so typing "e" doesn't close the screen
            searchBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        inv.learnFlag = 0;
        inv.unlearnFlag = 0;
    }

    // endregion
}
