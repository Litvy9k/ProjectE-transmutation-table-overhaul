package com.transtableoverhaul.client;

import com.transtableoverhaul.TransTableOverhaul;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = TransTableOverhaul.MODID, dist = Dist.CLIENT)
public class TransTableOverhaulClient {

    public TransTableOverhaulClient(ModContainer container) {
        // "Config" button in the mod list
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
