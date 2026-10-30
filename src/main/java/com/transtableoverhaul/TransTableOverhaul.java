package com.transtableoverhaul;

import com.mojang.logging.LogUtils;
import com.transtableoverhaul.client.ClientConfig;
import com.transtableoverhaul.network.ExtractItemPayload;
import com.transtableoverhaul.network.FillCraftingGridPayload;
import com.transtableoverhaul.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

@Mod(TransTableOverhaul.MODID)
public class TransTableOverhaul {

    public static final String MODID = "transtable_overhaul";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TransTableOverhaul(IEventBus modBus, ModContainer container) {
        ModRegistries.register(modBus);
        modBus.addListener(TransTableOverhaul::registerPayloads);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Optional so that a client with the mod can still join a server without it (the screen then falls back to ProjectE's).
        event.registrar("2")
                .optional()
                .playToServer(ExtractItemPayload.TYPE, ExtractItemPayload.STREAM_CODEC, ExtractItemPayload::handle)
                .playToServer(FillCraftingGridPayload.TYPE, FillCraftingGridPayload.STREAM_CODEC, FillCraftingGridPayload::handle);
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
