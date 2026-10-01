package com.journey.jsdrinks;

import com.journey.jsdrinks.client.JSDClientEvents;
import com.journey.jsdrinks.recipe.JSDItemStackModifiers;
import com.journey.jsdrinks.recipe.JSDRecipeSerializers;
import com.journey.jsdrinks.recipe.JSDRecipeTypes;
import com.journey.jsdrinks.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(JourneyDrinks.MODID)
public class JourneyDrinks {
    public static final String MODID = "jsdrinks";
    public static final String MOD_ID = MODID;
    public static final Logger LOGGER = LoggerFactory.getLogger(JourneyDrinks.class);

    public JourneyDrinks(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Initializing Journey Drinks addon for TFC & Firmalife");

        modContainer.registerConfig(ModConfig.Type.SERVER, JSDConfig.SPEC);

        JSDDataComponents.DATA_COMPONENTS.register(modEventBus);
        JSDBlocks.BLOCKS.register(modEventBus);
        JSDItems.ITEMS.register(modEventBus);
        JSDBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        JSDFluids.FLUIDS.register(modEventBus);
        JSDFluids.FLUID_TYPES.register(modEventBus);
        JSDItemStackModifiers.MODIFIERS.register(modEventBus);
        JSDRecipeTypes.RECIPE_TYPES.register(modEventBus);
        JSDRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        JSDCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        JSDFeatures.FEATURES.register(modEventBus);

        if (FMLEnvironment.dist.isClient()) {
            JSDClientEvents.init(modEventBus);
        }
    }
}
