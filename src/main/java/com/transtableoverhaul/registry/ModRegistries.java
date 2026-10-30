package com.transtableoverhaul.registry;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.crafting.PortableCraftingTableItem;
import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import com.transtableoverhaul.crafting.TransmutationCraftingTableBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRegistries {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TransTableOverhaul.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TransTableOverhaul.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TransTableOverhaul.MODID);

    private static final ResourceLocation PROJECTE_TAB = ResourceLocation.fromNamespaceAndPath("projecte", "projecte");
    private static final ResourceLocation PROJECTE_TABLE = ResourceLocation.fromNamespaceAndPath("projecte", "transmutation_table");

    // Same properties as ProjectE's transmutation table
    public static final DeferredBlock<TransmutationCraftingTableBlock> CRAFTING_TABLE = BLOCKS.register("transmutation_crafting_table",
            () -> new TransmutationCraftingTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(10, 30)));
    public static final DeferredItem<BlockItem> CRAFTING_TABLE_ITEM = ITEMS.registerSimpleBlockItem(CRAFTING_TABLE);
    public static final DeferredItem<PortableCraftingTableItem> PORTABLE_CRAFTING_TABLE = ITEMS.register("portable_transmutation_crafting_table",
            () -> new PortableCraftingTableItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<MenuType<?>, MenuType<TransmutationCraftingMenu>> CRAFTING_MENU = MENUS.register("transmutation_crafting",
            () -> IMenuTypeExtension.create(TransmutationCraftingMenu::fromNetwork));

    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        MENUS.register(modBus);
        modBus.addListener(ModRegistries::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().location().equals(PROJECTE_TAB)) {
            ItemStack projecteTable = new ItemStack(BuiltInRegistries.ITEM.get(PROJECTE_TABLE));
            event.insertAfter(projecteTable, CRAFTING_TABLE_ITEM.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.insertAfter(CRAFTING_TABLE_ITEM.toStack(), PORTABLE_CRAFTING_TABLE.toStack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CRAFTING_TABLE_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PORTABLE_CRAFTING_TABLE);
        }
    }
}
