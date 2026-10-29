package com.transtableoverhaul.client;

import com.transtableoverhaul.TransTableOverhaul;
import com.transtableoverhaul.client.screen.TransmutationTerminalScreen;
import com.transtableoverhaul.network.ExtractItemPayload;
import moze_intel.projecte.gameObjs.gui.GUITransmutation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = TransTableOverhaul.MODID, value = Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    /**
     * The transmutation table, the tablet and the Curios keybind all open ProjectE's {@link GUITransmutation} for the same
     * menu, so swapping the screen here covers all of them.
     */
    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof GUITransmutation original && serverSupportsTerminal()) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                event.setNewScreen(new TransmutationTerminalScreen(original.getMenu(), minecraft.player.getInventory(), original.getTitle()));
            }
        }
    }

    private static boolean serverSupportsTerminal() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && connection.hasChannel(ExtractItemPayload.TYPE);
    }
}
