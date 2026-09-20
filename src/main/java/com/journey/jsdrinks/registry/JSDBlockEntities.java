package com.journey.jsdrinks.registry;

import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.block.entity.TeaBushBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class JSDBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JourneyDrinks.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeaBushBlockEntity>> TEA_BUSH =
            BLOCK_ENTITIES.register("tea_bush", () ->
                    BlockEntityType.Builder.of(TeaBushBlockEntity::new, JSDBlocks.TEA_BUSH.get()).build(null));
}
