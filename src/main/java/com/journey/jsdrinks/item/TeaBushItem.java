package com.journey.jsdrinks.item;

import java.util.List;
import java.util.function.Supplier;

import com.journey.jsdrinks.JSDConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.items.PlantableInfo;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.ClimateRange;

/**
 * Item that represents a full tea bush (not a sapling).
 * When placed, it immediately creates a 2-block-tall TeaBushBlock.
 * Implements PlantableInfo so the "Grows in: [time]" (to tree) and climate are shown in JEI/inventory.
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

    @Override
    public int getGrowthTimeInfo() {
        int years = JSDConfig.TEA_BUSH_TRANSFORM_YEARS.get();
        return (int) ((long) years * Calendars.get().getCalendarDaysInMonth() * ICalendar.MONTHS_IN_YEAR * ICalendar.CALENDAR_TICKS_IN_DAY);
    }

    @Override
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        int years = JSDConfig.TEA_BUSH_TRANSFORM_YEARS.get();
        tooltip.add(Component.translatable("jsdrinks.tooltip.tea_bush.tree_transform_hint", years).withStyle(ChatFormatting.DARK_GREEN));
    }
}
