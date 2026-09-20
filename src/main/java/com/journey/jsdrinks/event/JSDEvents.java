package com.journey.jsdrinks.event;

import com.journey.jsdrinks.registry.JSDFluids;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.IHeat;

import static com.journey.jsdrinks.JourneyDrinks.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class JSDEvents {

    // Cached Cold Sweat check
    private static Boolean coldSweatLoaded = null;

    private static boolean isColdSweatLoaded() {
        if (coldSweatLoaded == null) {
            coldSweatLoaded = ModList.get().isLoaded("coldsweat");
        }
        return coldSweatLoaded;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        // Check every 30 ticks (~1.5s)
        if (level.getGameTime() % 30 != 0) return;

        BlockPos playerPos = player.blockPosition();
        // Check 3x3x3 around player for any block entity cooking coffee beans
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-3, -2, -3), playerPos.offset(3, 2, 3))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                if (handler != null) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        var stack = handler.getStackInSlot(i);
                        if (stack.is(JSDItems.DRIED_COFFEE_BEAN.get()) || stack.is(JSDItems.ROASTED_COFFEE_BEAN.get())) {
                            IHeat heat = HeatCapability.get(stack);
                            if (heat != null && heat.getTemperature() >= 180.0f) {
                                // Play coffee roasting crackle near player
                                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.45f, 1.2f + level.random.nextFloat() * 0.4f);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * Cache the fluid in the vessel before drinking starts, so we can check it in Finish.
     */
    private static final ThreadLocal<Fluid> DRINKING_FLUID = new ThreadLocal<>();

    @SubscribeEvent
    public static void onStartUsingItem(LivingEntityUseItemEvent.Start event) {
        if (!isColdSweatLoaded()) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player)) return;

        ItemStack stack = event.getItem();
        IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler != null) {
            FluidStack fluidStack = fluidHandler.getFluidInTank(0);
            if (!fluidStack.isEmpty()) {
                Fluid fluid = fluidStack.getFluid();
                if (fluid == JSDFluids.WHITE_TEA.getSource() || fluid == JSDFluids.RED_TEA.getSource()) {
                    DRINKING_FLUID.set(fluid);
                    return;
                }
            }
        }
        DRINKING_FLUID.remove();
    }

    @SubscribeEvent
    public static void onFinishDrinking(LivingEntityUseItemEvent.Finish event) {
        if (!isColdSweatLoaded()) return;

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        Fluid fluid = DRINKING_FLUID.get();
        DRINKING_FLUID.remove();

        if (fluid == null) return;

        if (fluid == JSDFluids.WHITE_TEA.getSource()) {
            applyColdSweatEffect(player, "cold_sweat", "frigidness", 3600, 0);
        } else if (fluid == JSDFluids.RED_TEA.getSource()) {
            applyColdSweatEffect(player, "cold_sweat", "warmth", 3600, 0);
        }
    }

    private static void applyColdSweatEffect(Player player, String namespace, String path, int duration, int amplifier) {
        ResourceLocation effectId = ResourceLocation.fromNamespaceAndPath(namespace, path);
        var effectOptional = BuiltInRegistries.MOB_EFFECT.getHolder(effectId);
        if (effectOptional.isPresent()) {
            Holder<MobEffect> effect = effectOptional.get();
            player.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
        }
    }
}
