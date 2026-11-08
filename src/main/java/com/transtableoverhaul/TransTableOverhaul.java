package com.transtableoverhaul;

import com.mojang.logging.LogUtils;
import com.transtableoverhaul.client.ClientConfig;
import com.transtableoverhaul.network.ModNetwork;
import com.transtableoverhaul.registry.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(TransTableOverhaul.MODID)
public class TransTableOverhaul {

    public static final String MODID = "transtable_overhaul";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TransTableOverhaul() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistries.register(modBus);
        ModNetwork.register();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }
}
