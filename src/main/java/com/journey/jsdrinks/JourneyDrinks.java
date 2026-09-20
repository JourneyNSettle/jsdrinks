package com.journey.jsdrinks;

import com.journey.jsdrinks.registry.JSDCreativeTabs;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(JourneyDrinks.MODID)
public class JourneyDrinks {
    public static final String MODID = "jsdrinks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JourneyDrinks(IEventBus modEventBus) {
        LOGGER.info("Initializing Journey Drinks (TFC + Firmalife Addon)");

        // Register registries
        JSDDataComponents.DATA_COMPONENTS.register(modEventBus);
        JSDItems.ITEMS.register(modEventBus);
        JSDCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
    }
}
