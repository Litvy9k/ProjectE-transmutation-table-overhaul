package com.transtableoverhaul.registry;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.crafting.PortableCraftingTableItem;
import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import com.transtableoverhaul.crafting.TransmutationCraftingTableBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistries {

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, TransTableOverhaul.MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TransTableOverhaul.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, TransTableOverhaul.MODID);

    private static final ResourceLocation PROJECTE_TAB = new ResourceLocation("projecte", "projecte");
    private static final ResourceLocation PROJECTE_TABLE = new ResourceLocation("projecte", "transmutation_table");

    // Same properties as ProjectE's transmutation table
    public static final RegistryObject<TransmutationCraftingTableBlock> CRAFTING_TABLE = BLOCKS.register("transmutation_crafting_table",
            () -> new TransmutationCraftingTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .instrument(NoteBlockInstrument.BASEDRUM)
                    .requiresCorrectToolForDrops()
                    .strength(10, 30)));
    public static final RegistryObject<BlockItem> CRAFTING_TABLE_ITEM = ITEMS.register("transmutation_crafting_table",
            () -> new BlockItem(CRAFTING_TABLE.get(), new Item.Properties()));
    public static final RegistryObject<PortableCraftingTableItem> PORTABLE_CRAFTING_TABLE = ITEMS.register("portable_transmutation_crafting_table",
            () -> new PortableCraftingTableItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<MenuType<TransmutationCraftingMenu>> CRAFTING_MENU = MENUS.register("transmutation_crafting",
            () -> IForgeMenuType.create(TransmutationCraftingMenu::fromNetwork));

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
            ItemStack craftingTable = new ItemStack(CRAFTING_TABLE_ITEM.get());
            event.getEntries().putAfter(projecteTable, craftingTable, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            event.getEntries().putAfter(craftingTable, new ItemStack(PORTABLE_CRAFTING_TABLE.get()), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CRAFTING_TABLE_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PORTABLE_CRAFTING_TABLE);
        }
    }
}
