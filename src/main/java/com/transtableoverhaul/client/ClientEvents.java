package com.transtableoverhaul.client;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.client.screen.TransmutationTerminalScreen;
import com.transtableoverhaul.network.ModNetwork;
import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TransTableOverhaul.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    /**
     * The transmutation table, the tablet and the Curios keybind all open ProjectE's {@link GUITransmutation} for the same
     * menu, so swapping the screen here covers all of them.
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof GUITransmutation original && ModNetwork.serverHasMod()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                event.setNewScreen(new TransmutationTerminalScreen(original.getMenu(), minecraft.player.getInventory(), original.getTitle()));
            }
        }
    }
}
