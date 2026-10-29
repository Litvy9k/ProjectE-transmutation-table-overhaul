package com.transtableoverhaul.client.screen;

import com.transtableoverhaul.client.search.SearchEntry;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;

/**
 * One learned item shown in the terminal grid. Tags and tooltip text are only computed when a search needs them.
 */
public final class TerminalEntry implements SearchEntry {

    private final ItemInfo info;
    private final ItemStack stack;
    private final long emc;
    private final String name;
    private final String id;
    private final String modId;
    private final String modName;
    private List<String> tags;
    private String tooltip;

    public TerminalEntry(ItemInfo info, long emc) {
        this.info = info;
        this.stack = info.createStack();
        this.emc = emc;
        this.name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        this.id = key.toString();
        this.modId = key.getNamespace();
        this.modName = ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElse(modId)
                .toLowerCase(Locale.ROOT);
    }

    public ItemInfo info() {
        return info;
    }

    /**
     * @apiNote Shared instance, never modify or hand it to something that might.
     */
    public ItemStack stack() {
        return stack;
    }

    public long emc() {
        return emc;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String modId() {
        return modId;
    }

    @Override
    public String modName() {
        return modName;
    }

    @Override
    public List<String> tags() {
        if (tags == null) {
            tags = stack.getTags().map(tag -> tag.location().toString()).toList();
        }
        return tags;
    }

    @Override
    public String tooltip() {
        if (tooltip == null) {
            Minecraft minecraft = Minecraft.getInstance();
            tooltip = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.Default.NORMAL)
                    .stream()
                    .map(Component::getString)
                    .collect(Collectors.joining("\n"))
                    .toLowerCase(Locale.ROOT);
        }
        return tooltip;
    }
}
