package com.transtableoverhaul.client;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.client.screen.TransmutationCraftingScreen;
import com.transtableoverhaul.crafting.TransmutationCraftingMenu;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import com.transtableoverhaul.registry.ModRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TransTableOverhaul.MODID, dist = Dist.CLIENT)
public class TransTableOverhaulClient {

    public TransTableOverhaulClient(ModContainer container, IEventBus modBus) {
        // "Config" button in the mod list
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modBus.addListener(TransTableOverhaulClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        // The screen's menu type is ProjectE's TransmutationContainer, so the constructor reference can't be inferred
        event.<TransmutationContainer, TransmutationCraftingScreen>register(ModRegistries.CRAFTING_MENU.get(),
                (menu, inventory, title) -> new TransmutationCraftingScreen((TransmutationCraftingMenu) menu, inventory, title));
    }
}
