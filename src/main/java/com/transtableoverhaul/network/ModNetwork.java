package com.transtableoverhaul.network;

import com.transtableoverhaul.TransTableOverhaul;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {

    private static final String VERSION = "2";

    // Optional on both sides so that a client with the mod can still join a server without it (the screen then falls back to ProjectE's)
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(TransTableOverhaul.rl("main"),
            () -> VERSION, NetworkRegistry.acceptMissingOr(VERSION), NetworkRegistry.acceptMissingOr(VERSION));

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, ExtractItemMessage.class, ExtractItemMessage::encode, ExtractItemMessage::decode, ExtractItemMessage::handle);
        CHANNEL.registerMessage(id++, FillCraftingGridMessage.class, FillCraftingGridMessage::encode, FillCraftingGridMessage::decode, FillCraftingGridMessage::handle);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    /**
     * @apiNote Client only
     */
    public static boolean serverHasMod() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        return connection != null && CHANNEL.isRemotePresent(connection.getConnection());
    }
}
