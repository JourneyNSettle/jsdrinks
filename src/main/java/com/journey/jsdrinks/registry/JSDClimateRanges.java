package com.journey.jsdrinks.registry;

import com.journey.jsdrinks.JourneyDrinks;
import net.minecraft.resources.ResourceLocation;
import net.dries007.tfc.util.climate.ClimateRange;
import net.dries007.tfc.util.data.DataManager;

import java.util.function.Supplier;

public final class JSDClimateRanges {
    public static final Supplier<ClimateRange> TEA_BUSH = register("plant/tea_bush");
    public static final Supplier<ClimateRange> TEA_TREE = register("plant/tea_tree");
    public static final Supplier<ClimateRange> COFFEE_TREE = register("plant/coffee_tree");

    private static DataManager.Reference<ClimateRange> register(String name) {
        return ClimateRange.MANAGER.getReference(ResourceLocation.fromNamespaceAndPath(JourneyDrinks.MODID, name));
    }
}
