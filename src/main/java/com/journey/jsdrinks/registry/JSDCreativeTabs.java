package com.journey.jsdrinks.registry;

import com.journey.jsdrinks.JourneyDrinks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class JSDCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, JourneyDrinks.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> JOURNEY_DRINKS_TAB =
            CREATIVE_MODE_TABS.register("journey_drinks_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.jsdrinks"))
                    .icon(() -> new ItemStack(JSDItems.COFFEE_CHERRY.get()))
                    .displayItems((parameters, output) -> {
                        JSDItems.ITEMS.getEntries().forEach(entry -> output.accept(entry.get()));
                    })
                    .build());
}
