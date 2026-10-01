package com.journey.jsdrinks.compat.jei;

import java.util.List;
import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.registry.JSDItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.compat.jei.JEIIntegration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class JSDJEIPlugin implements IModPlugin {
    public static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(JourneyDrinks.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new TeaPileCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(JSDItems.BRUISED_TEA_LEAF.get()), TeaPileCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get()), TeaPileCategory.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(TeaPileCategory.TYPE, TeaPileRecipe.getRecipes());

        registration.addItemStackInfo(
            List.of(
                new ItemStack(JSDItems.BRUISED_TEA_LEAF.get()),
                new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get())
            ),
            Component.translatable("jsdrinks.jei.info.bruised_tea_leaf")
        );
        registration.addItemStackInfo(
            List.of(
                new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get()),
                new ItemStack(JSDItems.RED_TEA_LEAF.get()),
                new ItemStack(JSDItems.PUERH_TEA.get()),
                new ItemStack(JSDItems.STRONG_PUERH_TEA.get())
            ),
            Component.translatable("jsdrinks.jei.info.fermented_tea_leaf")
        );
        registration.addItemStackInfo(new ItemStack(JSDItems.YELLOW_TEA_LEAF.get()), Component.translatable("jsdrinks.jei.info.yellow_tea_leaf"));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        IRecipeManager recipeManager = jeiRuntime.getRecipeManager();
        RecipeType<RecipeHolder<HeatingRecipe>> heatingType = JEIIntegration.HEATING;
        if (heatingType != null) {
            List<RecipeHolder<HeatingRecipe>> toHide = recipeManager.createRecipeLookup(heatingType)
                .get()
                .filter(holder -> {
                    ResourceLocation id = holder.id();
                    return id.getNamespace().equals(JourneyDrinks.MODID) &&
                        (id.getPath().contains("burn_red_tea_leaf") || id.getPath().contains("burn_coffee_bean"));
                })
                .toList();

            if (!toHide.isEmpty()) {
                recipeManager.hideRecipes(heatingType, toHide);
            }
        }
    }
}
