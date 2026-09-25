package com.journey.jsdrinks.worldgen.feature;

import com.journey.jsdrinks.block.JSDCoffeeLeavesBlock;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.mojang.serialization.Codec;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeBranchBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WildCoffeeTreeFeature extends Feature<NoneFeatureConfiguration> {

    public WildCoffeeTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource rand = context.random();

        if (!Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.BUSH_PLANTABLE_ON)) {
            return false;
        }

        int trunkHeight = 2 + rand.nextInt(2); // 2-3 блока ствола
        for (int y = 0; y < trunkHeight + 2; y++) {
            if (!EnvironmentHelpers.isWorldgenReplaceable(level, pos.above(y))) {
                return false;
            }
        }

        FruitTreeBranchBlock branchBlock = JSDBlocks.COFFEE_BRANCH.get();
        JSDCoffeeLeavesBlock leavesBlock = JSDBlocks.COFFEE_LEAVES.get();
        Lifecycle lifecycle = leavesBlock.getLifecycleForCurrentMonth(level.getLevel(), pos);
        if (lifecycle == Lifecycle.DORMANT) {
            lifecycle = Lifecycle.HEALTHY;
        }

        BlockPos stemPos = pos;
        // ТЗ R23: дикое кофейное дерево имеет возраст >= 3 лет → плоды доступны сразу
        // resetCounter() сначала устанавливает lastUpdateTick = Calendars.SERVER.getTicks(),
        // затем increaseCounter() делает lastUpdateTick -= threeYearsTicks,
        // итого счётчик показывает «прошло 3 года» — без риска переполнения/нуля.
        long threeYearsTicks = 3L * Calendars.get(level.getLevel()).getCalendarTicksInYear();

        // Размещение ствола
        for (int y = 0; y < trunkHeight; y++) {
            setBlock(level, pos.above(y), branchBlock.defaultBlockState());
        }

        // Горизонтальные ветви
        BlockPos topPos = pos.above(trunkHeight - 1);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int branchLen = 1 + rand.nextInt(2);
            BlockPos cur = topPos;
            for (int step = 1; step <= branchLen; step++) {
                cur = cur.relative(dir);
                if (EnvironmentHelpers.isWorldgenReplaceable(level, cur)) {
                    setBlock(level, cur, branchBlock.defaultBlockState());
                    placeLeavesAround(level, cur, leavesBlock, lifecycle, stemPos, threeYearsTicks);
                }
            }
        }

        // Верхняя крона
        placeLeavesAround(level, pos.above(trunkHeight), leavesBlock, lifecycle, stemPos, threeYearsTicks);

        return true;
    }

    private void placeLeavesAround(WorldGenLevel level, BlockPos center, JSDCoffeeLeavesBlock leavesBlock, Lifecycle lifecycle, BlockPos stemPos, long ageTicks) {
        BlockState leafState = leavesBlock.defaultBlockState().setValue(JSDCoffeeLeavesBlock.LIFECYCLE, lifecycle);
        for (Direction d : Direction.values()) {
            BlockPos p = center.relative(d);
            if (EnvironmentHelpers.isWorldgenReplaceable(level, p)) {
                setBlock(level, p, leafState);
                // Используем аддонный JSDBerryBushBlockEntity — TFC BerryBushBlockEntity здесь не совпадает по типу BE
                if (level.getBlockEntity(p) instanceof JSDBerryBushBlockEntity be) {
                    be.setStemPos(stemPos);
                    be.resetCounter();         // ОБЯЗАТЕЛЬНО сначала reset — устанавливает lastUpdateTick в текущий момент
                    be.increaseCounter(ageTicks); // затем сдвигаем на 3 года назад
                }
            }
        }
    }
}
