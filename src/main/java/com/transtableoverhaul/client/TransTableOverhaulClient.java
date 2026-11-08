package com.transtableoverhaul.client;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.client.screen.TransmutationCraftingScreen;
import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import com.transtableoverhaul.registry.ModRegistries;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TransTableOverhaul.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TransTableOverhaulClient {

    private TransTableOverhaulClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // The screen's menu type is ProjectE's TransmutationContainer, so the constructor reference can't be inferred
        event.enqueueWork(() -> MenuScreens.<TransmutationContainer, TransmutationCraftingScreen>register(ModRegistries.CRAFTING_MENU.get(),
                (menu, inventory, title) -> new TransmutationCraftingScreen((TransmutationCraftingMenu) menu, inventory, title)));
    }
}
