package com.journey.jsdrinks.block;

import java.util.function.Supplier;

import com.journey.jsdrinks.block.entity.JSDTickingPlantBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.fruit.GrowingFruitTreeBranchBlock;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;

/**
 * Growing branch block for tea / coffee fruit trees.
 * <p>
 * <b>Critical override:</b> {@link #randomTick} fully replaces the parent's
 * implementation because {@link GrowingFruitTreeBranchBlock#randomTick} calls
 * {@code TickingPlantBlockEntity.reset(level, pos)} — a static utility that
 * performs a lookup via {@code TFCBlockEntities.TICK_COUNTING_PLANT}.  Our
 * blocks register their own BE type ({@code JSDBlockEntities.TICKING_PLANT}),
 * so the TFC lookup silently fails and tree growth breaks.
 * <p>
 * The inherited {@link GrowingFruitTreeBranchBlock#tick tick()},
 * {@link GrowingFruitTreeBranchBlock#grow grow()},
 * {@code placeGrownFlower()}, {@code placeBody()}, and {@code addLeaves()}
 * are <em>not</em> overridden because they use polymorphic
 * {@code instanceof TickingPlantBlockEntity} / {@code BerryBushBlockEntity}
 * checks and direct instance method calls — both of which resolve correctly
 * to our subclassed BE types at runtime.
 */
public class JSDGrowingFruitTreeBranchBlock extends GrowingFruitTreeBranchBlock {

    /**
     * Own copy of {@code climateRange} because the parent stores it in a
     * {@code private} field inaccessible to subclasses.
     */
    private final Supplier<ClimateRange> climateRange;

    public JSDGrowingFruitTreeBranchBlock(ExtendedProperties properties,
                                           Supplier<? extends Block> body,
                                           Supplier<? extends Block> leaves,
                                           Supplier<ClimateRange> climateRange) {
        super(properties, body, leaves, climateRange);
        this.climateRange = climateRange;
    }

    /**
     * Full replacement of {@link GrowingFruitTreeBranchBlock#randomTick}.
     * <p>
     * Logic mirrors the TFC original:
     * <ol>
     *   <li>Evaluate hydration + temperature against the climate range.</li>
     *   <li>Bad climate &amp; non-natural → reset growth counter (direct
     *       {@code instanceof} instead of the TFC static utility).</li>
     *   <li>Good climate (or natural tree) → delegate to
     *       {@link GrowingFruitTreeBranchBlock#tick tick()} which calculates
     *       growth cycles and calls {@code grow()} to expand the tree.
     *       {@code tick()} uses polymorphic {@code instanceof} internally,
     *       so it is safe for our custom BE types.</li>
     * </ol>
     * <b>Does NOT call {@code super.randomTick()}.</b>
     */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        final int hydration = getFruitBranchHydration(level, pos);
        final float temp = Climate.getAverageTemperature(level, pos);

        if (!climateRange.get().checkBoth(hydration, temp, false) && !state.getValue(NATURAL)) {
            // Bad climate, non-natural: reset growth counter.
            // Use direct instanceof to bypass TickingPlantBlockEntity.reset()
            // which looks up TFCBlockEntities.TICK_COUNTING_PLANT.
            if (level.getBlockEntity(pos) instanceof JSDTickingPlantBlockEntity counter) {
                counter.resetCounter();
            }
        } else {
            // Good climate (or natural): run growth logic.
            // GrowingFruitTreeBranchBlock.tick() internally:
            //   1. Calls super.tick() → FruitTreeBranchBlock.tick() (structural, no BE lookup)
            //   2. instanceof TickingPlantBlockEntity counter  → matches JSDTickingPlantBlockEntity
            //   3. counter.resetCounter() / grow()             → direct instance calls, safe
            this.tick(state, level, pos, random);
        }

        // ──── DO NOT call super.randomTick() ────
        // GrowingFruitTreeBranchBlock.randomTick() executes:
        //   TickingPlantBlockEntity.reset(level, pos)
        //     → level.getBlockEntity(pos, TFCBlockEntities.TICK_COUNTING_PLANT.get())
        //     → returns Optional.empty() for JSDBlockEntities.TICKING_PLANT
        //     → counter never resets → tree growth silently breaks
        //
        // The only other call in the parent's randomTick is
        //   super.randomTick(state, level, pos, random)  →  FruitTreeBranchBlock
        // which is a no-op (Block.randomTick default).
    }
}
