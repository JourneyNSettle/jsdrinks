package com.journey.jsdrinks.registry;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.dries007.tfc.common.fluids.FluidHolder;
import net.dries007.tfc.common.fluids.MixingFluid;
import net.dries007.tfc.util.registry.RegistrationHelpers;

import static com.journey.jsdrinks.JourneyDrinks.MOD_ID;

public class JSDFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, MOD_ID);

    // Coffee fluids
    public static final FluidHolder<BaseFlowingFluid> COFFEE = register("coffee");
    public static final FluidHolder<BaseFlowingFluid> STRONG_COFFEE = register("strong_coffee");
    public static final FluidHolder<BaseFlowingFluid> COFFEE_WITH_MILK = register("coffee_with_milk");
    public static final FluidHolder<BaseFlowingFluid> STRONG_COFFEE_WITH_MILK = register("strong_coffee_with_milk");

    // Tea fluids
    public static final FluidHolder<BaseFlowingFluid> WHITE_TEA = register("white_tea");
    public static final FluidHolder<BaseFlowingFluid> GREEN_TEA = register("green_tea");
    public static final FluidHolder<BaseFlowingFluid> YELLOW_TEA = register("yellow_tea");
    public static final FluidHolder<BaseFlowingFluid> RED_TEA = register("red_tea");
    public static final FluidHolder<BaseFlowingFluid> PUERH_TEA = register("puerh_tea");
    public static final FluidHolder<BaseFlowingFluid> STRONG_PUERH_TEA = register("strong_puerh_tea");

    private static FluidHolder<BaseFlowingFluid> register(String name) {
        return register(
            name,
            properties -> {}, // Vessel-only: no in-world block, no bucket item!
            waterLike().descriptionId("fluid." + MOD_ID + "." + name).canConvertToSource(false),
            MixingFluid.Source::new,
            MixingFluid.Flowing::new
        );
    }

    private static FluidType.Properties waterLike() {
        return FluidType.Properties.create()
            .adjacentPathType(PathType.WATER)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .canConvertToSource(false)
            .canDrown(true)
            .canExtinguish(true)
            .canHydrate(true)
            .canPushEntity(true)
            .canSwim(true)
            .supportsBoating(true);
    }

    private static <F extends FlowingFluid> FluidHolder<F> register(
        String name,
        Consumer<BaseFlowingFluid.Properties> builder,
        FluidType.Properties typeProperties,
        Function<BaseFlowingFluid.Properties, F> sourceFactory,
        Function<BaseFlowingFluid.Properties, F> flowingFactory
    ) {
        final int index = name.lastIndexOf('/');
        final String flowingName = index == -1 ? "flowing_" + name : name.substring(0, index) + "/flowing_" + name.substring(index + 1);

        return RegistrationHelpers.registerFluid(
            FLUID_TYPES, FLUIDS, name, name, flowingName,
            builder, () -> new FluidType(typeProperties),
            sourceFactory, flowingFactory
        );
    }
}
