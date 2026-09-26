package com.journey.jsdrinks.block;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeLeavesBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;

import com.journey.jsdrinks.JSDConfig;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;

public class JSDTeaLeavesBlock extends FruitTreeLeavesBlock {

    public JSDTeaLeavesBlock(ExtendedProperties properties, Lifecycle[] stages, Supplier<ClimateRange> climateRange, int flowerColor) {
        super(properties, JSDItems.FRESH_TEA_LEAF, stages, climateRange, flowerColor);
    }

    /**
     * Расширитель видимости: protected → public.
     * Необходим для вызова из LargeTeaTreeFeature (другой пакет).
     */
    @Override
    public Lifecycle getLifecycleForCurrentMonth(Level level, BlockPos pos) {
        return super.getLifecycleForCurrentMonth(level, pos);
    }

    // -------------------------------------------------------- Lifecycle update
    // Overrides to use per-plant tea regrowth delay (JSDConfig.TEA_FRUIT_REGROWTH_DAYS)
    // instead of TFC's global fruitPickBloomDelayTicks config.

    @Override
    public void onUpdate(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PERSISTENT)) return;

        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity plant) {
            Lifecycle currentLifecycle = state.getValue(LIFECYCLE);
            Lifecycle expectedLifecycle = getLifecycleForCurrentMonth(level, pos);
            if (!checkAndSetDormant(level, pos, state, currentLifecycle, expectedLifecycle)) {
                final ClimateRange range = climateRange.get();
                final BlockPos stemPos = plant.getStemPos();
                final int hydration = getFruitBushHydrationFromRootPos(level, stemPos.below());

                if (range.checkBoth(hydration, Climate.getAverageTemperature(level, stemPos), false)) {
                    currentLifecycle = currentLifecycle.advanceTowards(expectedLifecycle);
                } else {
                    currentLifecycle = Lifecycle.DORMANT;
                }

                BlockState newState = state.setValue(LIFECYCLE, currentLifecycle);
                long regrowthTicks = (long) JSDConfig.TEA_FRUIT_REGROWTH_DAYS.get() * ICalendar.CALENDAR_TICKS_IN_DAY;

                if (state != newState && (currentLifecycle != Lifecycle.FLOWERING ||
                    Calendars.SERVER.getTicks() - plant.getLastPickedTick() > regrowthTicks)) {
                    level.setBlock(pos, newState, 3);
                }
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(LIFECYCLE) == Lifecycle.FRUITING) {
            level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.9f);
            if (!level.isClientSide()) {
                boolean isKnife = stack.is(TFCTags.Items.TOOLS_KNIFE);
                if (isKnife) {
                    // Нож: больше листа (2-3 шт.), расход 1 прочности, без тегов
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(JSDItems.FRESH_TEA_LEAF.get(), 2 + level.random.nextInt(2)));
                } else {
                    // Рука: меньше листа (1-2 шт.), шанс тега LARGE_LEAF для пуэра (ТЗ R08/R10)
                    ItemStack drop = new ItemStack(JSDItems.FRESH_TEA_LEAF.get(), 1 + (level.random.nextFloat() < 0.35f ? 1 : 0));
                    if (level.random.nextFloat() < 0.40f) {
                        drop.set(JSDDataComponents.LARGE_LEAF.get(), true);
                    }
                    ItemHandlerHelper.giveItemToPlayer(player, drop);
                }
            }
            JSDBerryBushBlockEntity.resetPickedTick(level, pos);
            level.setBlockAndUpdate(pos, state.setValue(LIFECYCLE, Lifecycle.HEALTHY));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
