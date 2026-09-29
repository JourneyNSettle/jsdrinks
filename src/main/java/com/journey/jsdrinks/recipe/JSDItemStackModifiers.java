package com.journey.jsdrinks.recipe;

import com.journey.jsdrinks.registry.JSDDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.dries007.tfc.common.recipes.RecipeHelpers;
import net.dries007.tfc.common.recipes.outputs.ItemStackModifier;
import net.dries007.tfc.common.recipes.outputs.ItemStackModifierType;
import net.dries007.tfc.common.recipes.outputs.ItemStackModifiers;

import static com.journey.jsdrinks.JourneyDrinks.MOD_ID;

public class JSDItemStackModifiers {
    public static final DeferredRegister<ItemStackModifierType<?>> MODIFIERS = DeferredRegister.create(ItemStackModifiers.KEY, MOD_ID);

    public static final DeferredHolder<ItemStackModifierType<?>, ItemStackModifierType<CopyComponentsModifier>> COPY_COMPONENTS =
        MODIFIERS.register("copy_components", () -> new ItemStackModifierType<>(MapCodec.unit(CopyComponentsModifier.INSTANCE), StreamCodec.unit(CopyComponentsModifier.INSTANCE)));

    public static final DeferredHolder<ItemStackModifierType<?>, ItemStackModifierType<RoastModifier>> ROAST =
        MODIFIERS.register("roast", () -> new ItemStackModifierType<>(MapCodec.unit(RoastModifier.INSTANCE), StreamCodec.unit(RoastModifier.INSTANCE)));

    public static final DeferredHolder<ItemStackModifierType<?>, ItemStackModifierType<AddBurntModifier>> ADD_BURNT =
        MODIFIERS.register("add_burnt", () -> new ItemStackModifierType<>(MapCodec.unit(AddBurntModifier.INSTANCE), StreamCodec.unit(AddBurntModifier.INSTANCE)));

    public enum CopyComponentsModifier implements ItemStackModifier {
        INSTANCE;

        @Override
        public boolean dependsOnInput() {
            return true;
        }

        @Override
        public ItemStack apply(ItemStack stack, ItemStack input, Context context) {
            if (input.has(JSDDataComponents.SELECT.get())) {
                stack.set(JSDDataComponents.SELECT.get(), input.get(JSDDataComponents.SELECT.get()));
            }
            if (input.has(JSDDataComponents.BURNT.get())) {
                stack.set(JSDDataComponents.BURNT.get(), input.get(JSDDataComponents.BURNT.get()));
            }
            if (input.has(JSDDataComponents.LARGE_LEAF.get())) {
                stack.set(JSDDataComponents.LARGE_LEAF.get(), input.get(JSDDataComponents.LARGE_LEAF.get()));
            }
            if (input.has(JSDDataComponents.QUALITY.get())) {
                stack.set(JSDDataComponents.QUALITY.get(), input.get(JSDDataComponents.QUALITY.get()));
            }
            return stack;
        }

        @Override
        public ItemStackModifierType<?> type() {
            return COPY_COMPONENTS.get();
        }
    }

    public enum RoastModifier implements ItemStackModifier {
        INSTANCE;

        @Override
        public boolean dependsOnInput() {
            return true;
        }

        @Override
        public ItemStack apply(ItemStack stack, ItemStack input, Context context) {
            if (input.has(JSDDataComponents.QUALITY.get())) {
                stack.set(JSDDataComponents.QUALITY.get(), input.get(JSDDataComponents.QUALITY.get()));
            }
            if (input.has(JSDDataComponents.BURNT.get())) {
                stack.set(JSDDataComponents.BURNT.get(), input.get(JSDDataComponents.BURNT.get()));
            }
            final Player player = RecipeHelpers.getCraftingPlayer();
            if (player != null && !player.level().isClientSide) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            return stack;
        }

        @Override
        public ItemStackModifierType<?> type() {
            return ROAST.get();
        }
    }

    public enum AddBurntModifier implements ItemStackModifier {
        INSTANCE;

        @Override
        public ItemStack apply(ItemStack stack, ItemStack input, Context context) {
            stack.set(JSDDataComponents.BURNT.get(), true);
            final Player player = RecipeHelpers.getCraftingPlayer();
            if (player != null && !player.level().isClientSide) {
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.7f, 1.5f);
            }
            return stack;
        }

        @Override
        public ItemStackModifierType<?> type() {
            return ADD_BURNT.get();
        }
    }
}
