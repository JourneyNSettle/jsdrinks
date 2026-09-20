package com.journey.jsdrinks.registry;

import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.block.JSDCoffeeLeavesBlock;
import com.journey.jsdrinks.block.JSDFruitTreeSaplingBlock;
import com.journey.jsdrinks.block.JSDGrowingFruitTreeBranchBlock;
import com.journey.jsdrinks.block.JSDTeaLeavesBlock;
import com.journey.jsdrinks.block.TeaBushBlock;
import com.journey.jsdrinks.block.TeaPileBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeBranchBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.ICalendar;

import java.awt.Color;

import static net.dries007.tfc.common.blocks.plant.fruit.Lifecycle.*;

public class JSDBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, JourneyDrinks.MODID);

    // Tea Lifecycle: 2 windows (spring: Apr-May flowering/fruiting, late summer: Jul-Aug flowering/fruiting)
    public static final Lifecycle[] TEA_STAGES = new Lifecycle[]{
            DORMANT, HEALTHY, FLOWERING, FRUITING, HEALTHY, FLOWERING, FRUITING, HEALTHY, DORMANT, DORMANT, DORMANT, DORMANT
    };

    // Coffee Lifecycle: Summer fruiting (May-Jun flowering, Jul-Aug fruiting)
    public static final Lifecycle[] COFFEE_STAGES = new Lifecycle[]{
            DORMANT, DORMANT, HEALTHY, HEALTHY, FLOWERING, FLOWERING, FRUITING, FRUITING, HEALTHY, DORMANT, DORMANT, DORMANT
    };

    // --- Tea Bush ---
    public static final DeferredHolder<Block, TeaBushBlock> TEA_BUSH = BLOCKS.register("plant/tea_bush", () ->
            new TeaBushBlock(ExtendedProperties.of(MapColor.PLANT).strength(0.6f).noOcclusion().randomTicks().sound(SoundType.SWEET_BERRY_BUSH).flammableLikeLeaves(),
                    TEA_STAGES, JSDClimateRanges.TEA_BUSH));

    // --- Tea Tree ---
    public static final DeferredHolder<Block, FruitTreeBranchBlock> TEA_BRANCH = BLOCKS.register("plant/tea_branch", () ->
            new FruitTreeBranchBlock(ExtendedProperties.of(MapColor.WOOD).sound(SoundType.SCAFFOLDING).randomTicks().strength(1.0f).flammableLikeLogs(),
                    JSDClimateRanges.TEA_TREE));

    public static final DeferredHolder<Block, JSDTeaLeavesBlock> TEA_LEAVES = BLOCKS.register("plant/tea_leaves", () ->
            new JSDTeaLeavesBlock(ExtendedProperties.of(MapColor.PLANT).strength(0.5F).sound(SoundType.GRASS).randomTicks().noOcclusion().blockEntity(JSDBlockEntities.BERRY_BUSH).flammableLikeLeaves(),
                    TEA_STAGES, JSDClimateRanges.TEA_TREE, new Color(240, 240, 240).getRGB()));

    public static final DeferredHolder<Block, JSDGrowingFruitTreeBranchBlock> TEA_GROWING_BRANCH = BLOCKS.register("plant/tea_growing_branch", () ->
            new JSDGrowingFruitTreeBranchBlock(ExtendedProperties.of(MapColor.WOOD).sound(SoundType.SCAFFOLDING).randomTicks().strength(1.0f).blockEntity(JSDBlockEntities.TICKING_PLANT).flammableLikeLogs(),
                    TEA_BRANCH, () -> TEA_LEAVES.get(), JSDClimateRanges.TEA_TREE));

    public static final DeferredHolder<Block, JSDFruitTreeSaplingBlock> TEA_SAPLING = BLOCKS.register("plant/tea_sapling", () ->
            new JSDFruitTreeSaplingBlock(ExtendedProperties.of(MapColor.PLANT).noCollission().randomTicks().strength(0).sound(SoundType.GRASS).blockEntity(JSDBlockEntities.TICKING_PLANT).flammableLikeLeaves(),
                    TEA_GROWING_BRANCH, () -> 8 * ICalendar.CALENDAR_TICKS_IN_DAY, JSDClimateRanges.TEA_TREE, TEA_STAGES));

    public static final DeferredHolder<Block, FlowerPotBlock> POTTED_TEA_SAPLING = BLOCKS.register("plant/potted/tea_sapling", () ->
            new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, TEA_SAPLING, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ACACIA_SAPLING)));

    // --- Coffee Tree ---
    public static final DeferredHolder<Block, FruitTreeBranchBlock> COFFEE_BRANCH = BLOCKS.register("plant/coffee_branch", () ->
            new FruitTreeBranchBlock(ExtendedProperties.of(MapColor.WOOD).sound(SoundType.SCAFFOLDING).randomTicks().strength(1.0f).flammableLikeLogs(),
                    JSDClimateRanges.COFFEE_TREE));

    public static final DeferredHolder<Block, JSDCoffeeLeavesBlock> COFFEE_LEAVES = BLOCKS.register("plant/coffee_leaves", () ->
            new JSDCoffeeLeavesBlock(ExtendedProperties.of(MapColor.PLANT).strength(0.5F).sound(SoundType.GRASS).randomTicks().noOcclusion().blockEntity(JSDBlockEntities.BERRY_BUSH).flammableLikeLeaves(),
                    COFFEE_STAGES, JSDClimateRanges.COFFEE_TREE, new Color(255, 255, 255).getRGB()));

    public static final DeferredHolder<Block, JSDGrowingFruitTreeBranchBlock> COFFEE_GROWING_BRANCH = BLOCKS.register("plant/coffee_growing_branch", () ->
            new JSDGrowingFruitTreeBranchBlock(ExtendedProperties.of(MapColor.WOOD).sound(SoundType.SCAFFOLDING).randomTicks().strength(1.0f).blockEntity(JSDBlockEntities.TICKING_PLANT).flammableLikeLogs(),
                    COFFEE_BRANCH, () -> COFFEE_LEAVES.get(), JSDClimateRanges.COFFEE_TREE));

    public static final DeferredHolder<Block, JSDFruitTreeSaplingBlock> COFFEE_SAPLING = BLOCKS.register("plant/coffee_sapling", () ->
            new JSDFruitTreeSaplingBlock(ExtendedProperties.of(MapColor.PLANT).noCollission().randomTicks().strength(0).sound(SoundType.GRASS).blockEntity(JSDBlockEntities.TICKING_PLANT).flammableLikeLeaves(),
                    COFFEE_GROWING_BRANCH, () -> 8 * ICalendar.CALENDAR_TICKS_IN_DAY, JSDClimateRanges.COFFEE_TREE, COFFEE_STAGES));

    public static final DeferredHolder<Block, FlowerPotBlock> POTTED_COFFEE_SAPLING = BLOCKS.register("plant/potted/coffee_sapling", () ->
            new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, COFFEE_SAPLING, BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_ACACIA_SAPLING)));

    // --- Tea Piles ---
    public static final DeferredHolder<Block, TeaPileBlock> TEA_PILE = BLOCKS.register("tea_pile", () ->
            new TeaPileBlock(BlockBehaviour.Properties.of().sound(SoundType.GRASS).strength(0.2f).noOcclusion()));
}
