package com.journey.jsdrinks.registry;

import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import com.journey.jsdrinks.block.entity.JSDTickingPlantBlockEntity;
import com.journey.jsdrinks.block.entity.TeaBushBlockEntity;
import com.journey.jsdrinks.block.entity.TeaPileBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class JSDBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JourneyDrinks.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeaBushBlockEntity>> TEA_BUSH =
            BLOCK_ENTITIES.register("tea_bush", () ->
                    BlockEntityType.Builder.of(TeaBushBlockEntity::new, JSDBlocks.TEA_BUSH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeaPileBlockEntity>> TEA_PILE =
            BLOCK_ENTITIES.register("tea_pile", () ->
                    BlockEntityType.Builder.of(TeaPileBlockEntity::new, JSDBlocks.TEA_PILE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JSDTickingPlantBlockEntity>> TICKING_PLANT =
            BLOCK_ENTITIES.register("ticking_plant", () ->
                    BlockEntityType.Builder.of(JSDTickingPlantBlockEntity::new,
                            Stream.<Supplier<? extends Block>>of(
                                    JSDBlocks.TEA_SAPLING,
                                    JSDBlocks.COFFEE_SAPLING,
                                    JSDBlocks.TEA_GROWING_BRANCH,
                                    JSDBlocks.COFFEE_GROWING_BRANCH
                            ).map(Supplier::get).toArray(Block[]::new)
                    ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<JSDBerryBushBlockEntity>> BERRY_BUSH =
            BLOCK_ENTITIES.register("berry_bush", () ->
                    BlockEntityType.Builder.of(JSDBerryBushBlockEntity::new,
                            Stream.<Supplier<? extends Block>>of(
                                    JSDBlocks.TEA_LEAVES,
                                    JSDBlocks.COFFEE_LEAVES
                            ).map(Supplier::get).toArray(Block[]::new)
                    ).build(null));
}
