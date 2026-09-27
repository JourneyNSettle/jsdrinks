package com.journey.jsdrinks.block;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeLeavesBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;

import com.journey.jsdrinks.JSDConfig;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;

/**
 * Tea tree leaves block.
 * <p>
 * Extends {@link FruitTreeLeavesBlock} with:
 * <ul>
 *   <li>Per-plant tea regrowth delay ({@link JSDConfig#TEA_FRUIT_REGROWTH_DAYS})
 *       instead of TFC's global {@code fruitPickBloomDelayTicks}.</li>
 *   <li>Fixed bloom delay logic: delay only gates FLOWERING→FRUITING when FLOWERING
 *       is the expected month lifecycle (April). When FLOWERING is a transitional step
 *       toward a FRUITING month (August 2nd wave), no delay applies.</li>
 *   <li>Frost-kill: leaf block is destroyed when instant temperature ≤
 *       {@link JSDConfig#TEA_FROST_KILL_TEMP} (default −5 °C).</li>
 *   <li>Proper hydration via stem → root soil traversal.</li>
 * </ul>
 */
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

    // -------------------------------------------------------- Hydration helper
    // The leaf block entity stores stemPos (set during tree growth or placement).
    // To find the root soil, we walk DOWN from stemPos through branch/trunk blocks.

    /**
     * Walks downward from {@code stemPos} through fruit-tree branch blocks
     * until it reaches the soil (or a non-branch block). Returns the hydration
     * at the block directly below the lowest branch.
     */
    private static int getHydrationFromStem(Level level, BlockPos stemPos) {
        BlockPos.MutableBlockPos cursor = stemPos.mutable();
        // Walk down through branch blocks to find the root soil
        for (int i = 0; i < 16; i++) {
            BlockPos below = cursor.below();
            BlockState belowState = level.getBlockState(below);
            if (Helpers.isBlock(belowState, TFCTags.Blocks.FRUIT_TREE_BRANCH)) {
                cursor.move(Direction.DOWN);
            } else {
                // Found the soil block
                return getFruitBushHydrationFromRootPos(level, cursor.below());
            }
        }
        // Fallback: just use stemPos.below()
        return getFruitBushHydrationFromRootPos(level, stemPos.below());
    }

    // -------------------------------------------------------- Hoe overlay
    // Override to properly resolve hydration via stem → root soil traversal.

    @Override
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, Consumer<Component> text, boolean isDebug) {
        final ClimateRange range = climateRange.get();

        final BlockPos stemPos;
        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity bush) {
            stemPos = bush.getStemPos();
        } else {
            stemPos = pos;
        }

        final int hydration = getHydrationFromStem(level, stemPos);
        text.accept(FarmlandBlock.getHydrationTooltip(range, false, hydration));
        text.accept(FarmlandBlock.getAverageTemperatureTooltip(level, stemPos, range, false));
    }

    // -------------------------------------------------------- Frost check

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Frost-kill: if instant temperature ≤ threshold, destroy the leaf block.
        float instantTemp = Climate.getInstantTemperature(level, pos);
        if (instantTemp <= JSDConfig.TEA_FROST_KILL_TEMP.get()) {
            level.removeBlockEntity(pos);
            level.destroyBlock(pos, false);
            return;
        }

        // Parent handles lifecycle progression via onUpdate().
        super.randomTick(state, level, pos, random);
    }

    // -------------------------------------------------------- Lifecycle update
    // Overrides to use per-plant tea regrowth delay (JSDConfig.TEA_FRUIT_REGROWTH_DAYS)
    // instead of TFC's global fruitPickBloomDelayTicks config.
    // Also uses stem → root soil traversal for hydration.
    // Fixed: bloom delay only applies when FLOWERING is the expected month lifecycle.

    @Override
    public void onUpdate(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PERSISTENT)) return;

        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity plant) {
            Lifecycle currentLifecycle = state.getValue(LIFECYCLE);
            Lifecycle expectedLifecycle = getLifecycleForCurrentMonth(level, pos);
            if (!checkAndSetDormant(level, pos, state, currentLifecycle, expectedLifecycle)) {
                final ClimateRange range = climateRange.get();
                final BlockPos stemPos = plant.getStemPos();
                final int hydration = getHydrationFromStem(level, stemPos);

                if (range.checkBoth(hydration, Climate.getAverageTemperature(level, stemPos), false)) {
                    currentLifecycle = currentLifecycle.advanceTowards(expectedLifecycle);
                } else {
                    currentLifecycle = Lifecycle.DORMANT;
                }

                BlockState newState = state.setValue(LIFECYCLE, currentLifecycle);

                // Apply bloom delay ONLY when FLOWERING is both the current computed state
                // AND the expected month lifecycle. When FLOWERING is a transitional step
                // toward a FRUITING month (e.g. tea August 2nd wave), no delay applies.
                boolean bloomDelayActive = false;
                if (currentLifecycle == Lifecycle.FLOWERING && expectedLifecycle == Lifecycle.FLOWERING) {
                    long regrowthTicks = (long) JSDConfig.TEA_FRUIT_REGROWTH_DAYS.get() * ICalendar.CALENDAR_TICKS_IN_DAY;
                    bloomDelayActive = Calendars.SERVER.getTicks() - plant.getLastPickedTick() <= regrowthTicks;
                }

                if (state != newState && !bloomDelayActive) {
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
