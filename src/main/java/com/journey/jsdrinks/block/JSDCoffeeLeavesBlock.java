package com.journey.jsdrinks.block;

import java.util.function.Supplier;

import com.journey.jsdrinks.JSDConfig;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

/**
 * Coffee tree leaves block.
 * <p>
 * TZ §3.4: coffee trees must be at least 2 TFC calendar years old before they
 * can bear fruit.  During the first 2 years FRUITING is suppressed (shows
 * FLOWERING instead).
 * <p>
 * The age is tracked via {@link JSDBerryBushBlockEntity#getPlacedTick()}, which
 * records the calendar tick at which the block entity was first created and is
 * never reset. This is more reliable than {@code getTicksSinceUpdate()} which
 * resets on growth events.
 * <p>
 * <b>Frost-kill:</b> dies when instant temperature ≤ {@link JSDConfig#COFFEE_FROST_KILL_TEMP}
 * (default 0 °C).
 */
public class JSDCoffeeLeavesBlock extends FruitTreeLeavesBlock {

    public JSDCoffeeLeavesBlock(ExtendedProperties properties, Lifecycle[] stages,
                                 Supplier<ClimateRange> climateRange, int flowerColor) {
        super(properties, JSDItems.COFFEE_CHERRY, stages, climateRange, flowerColor);
    }

    // Widen protected → public so worldgen features in another package can call it.
    @Override
    public Lifecycle getLifecycleForCurrentMonth(Level level, BlockPos pos) {
        return super.getLifecycleForCurrentMonth(level, pos);
    }

    // -------------------------------------------------------- Frost check

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Frost-kill: if instant temperature ≤ threshold, destroy the leaf block.
        float instantTemp = Climate.getInstantTemperature(level, pos);
        if (instantTemp <= JSDConfig.COFFEE_FROST_KILL_TEMP.get()) {
            level.removeBlockEntity(pos);
            level.destroyBlock(pos, false);
            return;
        }

        // Parent handles lifecycle progression via onUpdate().
        super.randomTick(state, level, pos, random);
    }

    // -------------------------------------------------------- Age-gated fruiting

    /**
     * Lifecycle update with 2-year age gate AND per-plant coffee regrowth delay.
     * <p>
     * Fully replaces SeasonalPlantBlock.onUpdate() to:
     * <ol>
     *   <li>Suppress FRUITING for young trees (&lt; 2 calendar years old).</li>
     *   <li>Use {@link JSDConfig#COFFEE_FRUIT_REGROWTH_DAYS} (default 8 days)
     *       instead of TFC's global {@code fruitPickBloomDelayTicks}.</li>
     * </ol>
     * <p>
     * The age gate uses {@link JSDBerryBushBlockEntity#getPlacedTick()}, which is
     * set once on block entity creation and never reset, providing reliable
     * absolute age measurement.
     * <ul>
     *   <li><b>Worldgen trees:</b> {@code WildCoffeeTreeFeature} calls
     *       {@code setPlacedTick(currentTick - 3yearTicks)} so the tree reads
     *       as ≥ 3 years old → fruit immediately.</li>
     *   <li><b>Player trees:</b> leaves are created with placedTick = current time
     *       → blocks fruiting for 2 years.</li>
     * </ul>
     */
    @Override
    public void onUpdate(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PERSISTENT)) return;

        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity plant) {
            // Age gate using placedTick for reliable absolute age measurement
            long currentTick = Calendars.get(level).getTicks();
            long twoYearsTicks = 2L * Calendars.get(level).getCalendarTicksInYear();

            boolean isYoung = false;
            if (plant instanceof JSDBerryBushBlockEntity jsdPlant) {
                long ageTicks = currentTick - jsdPlant.getPlacedTick();
                isYoung = ageTicks < twoYearsTicks;
            }

            if (isYoung) {
                // Young tree: suppress FRUITING → show FLOWERING instead.
                Lifecycle expectedLifecycle = getLifecycleForCurrentMonth(level, pos);
                if (expectedLifecycle == Lifecycle.FRUITING) {
                    if (state.getValue(LIFECYCLE) != Lifecycle.FLOWERING) {
                        level.setBlockAndUpdate(pos, state.setValue(LIFECYCLE, Lifecycle.FLOWERING));
                    }
                    return;
                }
            }

            // Mature tree (or non-FRUITING month for young tree): standard lifecycle
            // with coffee-specific regrowth delay.
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

                // Regrowth delay: prevent transition into FLOWERING until coffeeRegrowthDays have passed since last harvest.
                boolean bloomDelayActive = false;
                if (currentLifecycle == Lifecycle.FLOWERING) {
                    long regrowthTicks = (long) JSDConfig.COFFEE_FRUIT_REGROWTH_DAYS.get() * ICalendar.CALENDAR_TICKS_IN_DAY;
                    bloomDelayActive = Calendars.SERVER.getTicks() - plant.getLastPickedTick() <= regrowthTicks;
                }

                if (state != newState && !bloomDelayActive) {
                    level.setBlock(pos, newState, 3);
                }
            }
        }
    }

    // -------------------------------------------------------------- Harvest (RMB)

    /**
     * TZ §3.5 harvest rules for coffee:
     * <ul>
     *   <li><b>Knife:</b> 2–3 cherries, no {@code quality} tag, −1 durability.</li>
     *   <li><b>Bare hand:</b> 1–2 cherries, always tagged with
     *       {@code quality} 1–5.</li>
     * </ul>
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (state.getValue(LIFECYCLE) == Lifecycle.FRUITING) {
            level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.PLAYERS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.9f);

            if (!level.isClientSide()) {
                boolean isKnife = stack.is(TFCTags.Items.TOOLS_KNIFE) || stack.is(TeaBushBlock.KNIVES_C_TAG);

                if (isKnife) {
                    // Knife: more cherries, no quality tag, costs durability.
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    ItemHandlerHelper.giveItemToPlayer(player,
                        new ItemStack(JSDItems.COFFEE_CHERRY.get(), 2 + level.random.nextInt(2)));
                } else {
                    // Bare hand: fewer cherries, always tagged with quality 1–5.
                    ItemStack drop = new ItemStack(JSDItems.COFFEE_CHERRY.get(),
                        1 + (level.random.nextFloat() < 0.35f ? 1 : 0));
                    drop.set(JSDDataComponents.QUALITY.get(), 1 + level.random.nextInt(5));
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
