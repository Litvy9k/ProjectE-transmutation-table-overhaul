package com.transtableoverhaul.crafting;

import com.transtableoverhaul.registry.ModRegistries;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ProjectE's transmutation menu plus a 3x3 crafting grid, like an AE2 crafting terminal. Everything the plain terminal
 * does (grid extraction, learning, Klein stars) keeps working because this is still a {@link TransmutationContainer}.
 * <p>
 * Each grid slot either holds real items (from the player's inventory) or is an EMC placeholder: a single learned item
 * that is never used up, instead every craft pays its EMC value. Real items that run out are refilled from the
 * inventory when that is the preferred source, otherwise they turn into a placeholder if the item is learned. Switching
 * the preferred source converts the grid right away. Taking a result that has an EMC value also teaches it to the player.
 */
public class TransmutationCraftingMenu extends TransmutationContainer {

    public static final int BUTTON_PREFER_TABLE = 0;
    public static final int BUTTON_PREFER_INVENTORY = 1;
    public static final int BUTTON_CLEAR_GRID = 2;
    /** Plus the index in {@link #matchingRecipes}: use that recipe when several match the grid. */
    public static final int BUTTON_SELECT_RECIPE = 100;
    public static final int GRID_SIZE = 9;

    private final ContainerLevelAccess access;
    private final Player player;
    private final TransientCraftingContainer craftSlots = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultSlots = new ResultContainer();
    /** Bit i set: grid slot i is an EMC placeholder. Synced to the client so it can draw and predict clicks. */
    private final DataSlot placeholders = DataSlot.standalone();
    private final int gridStart;
    private final int resultIndex;
    private int playerInventoryStart = Integer.MAX_VALUE;
    private int playerInventoryEnd;
    private boolean preferInventory;
    private boolean suppressUpdates;
    private boolean lastAffordable = true;
    @Nullable
    private RecipeHolder<CraftingRecipe> lastRecipe;
    /** Recipe the player picked when several match; used whenever it matches the grid. */
    @Nullable
    private ResourceLocation selectedRecipe;

    public TransmutationCraftingMenu(int windowId, Inventory playerInv) {
        this(windowId, playerInv, ContainerLevelAccess.NULL);
    }

    public TransmutationCraftingMenu(int windowId, Inventory playerInv, ContainerLevelAccess access) {
        super(windowId, playerInv);
        this.access = access;
        this.player = playerInv.player;
        for (Slot slot : slots) {
            if (slot.container == playerInv) {
                playerInventoryStart = Math.min(playerInventoryStart, slot.index);
                playerInventoryEnd = Math.max(playerInventoryEnd, slot.index + 1);
            }
        }
        // Positions are set by the screen
        gridStart = slots.size();
        for (int i = 0; i < GRID_SIZE; i++) {
            addSlot(new GridSlot(i));
        }
        resultIndex = slots.size();
        addSlot(new TerminalResultSlot(playerInv.player));
        addDataSlot(placeholders);
    }

    @NotNull
    @Override
    public MenuType<?> getType() {
        // ProjectE's constructor registers its own menu type, but the client has to open our screen
        return ModRegistries.CRAFTING_MENU.get();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(access, player, ModRegistries.CRAFTING_TABLE.get());
    }

    public boolean isPreferInventory() {
        return preferInventory;
    }

    public void setPreferInventory(boolean preferInventory) {
        this.preferInventory = preferInventory;
    }

    public boolean isCraftingGrid(Container container) {
        return container == craftSlots;
    }

    public boolean isResult(Container container) {
        return container == resultSlots;
    }

    public boolean isPlaceholder(int gridIndex) {
        return (placeholders.get() >> gridIndex & 1) != 0;
    }

    private void setPlaceholder(int gridIndex, boolean placeholder) {
        int mask = placeholders.get();
        placeholders.set(placeholder ? mask | 1 << gridIndex : mask & ~(1 << gridIndex));
    }

    /**
     * EMC paid per craft for the placeholders in the grid.
     *
     * @return the cost, or {@code null} if a placeholder's item is no longer learned
     */
    @Nullable
    public BigInteger placeholderCost() {
        BigInteger total = BigInteger.ZERO;
        for (int i = 0; i < GRID_SIZE; i++) {
            if (isPlaceholder(i)) {
                long value = CraftingSources.learnedValue(transmutationInventory, craftSlots.getItem(i));
                if (value <= 0) {
                    return null;
                }
                total = total.add(BigInteger.valueOf(value));
            }
        }
        return total;
    }

    private boolean canAffordPlaceholders() {
        BigInteger cost = placeholderCost();
        return cost != null && transmutationInventory.getAvailableEmc().compareTo(cost) >= 0;
    }

    /**
     * Every recipe the grid matches, in a stable order (the same on client and server).
     */
    public List<RecipeHolder<CraftingRecipe>> matchingRecipes(Level level) {
        return level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, craftSlots.asCraftInput(), level);
    }

    public CraftingInput craftInput() {
        return craftSlots.asCraftInput();
    }

    private Optional<RecipeHolder<CraftingRecipe>> findRecipe(Level level, CraftingInput input) {
        if (selectedRecipe != null) {
            for (RecipeHolder<CraftingRecipe> recipe : level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING, input, level)) {
                if (recipe.id().equals(selectedRecipe)) {
                    return Optional.of(recipe);
                }
            }
        }
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level, lastRecipe);
    }

    /**
     * For the JEI transfer button: can {@code template} be put into the grid?
     */
    public boolean canSupply(ItemStack template) {
        for (int i = 0; i < GRID_SIZE; i++) {
            if (ItemStack.isSameItemSameComponents(craftSlots.getItem(i), template)) {
                return true;
            }
        }
        return CraftingSources.isAvailable(player, transmutationInventory, template);
    }

    // region Grid and result slots

    private class GridSlot extends Slot {

        GridSlot(int index) {
            super(craftSlots, index, 0, 0);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return !isPlaceholder(getContainerSlot());
        }
    }

    /**
     * Vanilla result slot, except placeholders are paid for with EMC instead of being used up, and taking a result
     * also records it as learned.
     */
    private class TerminalResultSlot extends ResultSlot {

        TerminalResultSlot(Player player) {
            super(player, craftSlots, resultSlots, 0, 0, 0);
        }

        @Override
        public boolean mayPickup(@NotNull Player player) {
            return canAffordPlaceholders();
        }

        /**
         * Called with the crafted stack both when it is picked up and when it is shift-clicked (where the stack passed
         * to {@link #onTake} may already be empty).
         */
        @Override
        protected void checkTakeAchievements(@NotNull ItemStack stack) {
            if (!player.level().isClientSide && !stack.isEmpty() && IEMCProxy.INSTANCE.hasValue(stack)) {
                transmutationInventory.handleKnowledge(stack);
            }
            super.checkTakeAchievements(stack);
        }

        @Override
        public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
            checkTakeAchievements(stack);
            consumeIngredients(player);
        }
    }

    private void consumeIngredients(Player player) {
        CraftingInput.Positioned positioned = craftSlots.asPositionedCraftInput();
        CraftingInput input = positioned.input();
        Level level = player.level();
        CommonHooks.setCraftingPlayer(player);
        // The picked recipe decides the leftovers too, not just the first one that matches
        NonNullList<ItemStack> remaining = lastRecipe != null && lastRecipe.value().matches(input, level)
                ? lastRecipe.value().getRemainingItems(input)
                : level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, input, level);
        CommonHooks.setCraftingPlayer(null);

        BigInteger cost = BigInteger.ZERO;
        suppressUpdates = true;
        try {
            for (int y = 0; y < input.height(); y++) {
                for (int x = 0; x < input.width(); x++) {
                    int slot = x + positioned.left() + (y + positioned.top()) * craftSlots.getWidth();
                    ItemStack inSlot = craftSlots.getItem(slot);
                    ItemStack leftover = remaining.get(x + y * input.width());
                    if (isPlaceholder(slot)) {
                        cost = cost.add(BigInteger.valueOf(CraftingSources.learnedValue(transmutationInventory, inSlot)));
                        if (!leftover.isEmpty()) {
                            player.getInventory().placeItemBackInInventory(leftover);
                        }
                        continue;
                    }
                    ItemStack template = inSlot.copyWithCount(1);
                    if (!inSlot.isEmpty()) {
                        craftSlots.removeItem(slot, 1);
                        inSlot = craftSlots.getItem(slot);
                    }
                    if (!leftover.isEmpty()) {
                        if (inSlot.isEmpty()) {
                            craftSlots.setItem(slot, leftover);
                        } else if (ItemStack.isSameItemSameComponents(inSlot, leftover)) {
                            leftover.grow(inSlot.getCount());
                            craftSlots.setItem(slot, leftover);
                        } else if (!player.getInventory().add(leftover)) {
                            player.drop(leftover, false);
                        }
                    } else if (inSlot.isEmpty()) {
                        // Ran out: refill from the inventory if that is the preferred source, otherwise keep going on EMC
                        ItemStack refill = preferInventory ? CraftingSources.takeFromInventory(player, template, 1) : ItemStack.EMPTY;
                        if (!refill.isEmpty()) {
                            craftSlots.setItem(slot, refill);
                        } else if (CraftingSources.learnedValue(transmutationInventory, template) > 0) {
                            setPlaceholder(slot, true);
                            craftSlots.setItem(slot, CraftingSources.placeholder(template));
                        }
                    }
                }
            }
        } finally {
            suppressUpdates = false;
        }
        if (!player.level().isClientSide && cost.signum() > 0) {
            transmutationInventory.removeEmc(cost);
        }
        updateResult();
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        if (container == craftSlots && !suppressUpdates) {
            updateResult();
        }
    }

    /**
     * Same as vanilla's crafting menu (including the doLimitedCrafting check), but no result while the placeholders
     * can't be paid for.
     */
    private void updateResult() {
        access.execute((level, pos) -> {
            CraftingInput input = craftSlots.asCraftInput();
            ItemStack result = ItemStack.EMPTY;
            lastAffordable = canAffordPlaceholders();
            Optional<RecipeHolder<CraftingRecipe>> recipe = findRecipe(level, input);
            if (recipe.isPresent() && player instanceof ServerPlayer serverPlayer && resultSlots.setRecipeUsed(level, serverPlayer, recipe.get())
                    && lastAffordable) {
                lastRecipe = recipe.get();
                ItemStack assembled = recipe.get().value().assemble(input, level.registryAccess());
                if (assembled.isItemEnabled(level.enabledFeatures())) {
                    result = assembled;
                }
            }
            resultSlots.setItem(0, result);
        });
    }

    @Override
    public void broadcastChanges() {
        // EMC can change without the grid changing (Klein stars, extracting from the terminal grid)
        if (!suppressUpdates && placeholders.get() != 0 && canAffordPlaceholders() != lastAffordable) {
            updateResult();
        }
        super.broadcastChanges();
    }

    // endregion

    // region Clicks

    @Override
    public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (slotId == resultIndex && clickType == ClickType.QUICK_MOVE) {
            quickCraftStack(player);
            return;
        }
        if (slotId >= gridStart && slotId < gridStart + GRID_SIZE && isPlaceholder(slotId - gridStart)) {
            clickPlaceholder(slotId - gridStart, button, clickType);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    /**
     * Clicking a placeholder with an item replaces it with that item, clicking it with an empty hand removes it.
     */
    private void clickPlaceholder(int gridIndex, int button, ClickType clickType) {
        ItemStack carried = getCarried();
        if (clickType == ClickType.PICKUP && !carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            setPlaceholder(gridIndex, false);
            craftSlots.setItem(gridIndex, carried.split(Math.min(amount, craftSlots.getMaxStackSize(carried))));
            setCarried(carried);
        } else if (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE || clickType == ClickType.THROW) {
            setPlaceholder(gridIndex, false);
            craftSlots.setItem(gridIndex, ItemStack.EMPTY);
        }
    }

    /**
     * Shift-click on the result: craft one stack's worth into the inventory.
     */
    private void quickCraftStack(Player player) {
        Slot slot = slots.get(resultIndex);
        if (!slot.hasItem()) {
            return;
        }
        ItemStack first = slot.getItem().copy();
        int target = first.getMaxStackSize();
        int produced = 0;
        while (slot.hasItem() && ItemStack.isSameItemSameComponents(slot.getItem(), first) && slot.mayPickup(player)
                && (produced == 0 || produced + slot.getItem().getCount() <= target)) {
            int crafted = quickCraftOnce(player, slot);
            if (crafted <= 0) {
                break;
            }
            produced += crafted;
        }
    }

    /**
     * One shift-click craft, same as vanilla's crafting menu.
     *
     * @return number of items crafted, 0 if the inventory had no room
     */
    private int quickCraftOnce(Player player, Slot slot) {
        ItemStack result = slot.getItem();
        ItemStack original = result.copy();
        access.execute((level, pos) -> result.getItem().onCraftedBy(result, level, player));
        if (!moveItemStackTo(result, playerInventoryStart, playerInventoryEnd, true)) {
            return 0;
        }
        slot.onQuickCraft(result, original);
        if (result.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, result);
        if (!result.isEmpty()) {
            player.drop(result, false);
        }
        return original.getCount();
    }

    @NotNull
    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int slotIndex) {
        if (slotIndex == resultIndex) {
            return ItemStack.EMPTY;
        }
        if (slotIndex >= gridStart && slotIndex < gridStart + GRID_SIZE) {
            // ProjectE would treat these as player inventory and burn them, move them to the inventory instead
            Slot slot = slots.get(slotIndex);
            if (!slot.hasItem()) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (!moveItemStackTo(stack, playerInventoryStart, playerInventoryEnd, true)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            return original;
        }
        return super.quickMoveStack(player, slotIndex);
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slot) {
        return slot.container != resultSlots && !isPlaceholderSlot(slot) && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canDragTo(@NotNull Slot slot) {
        return !isPlaceholderSlot(slot) && super.canDragTo(slot);
    }

    private boolean isPlaceholderSlot(Slot slot) {
        return slot.container == craftSlots && isPlaceholder(slot.getContainerSlot());
    }

    // endregion

    // region Filling and clearing

    /**
     * JEI recipe transfer. Each grid slot gets one of its {@code alternatives}: as an EMC placeholder if it is learned,
     * or as real items from the inventory, whichever source the player prefers first.
     *
     * @param max with real items, fill a whole stack instead of one
     */
    public void fillGrid(ServerPlayer player, List<List<ItemStack>> alternatives, boolean max) {
        suppressUpdates = true;
        try {
            // Keep what already fits, return the rest so it can be used again below
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack current = craftSlots.getItem(i);
                if (!current.isEmpty() && options(alternatives, i).stream().noneMatch(option -> ItemStack.isSameItemSameComponents(option, current))) {
                    clearSlot(player, i);
                }
            }
            for (int i = 0; i < GRID_SIZE; i++) {
                List<ItemStack> options = options(alternatives, i);
                if (craftSlots.getItem(i).isEmpty() && !options.isEmpty()) {
                    supplySlot(player, i, options);
                }
            }
            if (max) {
                topUpFromInventory(player);
            }
        } finally {
            suppressUpdates = false;
            updateResult();
        }
    }

    private static List<ItemStack> options(List<List<ItemStack>> alternatives, int index) {
        return index < alternatives.size() ? alternatives.get(index) : List.of();
    }

    private void supplySlot(Player player, int index, List<ItemStack> options) {
        for (boolean fromInventory : preferInventory ? new boolean[]{true, false} : new boolean[]{false, true}) {
            for (ItemStack option : options) {
                if (fromInventory) {
                    ItemStack taken = CraftingSources.takeFromInventory(player, option, 1);
                    if (!taken.isEmpty()) {
                        craftSlots.setItem(index, taken);
                        return;
                    }
                } else if (CraftingSources.learnedValue(transmutationInventory, option) > 0) {
                    setPlaceholder(index, true);
                    craftSlots.setItem(index, CraftingSources.placeholder(option));
                    return;
                }
            }
        }
    }

    /**
     * Fills the real-item slots with as many crafts' worth as the inventory allows (up to a stack), splitting each
     * ingredient evenly over all the slots that use it.
     */
    private void topUpFromInventory(Player player) {
        // Group the real-item slots by ingredient
        List<List<Integer>> groups = new ArrayList<>();
        for (int i = 0; i < GRID_SIZE; i++) {
            ItemStack stack = craftSlots.getItem(i);
            if (stack.isEmpty() || isPlaceholder(i)) {
                continue;
            }
            List<Integer> group = null;
            for (List<Integer> existing : groups) {
                if (ItemStack.isSameItemSameComponents(craftSlots.getItem(existing.getFirst()), stack)) {
                    group = existing;
                    break;
                }
            }
            if (group == null) {
                group = new ArrayList<>();
                groups.add(group);
            }
            group.add(i);
        }
        if (groups.isEmpty()) {
            return;
        }

        // Number of crafts every slot can be filled for
        int sets = 64;
        int[] inGrid = new int[groups.size()];
        for (int g = 0; g < groups.size(); g++) {
            List<Integer> group = groups.get(g);
            ItemStack template = craftSlots.getItem(group.getFirst());
            for (int slot : group) {
                inGrid[g] += craftSlots.getItem(slot).getCount();
            }
            int total = inGrid[g] + CraftingSources.countInInventory(player, template);
            sets = Math.min(sets, Math.min(template.getMaxStackSize(), total / group.size()));
        }

        for (int g = 0; g < groups.size(); g++) {
            List<Integer> group = groups.get(g);
            ItemStack template = craftSlots.getItem(group.getFirst()).copyWithCount(1);
            int wanted = sets * group.size();
            int available = inGrid[g];
            if (wanted > available) {
                available += CraftingSources.takeFromInventory(player, template, wanted - available).getCount();
            }
            // Even split; anything above the target (already in the grid before) stays spread over the same slots
            int base = available / group.size();
            int extra = available % group.size();
            for (int n = 0; n < group.size(); n++) {
                craftSlots.setItem(group.get(n), template.copyWithCount(base + (n < extra ? 1 : 0)));
            }
        }
    }

    /**
     * Empties a grid slot: placeholders just disappear, real items go back to the inventory.
     */
    private void clearSlot(Player player, int index) {
        ItemStack stack = craftSlots.removeItemNoUpdate(index);
        if (isPlaceholder(index)) {
            setPlaceholder(index, false);
        } else if (!stack.isEmpty()) {
            player.getInventory().placeItemBackInInventory(stack);
        }
    }

    /**
     * Moves the grid over to the preferred source: placeholders become real items where the inventory has them, or real
     * items go back to the inventory and become placeholders where that keeps the recipe identical.
     */
    private void swapGridSources(Player player) {
        suppressUpdates = true;
        try {
            for (int i = 0; i < GRID_SIZE; i++) {
                ItemStack stack = craftSlots.getItem(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (preferInventory && isPlaceholder(i)) {
                    ItemStack taken = CraftingSources.takeFromInventory(player, stack, 1);
                    if (!taken.isEmpty()) {
                        setPlaceholder(i, false);
                        craftSlots.setItem(i, taken);
                    }
                } else if (!preferInventory && !isPlaceholder(i) && CraftingSources.learnedValue(transmutationInventory, stack) > 0) {
                    ItemStack placeholder = CraftingSources.placeholder(stack);
                    // Enchanted or damaged items can't be made from EMC as they are, leave those alone
                    if (ItemStack.isSameItemSameComponents(placeholder, stack)) {
                        setPlaceholder(i, true);
                        craftSlots.setItem(i, placeholder);
                        player.getInventory().placeItemBackInInventory(stack);
                    }
                }
            }
        } finally {
            suppressUpdates = false;
            updateResult();
        }
    }

    private void clearGrid(Player player) {
        suppressUpdates = true;
        try {
            for (int i = 0; i < GRID_SIZE; i++) {
                clearSlot(player, i);
            }
        } finally {
            suppressUpdates = false;
            updateResult();
        }
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        switch (id) {
            case BUTTON_PREFER_TABLE, BUTTON_PREFER_INVENTORY -> {
                preferInventory = id == BUTTON_PREFER_INVENTORY;
                if (!player.level().isClientSide) {
                    swapGridSources(player);
                }
            }
            case BUTTON_CLEAR_GRID -> {
                if (!player.level().isClientSide) {
                    clearGrid(player);
                }
            }
            default -> {
                if (id < BUTTON_SELECT_RECIPE) {
                    return false;
                }
                if (!player.level().isClientSide) {
                    List<RecipeHolder<CraftingRecipe>> recipes = matchingRecipes(player.level());
                    int index = id - BUTTON_SELECT_RECIPE;
                    if (index >= recipes.size()) {
                        return false;
                    }
                    selectedRecipe = recipes.get(index).id();
                    updateResult();
                }
            }
        }
        return true;
    }

    // endregion

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        access.execute((level, pos) -> {
            // Placeholders aren't real items, don't hand them out
            for (int i = 0; i < GRID_SIZE; i++) {
                if (isPlaceholder(i)) {
                    craftSlots.removeItemNoUpdate(i);
                }
            }
            clearContainer(player, craftSlots);
        });
    }
}
