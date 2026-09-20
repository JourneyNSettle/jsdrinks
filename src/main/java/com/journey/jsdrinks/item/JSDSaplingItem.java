package com.journey.jsdrinks.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.items.PlantableInfo;
import net.dries007.tfc.util.climate.ClimateRange;

import java.util.List;
import java.util.function.Supplier;

public class JSDSaplingItem extends BlockItem implements PlantableInfo {
    private final Supplier<ClimateRange> climateRange;
    private final List<Lifecycle> stages;

    public JSDSaplingItem(Block block, Supplier<ClimateRange> climateRange, Lifecycle[] stages) {
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        PlantableInfo.addTooltipInfo(stack, tooltip::add);
    }
}
