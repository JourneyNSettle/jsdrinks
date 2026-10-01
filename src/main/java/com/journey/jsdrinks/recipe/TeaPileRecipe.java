package com.journey.jsdrinks.recipe;

import java.util.List;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.recipes.outputs.ItemStackProvider;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TeaPileRecipe implements Recipe<TeaPileInput> {

    public enum TimeUnit implements StringRepresentable {
        DAYS("days"),
        MONTHS("months"),
        TICKS("ticks");

        public static final Codec<TimeUnit> CODEC = StringRepresentable.fromEnum(TimeUnit::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, TimeUnit> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC).cast();

        private final String name;

        TimeUnit(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum SurfaceType implements StringRepresentable {
        ANY("any"),
        SOLID("solid"),
        SOIL("soil");

        public static final Codec<SurfaceType> CODEC = StringRepresentable.fromEnum(SurfaceType::values);
        public static final StreamCodec<RegistryFriendlyByteBuf, SurfaceType> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC).cast();

        private final String name;

        SurfaceType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean test(BlockState state) {
            return switch (this) {
                case ANY -> true;
                case SOIL -> state.is(BlockTags.DIRT) || state.is(TFCTags.Blocks.DIRT) || state.is(TFCTags.Blocks.GRASS);
                case SOLID -> !(state.is(BlockTags.DIRT) || state.is(TFCTags.Blocks.DIRT) || state.is(TFCTags.Blocks.GRASS));
            };
        }
    }

    public record ChanceOutput(
        ItemStackProvider result,
        float chance,
        boolean rotten,
        int targetStage
    ) {
        public static final Codec<ChanceOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStackProvider.CODEC.fieldOf("result").forGetter(ChanceOutput::result),
            Codec.FLOAT.fieldOf("chance").forGetter(ChanceOutput::chance),
            Codec.BOOL.optionalFieldOf("rotten", false).forGetter(ChanceOutput::rotten),
            Codec.INT.optionalFieldOf("stage", 2).forGetter(ChanceOutput::targetStage)
        ).apply(i, ChanceOutput::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ChanceOutput> STREAM_CODEC = StreamCodec.composite(
            ItemStackProvider.STREAM_CODEC, ChanceOutput::result,
            ByteBufCodecs.FLOAT, ChanceOutput::chance,
            ByteBufCodecs.BOOL, ChanceOutput::rotten,
            ByteBufCodecs.VAR_INT, ChanceOutput::targetStage,
            ChanceOutput::new
        );
    }

    public static final MapCodec<TeaPileRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Ingredient.CODEC.fieldOf("ingredient").forGetter(c -> c.ingredient),
        Codec.BOOL.optionalFieldOf("requires_large_leaf", false).forGetter(c -> c.requiresLargeLeaf),
        Codec.INT.fieldOf("duration").forGetter(c -> c.duration),
        TimeUnit.CODEC.optionalFieldOf("time_unit", TimeUnit.DAYS).forGetter(c -> c.timeUnit),
        SurfaceType.CODEC.optionalFieldOf("surface", SurfaceType.ANY).forGetter(c -> c.surface),
        ChanceOutput.CODEC.listOf().fieldOf("outputs").forGetter(c -> c.outputs)
    ).apply(i, TeaPileRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TeaPileRecipe> STREAM_CODEC = StreamCodec.composite(
        Ingredient.CONTENTS_STREAM_CODEC, c -> c.ingredient,
        ByteBufCodecs.BOOL, c -> c.requiresLargeLeaf,
        ByteBufCodecs.VAR_INT, c -> c.duration,
        TimeUnit.STREAM_CODEC, c -> c.timeUnit,
        SurfaceType.STREAM_CODEC, c -> c.surface,
        ChanceOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), c -> c.outputs,
        TeaPileRecipe::new
    );

    private final Ingredient ingredient;
    private final boolean requiresLargeLeaf;
    private final int duration;
    private final TimeUnit timeUnit;
    private final SurfaceType surface;
    private final List<ChanceOutput> outputs;

    public TeaPileRecipe(
        Ingredient ingredient,
        boolean requiresLargeLeaf,
        int duration,
        TimeUnit timeUnit,
        SurfaceType surface,
        List<ChanceOutput> outputs
    ) {
        this.ingredient = ingredient;
        this.requiresLargeLeaf = requiresLargeLeaf;
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.surface = surface;
        this.outputs = outputs;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public boolean requiresLargeLeaf() {
        return requiresLargeLeaf;
    }

    public int getDuration() {
        return duration;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public SurfaceType getSurface() {
        return surface;
    }

    public List<ChanceOutput> getOutputs() {
        return outputs;
    }

    public long getDurationTicks(Level level) {
        return switch (timeUnit) {
            case DAYS -> (long) duration * ICalendar.CALENDAR_TICKS_IN_DAY;
            case MONTHS -> (long) duration * Calendars.get(level).getCalendarTicksInMonth();
            case TICKS -> (long) duration;
        };
    }

    @Nullable
    public ChanceOutput rollOutcome(RandomSource random) {
        if (outputs.isEmpty()) return null;
        float roll = random.nextFloat();
        float cumulative = 0.0f;
        for (ChanceOutput out : outputs) {
            cumulative += out.chance();
            if (roll < cumulative) {
                return out;
            }
        }
        return outputs.get(outputs.size() - 1);
    }

    public List<ItemStack> getDisplaySurfaceBlocks() {
        return switch (surface) {
            case ANY -> List.of(
                new ItemStack(Items.DIRT),
                new ItemStack(Items.GRASS_BLOCK),
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.STONE)
            );
            case SOLID -> List.of(
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.STONE),
                new ItemStack(Items.COBBLESTONE),
                new ItemStack(Items.BRICKS)
            );
            case SOIL -> List.of(
                new ItemStack(Items.DIRT),
                new ItemStack(Items.GRASS_BLOCK),
                new ItemStack(Items.PODZOL),
                new ItemStack(Items.COARSE_DIRT)
            );
        };
    }

    public Component getSurfaceDescription() {
        return switch (surface) {
            case ANY -> Component.translatable("jsdrinks.jei.tea_pile.surface.any");
            case SOLID -> Component.translatable("jsdrinks.jei.tea_pile.surface.solid");
            case SOIL -> Component.translatable("jsdrinks.jei.tea_pile.surface.soil");
        };
    }

    public Component getDurationText() {
        if (timeUnit == TimeUnit.DAYS && duration == 1) {
            return Component.translatable("jsdrinks.jei.tea_pile.duration.1_day");
        }
        if (timeUnit == TimeUnit.MONTHS && duration == 2) {
            return Component.translatable("jsdrinks.jei.tea_pile.duration.2_months");
        }
        return Component.literal(duration + " " + timeUnit.getSerializedName());
    }

    @Override
    public boolean matches(TeaPileInput input, Level level) {
        if (!ingredient.test(input.stack())) return false;
        if (requiresLargeLeaf && !Boolean.TRUE.equals(input.stack().get(JSDDataComponents.LARGE_LEAF.get()))) {
            return false;
        }
        return surface.test(input.surfaceBlock());
    }

    @Override
    public ItemStack assemble(TeaPileInput input, HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).result().getSingleStack(input.stack());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).result().getEmptyStack();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return JSDRecipeSerializers.TEA_PILE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return JSDRecipeTypes.TEA_PILE.get();
    }
}
