package com.journey.jsdrinks.block;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.journey.jsdrinks.JSDConfig;
import com.journey.jsdrinks.block.entity.TeaBushBlockEntity;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.plant.ITallPlant;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.common.blocks.soil.HoeOverlayBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;


/**
 * A 2-block-tall seasonal tea bush (LOWER + UPPER).
 * <ul>
 *   <li>Implements {@link HoeOverlayBlock} for rich Jade/WAILA agriculture telemetry.</li>
 *   <li>Harvest with an empty hand gives 1–2 leaves with a chance of the {@code select} tag.</li>
 *   <li>Harvest with a knife gives 2–3 leaves (no tags) and costs 1 durability.</li>
 *   <li>After {@code JSDConfig.TEA_BUSH_TRANSFORM_YEARS} calendar years without harvest
 *       the bush irreversibly transforms into a tea tree.</li>
 *   <li>Frost-kill: dies when instant temperature ≤ {@code JSDConfig.TEA_FROST_KILL_TEMP}
 *       (default −5 °C).</li>
 * </ul>
 */
public class TeaBushBlock extends SeasonalPlantBlock implements HoeOverlayBlock {

    public static final EnumProperty<ITallPlant.Part> PART = TFCBlockStateProperties.TALL_PLANT_PART;
    public static final TagKey<Item> KNIVES_C_TAG = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/knife"));
    public static final VoxelShape LOWER_SHAPE = box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);
    public static final VoxelShape UPPER_SHAPE = box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public TeaBushBlock(ExtendedProperties properties, Lifecycle[] lifecycle, Supplier<ClimateRange> climateRange) {
        super(properties, climateRange, JSDItems.FRESH_TEA_LEAF, lifecycle);
        registerDefaultState(getStateDefinition().any()
            .setValue(LIFECYCLE, Lifecycle.HEALTHY)
            .setValue(STAGE, 0)
            .setValue(PART, ITallPlant.Part.LOWER));
    }

    // ------------------------------------------------------------------ Shape

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PART) == ITallPlant.Part.LOWER ? LOWER_SHAPE : UPPER_SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    // ---------------------------------------------------------- Block-Entity

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Both halves carry a BlockEntity so HoeOverlay/Jade functions on both upper and lower blocks
        return new TeaBushBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> givenType) {
        // Only the LOWER half ticks on the server to prevent double-processing
        if (level.isClientSide() || state.getValue(PART) != ITallPlant.Part.LOWER) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof TeaBushBlockEntity bush) {
                bush.serverTick(lvl, pos, st);
            }
        };
    }

    @Override
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, Consumer<Component> text, boolean isDebug) {
        BlockPos lowerPos = state.getValue(PART) == ITallPlant.Part.UPPER ? pos.below() : pos;
        BlockPos rootPos = lowerPos.below();
        final ClimateRange range = climateRange.get();
        final int hydration = getFruitBushHydrationFromRootPos(level, rootPos);
        text.accept(FarmlandBlock.getHydrationTooltip(range, false, hydration));
        text.accept(FarmlandBlock.getAverageTemperatureTooltip(level, lowerPos, range, false));

        Lifecycle currentStage = getLifecycleForCurrentMonth(level, lowerPos);
        if (!currentStage.active()) {
            text.accept(Component.translatable("tfc.tooltip.fruit_tree.sapling_wrong_month"));
        } else {
            text.accept(Component.translatable("tfc.tooltip.fruit_tree.growing"));
        }

        if (level.getBlockEntity(lowerPos) instanceof TeaBushBlockEntity bush) {
            int months = bush.getSeasonsWithoutHarvest() * ICalendar.MONTHS_IN_YEAR;
            int maxMonths = JSDConfig.TEA_BUSH_TRANSFORM_YEARS.get() * ICalendar.MONTHS_IN_YEAR;
            text.accept(Component.translatable("jsdrinks.tooltip.tea_bush.months_without_harvest", months, maxMonths));
        }
    }

    // -------------------------------------------------------- Lifecycle update
    // Overrides SeasonalPlantBlock.onUpdate() to use per-plant tea regrowth delay
    // instead of TFC's global fruitPickBloomDelayTicks config.
    //
    // Key fix: the bloom delay should only gate FLOWERING→FRUITING when FLOWERING
    // is the expected lifecycle for the current month. When FLOWERING is just a
    // transitional step toward FRUITING (e.g. August 2nd wave), no delay applies.

    @Override
    public void onUpdate(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity plant) {
            Lifecycle currentLifecycle = state.getValue(LIFECYCLE);
            Lifecycle expectedLifecycle = getLifecycleForCurrentMonth(level, pos);
            if (!checkAndSetDormant(level, pos, state, currentLifecycle, expectedLifecycle)) {
                final ClimateRange range = climateRange.get();
                final int hydration = getFruitBushHydrationFromRootPos(level, pos.below());

                if (range.checkBoth(hydration, Climate.getAverageTemperature(level, pos), false)) {
                    currentLifecycle = currentLifecycle.advanceTowards(expectedLifecycle);
                } else {
                    currentLifecycle = Lifecycle.DORMANT;
                }

                BlockState newState = state.setValue(LIFECYCLE, currentLifecycle);

                // Regrowth delay: prevent transition into FLOWERING until regrowthDays have passed since last harvest.
                boolean bloomDelayActive = false;
                if (currentLifecycle == Lifecycle.FLOWERING) {
                    long regrowthTicks = (long) JSDConfig.TEA_FRUIT_REGROWTH_DAYS.get() * ICalendar.CALENDAR_TICKS_IN_DAY;
                    bloomDelayActive = Calendars.SERVER.getTicks() - plant.getLastPickedTick() <= regrowthTicks;
                }

                if (state != newState && !bloomDelayActive) {
                    level.setBlock(pos, newState, 3);
                }
            }
        }
    }

    // ------------------------------------------------------- State / Survival

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1
            && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState().setValue(PART, ITallPlant.Part.LOWER);
        }
        return null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) == ITallPlant.Part.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(PART) == ITallPlant.Part.LOWER;
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState,
                                   LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        ITallPlant.Part part = state.getValue(PART);
        if (facing.getAxis() == Direction.Axis.Y) {
            if (part == ITallPlant.Part.LOWER && facing == Direction.UP) {
                if (!facingState.is(this)) return Blocks.AIR.defaultBlockState();
            }
            if (part == ITallPlant.Part.UPPER && facing == Direction.DOWN) {
                if (!facingState.is(this)) return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    // ---------------------------------------------------------- Placement

    /**
     * Places the UPPER half and resets the tick counter on the LOWER's block entity.
     * <p>
     * <b>Does NOT call {@code super.setPlacedBy()}</b> because
     * {@link SeasonalPlantBlock#setPlacedBy} delegates to
     * {@code BerryBushBlockEntity.reset(level, pos)} which performs a lookup via
     * {@code TFCBlockEntities.BERRY_BUSH} — that type does not match our
     * {@code JSDBlockEntities.TEA_BUSH}, so the counter would silently fail to reset.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlockAndUpdate(pos.above(), defaultBlockState()
            .setValue(PART, ITallPlant.Part.UPPER)
            .setValue(LIFECYCLE, state.getValue(LIFECYCLE)));

        // Direct instance reset — bypasses TFC static utility that uses the wrong BE type.
        if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity bushBE) {
            bushBE.resetCounter();
        }
    }

    // -------------------------------------------------------- Visibility helper
    // Widens protected → public so worldgen features in another package can call it.

    @Override
    public Lifecycle getLifecycleForCurrentMonth(Level level, BlockPos pos) {
        return super.getLifecycleForCurrentMonth(level, pos);
    }

    public static int getHydration(Level level, BlockPos rootPos) {
        return getFruitBushHydrationFromRootPos(level, rootPos);
    }

    // --------------------------------------------------------------- Random tick

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Only the LOWER half drives all logic.
        if (state.getValue(PART) != ITallPlant.Part.LOWER) return;

        // 0. Frost check: if instant temperature ≤ threshold, kill the bush.
        float instantTemp = Climate.getInstantTemperature(level, pos);
        if (instantTemp <= JSDConfig.TEA_FROST_KILL_TEMP.get()) {
            killBush(level, pos);
            return;
        }

        // 1. Run the standard SeasonalPlantBlock lifecycle progression
        //    (DORMANT ↔ HEALTHY ↔ FLOWERING ↔ FRUITING based on month & climate).
        //    We call onUpdate() directly instead of super.randomTick() because
        //    SeasonalPlantBlock does not override Block.randomTick(4-arg) — its
        //    helper takes 5 args and is invoked differently.
        onUpdate(level, pos, state);

        // Re-read — onUpdate may have changed LIFECYCLE or the block entirely
        // (e.g. if checkAndSetDormant replaced it).
        state = level.getBlockState(pos);
        if (!state.is(this)) return;   // block was replaced (shouldn't happen, but guard)

        // 2. Check bush → tree transformation.
        if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity bushBE) {
            ClimateRange range = climateRange.get();
            int hydration = getFruitBushHydrationFromRootPos(level, pos.below());
            float temp = Climate.getAverageTemperature(level, pos);
            boolean climateValid = range.checkBoth(hydration, temp, false);

            bushBE.checkYearlyGrowth(level, pos, state.getValue(LIFECYCLE), climateValid);
        }

        // 3. Synchronise the UPPER part's lifecycle with LOWER.
        state = level.getBlockState(pos);
        if (!state.is(this)) return;   // may have transformed into a tree

        BlockPos upperPos = pos.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (upperState.is(this)
            && upperState.getValue(LIFECYCLE) != state.getValue(LIFECYCLE)) {
            level.setBlockAndUpdate(upperPos,
                upperState.setValue(LIFECYCLE, state.getValue(LIFECYCLE)));
        }
    }

    /**
     * Destroys both halves of the tea bush (frost-kill or other death).
     * Replaces with dead bush plants for visual feedback.
     */
    private void killBush(ServerLevel level, BlockPos lowerPos) {
        BlockPos upperPos = lowerPos.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (upperState.is(this) && upperState.getValue(PART) == ITallPlant.Part.UPPER) {
            level.removeBlockEntity(upperPos);
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(),
                Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
        }
        level.removeBlockEntity(lowerPos);
        level.destroyBlock(lowerPos, false);
    }

    // -------------------------------------------------------- Destroy / Break

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        ITallPlant.Part part = state.getValue(PART);

        if (!level.isClientSide()) {
            if (player.isCreative()) {
                // Creative: silently remove the lower half if player broke upper
                if (part == ITallPlant.Part.UPPER) {
                    BlockPos below = pos.below();
                    BlockState belowState = level.getBlockState(below);
                    if (belowState.is(this) && belowState.getValue(PART) == ITallPlant.Part.LOWER) {
                        level.setBlock(below, Blocks.AIR.defaultBlockState(), 35);
                    }
                }
            } else {
                // Survival: drop 1× tea bush item (not sapling).
                // Breaking either UPPER or LOWER drops 1 bush.
                // The surviving half is removed cleanly by updateShape without dropping a duplicate.
                popResource(level, pos, new ItemStack(JSDItems.TEA_BUSH.get()));

                // If the bush was FRUITING, also drop a tea leaf.
                BlockPos lowerPos = part == ITallPlant.Part.UPPER ? pos.below() : pos;
                BlockState lowerState = level.getBlockState(lowerPos);
                if (lowerState.is(this) && lowerState.getValue(LIFECYCLE) == Lifecycle.FRUITING) {
                    popResource(level, pos, new ItemStack(JSDItems.FRESH_TEA_LEAF.get()));
                } else if (part == ITallPlant.Part.LOWER && state.getValue(LIFECYCLE) == Lifecycle.FRUITING) {
                    popResource(level, pos, new ItemStack(JSDItems.FRESH_TEA_LEAF.get()));
                }
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(JSDItems.TEA_BUSH.get());
    }

    // ----------------------------------------------------------- Harvest (RMB)

    /**
     * TZ §3.2 harvest rules:
     * <ul>
     *   <li><b>Knife:</b> 2–3 leaves, no data-component tags, −1 durability.</li>
     *   <li><b>Bare hand:</b> 1–2 leaves, 35 % chance of {@code select} tag.</li>
     *   <li>Both reset {@code seasonsWithoutHarvest} and {@code lastPickedTick}
     *       on the authoritative LOWER block entity.</li>
     * </ul>
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        // Resolve the LOWER and UPPER positions. Both halves route to the authoritative LOWER block.
        BlockPos lowerPos = state.getValue(PART) == ITallPlant.Part.UPPER ? pos.below() : pos;
        BlockPos upperPos = state.getValue(PART) == ITallPlant.Part.UPPER ? pos : pos.above();
        BlockState lowerState = level.getBlockState(lowerPos);

        if (!lowerState.is(this) || lowerState.getValue(PART) != ITallPlant.Part.LOWER) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Harvesting is ONLY allowed when the bush is FRUITING
        if (lowerState.getValue(LIFECYCLE) != Lifecycle.FRUITING) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
            SoundSource.PLAYERS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.9f);

        if (!level.isClientSide()) {
            boolean isKnife = stack.is(TFCTags.Items.TOOLS_KNIFE) || stack.is(KNIVES_C_TAG);

            if (isKnife) {
                // Нож с куста: 25% 1 лист, 75% 2 листа (все без тегов), расход 1 прочности
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                int count = level.random.nextFloat() < 0.25f ? 1 : 2;
                ItemHandlerHelper.giveItemToPlayer(player,
                    new ItemStack(JSDItems.FRESH_TEA_LEAF.get(), count));
            } else {
                // Рука с куста: 1 лист (75% без тега, 25% отборный)
                ItemStack drop = new ItemStack(JSDItems.FRESH_TEA_LEAF.get(), 1);
                if (level.random.nextFloat() < 0.25f) {
                    drop.set(JSDDataComponents.SELECT.get(), true);
                }
                ItemHandlerHelper.giveItemToPlayer(player, drop);
            }

            // Reset harvest tracking and fruit picked timer on the authoritative LOWER block entity.
            if (level.getBlockEntity(lowerPos) instanceof TeaBushBlockEntity bushBE) {
                bushBE.resetSeasonsWithoutHarvest();
                bushBE.resetLastPickedCounter();
            }

            // Reset both halves back to HEALTHY after harvest.
            level.setBlockAndUpdate(lowerPos,
                lowerState.setValue(LIFECYCLE, Lifecycle.HEALTHY));

            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.is(this) && upperState.getValue(PART) == ITallPlant.Part.UPPER) {
                level.setBlockAndUpdate(upperPos,
                    upperState.setValue(LIFECYCLE, Lifecycle.HEALTHY));
            }
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
