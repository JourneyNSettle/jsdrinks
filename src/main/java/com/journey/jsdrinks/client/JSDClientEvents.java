package com.journey.jsdrinks.client;

import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.dries007.tfc.client.extensions.FluidRendererExtension;
import net.dries007.tfc.common.fluids.FluidHolder;
import net.dries007.tfc.common.fluids.TFCFluids;

public class JSDClientEvents {

    private static final ResourceLocation WATER_STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation WATER_OVERLAY = ResourceLocation.withDefaultNamespace("block/water_overlay");
    private static final ResourceLocation UNDERWATER = ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");

    public static void init(IEventBus modBus) {
        modBus.addListener(JSDClientEvents::registerClientExtensions);
        NeoForge.EVENT_BUS.addListener(JSDClientEvents::onItemTooltip);
    }

    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        // Coffee fluids
        registerFluid(event, JSDFluids.COFFEE, 0xFF3D2314);
        registerFluid(event, JSDFluids.STRONG_COFFEE, 0xFF24150B);
        registerFluid(event, JSDFluids.COFFEE_WITH_MILK, 0xFF8A674A);
        registerFluid(event, JSDFluids.STRONG_COFFEE_WITH_MILK, 0xFF6A4D34);

        // Tea fluids
        registerFluid(event, JSDFluids.WHITE_TEA, 0xFFEBCD58);
        registerFluid(event, JSDFluids.GREEN_TEA, 0xFF6E8B3D);
        registerFluid(event, JSDFluids.YELLOW_TEA, 0xFFD4AF37);
        registerFluid(event, JSDFluids.RED_TEA, 0xFF8A3324);
        registerFluid(event, JSDFluids.PUERH_TEA, 0xFF4A1A12);
        registerFluid(event, JSDFluids.STRONG_PUERH_TEA, 0xFF2D0F0A);
    }

    private static void registerFluid(RegisterClientExtensionsEvent event, FluidHolder<?> holder, int color) {
        event.registerFluidType(
            new FluidRendererExtension(
                TFCFluids.ALPHA_MASK | color,
                WATER_STILL,
                WATER_FLOW,
                WATER_OVERLAY,
                UNDERWATER
            ),
            holder.getType()
        );
    }

    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (stack.has(JSDDataComponents.SELECT.get())) {
            Boolean val = stack.get(JSDDataComponents.SELECT.get());
            if (val != null && val) {
                event.getToolTip().add(Component.translatable("tooltip.jsdrinks.select").withStyle(ChatFormatting.GOLD));
            }
        }
        if (stack.has(JSDDataComponents.LARGE_LEAF.get())) {
            Boolean val = stack.get(JSDDataComponents.LARGE_LEAF.get());
            if (val != null && val) {
                event.getToolTip().add(Component.translatable("tooltip.jsdrinks.large_leaf").withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        if (stack.has(JSDDataComponents.BURNT.get())) {
            Boolean val = stack.get(JSDDataComponents.BURNT.get());
            if (val != null && val) {
                event.getToolTip().add(Component.translatable("tooltip.jsdrinks.burnt").withStyle(ChatFormatting.RED));
            }
        }
        if (stack.has(JSDDataComponents.QUALITY.get())) {
            Integer quality = stack.get(JSDDataComponents.QUALITY.get());
            if (quality != null) {
                event.getToolTip().add(Component.translatable("tooltip.jsdrinks.quality", quality).withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
