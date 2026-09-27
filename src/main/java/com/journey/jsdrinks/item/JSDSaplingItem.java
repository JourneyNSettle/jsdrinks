package com.journey.jsdrinks.item;

import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeSaplingBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.items.PlantableInfo;
import net.dries007.tfc.util.climate.ClimateRange;

public class JSDSaplingItem extends BlockItem implements PlantableInfo {
    private final Supplier<ClimateRange> climateRange;
    private final List<Lifecycle> stages;
    private final LongSupplier ticksToGrow;

    public JSDSaplingItem(Block block, Supplier<ClimateRange> climateRange, Lifecycle[] stages) {
        super(block, new Properties());
        this.climateRange = climateRange;
        this.stages = List.of(stages);
        if (block instanceof FruitTreeSaplingBlock sapling) {
            this.ticksToGrow = sapling::getTicksToGrow;
        } else {
            this.ticksToGrow = () -> -1;
        }
    }

    @Override
    public @Nullable ClimateRange getClimateRangeInfo() {
        return climateRange.get();
    }

    @Override
    public @Nullable List<Lifecycle> getLifecycleInfo() {
        return stages;
    }

    @Override
    public int getGrowthTimeInfo() {
        return (int) ticksToGrow.getAsLong();
    }

    @Override
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
}
