package com.journey.jsdrinks;

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
    }
}
