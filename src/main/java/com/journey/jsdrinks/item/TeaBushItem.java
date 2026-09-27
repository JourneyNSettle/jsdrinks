package com.journey.jsdrinks.item;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.items.PlantableInfo;
import net.dries007.tfc.util.climate.ClimateRange;

/**
 * Item that represents a full tea bush (not a sapling).
 * When placed, it immediately creates a 2-block-tall TeaBushBlock.
 * Implements PlantableInfo so the "Grows in: [months]" tooltip is shown in JEI/inventory.
 */
public class TeaBushItem extends BlockItem implements PlantableInfo {
    private final Supplier<ClimateRange> climateRange;
    private final List<Lifecycle> stages;

    public TeaBushItem(Block block, Supplier<ClimateRange> climateRange, Lifecycle[] stages) {
        super(block, new Properties());
        this.climateRange = climateRange;
        this.stages = List.of(stages);
    }

    @Override
    public @Nullable ClimateRange getClimateRangeInfo() {
        return climateRange.get();
    }

    @Override
    public @Nullable List<Lifecycle> getLifecycleInfo() {
        return stages;
    }
}
