package com.journey.jsdrinks.registry;

import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.worldgen.feature.LargeTeaTreeFeature;
import com.journey.jsdrinks.worldgen.feature.TeaBushFeature;
import com.journey.jsdrinks.worldgen.feature.WildCoffeeTreeFeature;
import com.journey.jsdrinks.worldgen.feature.WildTeaTreeFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class JSDFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, JourneyDrinks.MODID);

    public static final DeferredHolder<Feature<?>, TeaBushFeature> TEA_BUSH = FEATURES.register("tea_bush", () -> new TeaBushFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, WildTeaTreeFeature> TEA_TREE = FEATURES.register("tea_tree", () -> new WildTeaTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, LargeTeaTreeFeature> LARGE_TEA_TREE = FEATURES.register("large_tea_tree", () -> new LargeTeaTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, WildCoffeeTreeFeature> COFFEE_TREE = FEATURES.register("coffee_tree", () -> new WildCoffeeTreeFeature(NoneFeatureConfiguration.CODEC));
}
