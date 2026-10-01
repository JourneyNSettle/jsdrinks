package com.journey.jsdrinks.compat.jei;

import java.util.Arrays;
import java.util.List;
import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.recipe.TeaPileRecipe;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.compat.jei.category.BaseRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

public class TeaPileCategory extends AbstractRecipeCategory<RecipeHolder<TeaPileRecipe>> {
    public static final RecipeType<RecipeHolder<TeaPileRecipe>> TYPE = RecipeType.createRecipeHolderType(
        ResourceLocation.fromNamespaceAndPath(JourneyDrinks.MODID, "tea_pile")
    );

    private static final int WIDTH = 156;
    private static final int HEIGHT = 56;

    private final IDrawableStatic arrow;
    private final IDrawableAnimated arrowAnimated;

    public TeaPileCategory(IGuiHelper helper) {
        super(
            TYPE,
            Component.translatable("jsdrinks.jei.tea_pile"),
            helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, FoodCapability.setNonDecaying(new ItemStack(JSDItems.BRUISED_TEA_LEAF.get()))),
            WIDTH,
            HEIGHT
        );
        this.arrow = helper.createDrawable(BaseRecipeCategory.ICONS, 0, 14, 22, 16);
        IDrawableStatic arrowAnim = helper.createDrawable(BaseRecipeCategory.ICONS, 22, 14, 22, 16);
        this.arrowAnimated = helper.createAnimatedDrawable(arrowAnim, 80, IDrawableAnimated.StartDirection.LEFT, false);
    }

    private static int[] getOutputXPositions(int count) {
        return switch (count) {
            case 1 -> new int[]{104};
            case 2 -> new int[]{92, 120};
            case 3 -> new int[]{80, 104, 128};
            default -> new int[]{104};
        };
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<TeaPileRecipe> recipeHolder, IFocusGroup focuses) {
        TeaPileRecipe recipe = recipeHolder.value();

        List<ItemStack> baseStacks = Arrays.stream(recipe.getIngredient().getItems())
            .map(ItemStack::copy)
            .map(FoodCapability::setTransientNonDecaying)
            .toList();

        List<ItemStack> displayStacks = new java.util.ArrayList<>();
        if (recipe.requiresLargeLeaf()) {
            for (ItemStack base : baseStacks) {
                ItemStack withLarge = base.copy();
                withLarge.set(JSDDataComponents.LARGE_LEAF.get(), true);
                displayStacks.add(withLarge);
            }
        }
        displayStacks.addAll(baseStacks);

        IRecipeSlotBuilder inputSlot = builder.addSlot(RecipeIngredientRole.INPUT, 7, 7)
            .addItemStacks(displayStacks)
            .setStandardSlotBackground();

        if (recipe.requiresLargeLeaf()) {
            inputSlot.addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(Component.translatable("tooltip.jsdrinks.large_leaf").withStyle(ChatFormatting.DARK_GREEN));
                tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.requires_large_leaf").withStyle(ChatFormatting.GOLD));
            });
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 7, 31)
            .addItemStacks(recipe.getDisplaySurfaceBlocks())
            .setStandardSlotBackground()
            .addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(recipe.getSurfaceDescription());
            });

        int[] xPositions = getOutputXPositions(recipe.getOutputs().size());
        for (int i = 0; i < recipe.getOutputs().size(); i++) {
            TeaPileRecipe.ChanceOutput out = recipe.getOutputs().get(i);
            ItemStack displayStack = out.result().getEmptyStack().copy();
            if (out.rotten()) {
                FoodCapability.setRotten(displayStack);
            } else {
                FoodCapability.setTransientNonDecaying(displayStack);
            }
            builder.addSlot(RecipeIngredientRole.OUTPUT, xPositions[i], 19)
                .addItemStack(displayStack)
                .setStandardSlotBackground()
                .addRichTooltipCallback((slotView, tooltip) -> {
                    int pct = (int) (out.chance() * 100);
                    if (out.rotten()) {
                        tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.chance_spoilage", pct).withStyle(ChatFormatting.RED));
                    } else {
                        tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.chance", pct).withStyle(ChatFormatting.GREEN));
                    }
                });
        }
    }

    @Override
    public void draw(RecipeHolder<TeaPileRecipe> recipeHolder, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        TeaPileRecipe recipe = recipeHolder.value();
        Font font = Minecraft.getInstance().font;

        // Draw duration text above arrow
        int timeWidth = font.width(recipe.getDurationText());
        guiGraphics.drawString(font, recipe.getDurationText(), 51 - timeWidth / 2, 7, 0xFF404040, false);

        // Draw arrow
        arrow.draw(guiGraphics, 40, 20);
        arrowAnimated.draw(guiGraphics, 40, 20);

        // Draw weather text below arrow
        Component weather = Component.translatable("jsdrinks.jei.tea_pile.weather");
        int weatherWidth = font.width(weather);
        guiGraphics.drawString(font, weather, 51 - weatherWidth / 2, 40, 0xFF666666, false);

        // Draw percentage under each output slot
        int[] xPositions = getOutputXPositions(recipe.getOutputs().size());
        for (int i = 0; i < recipe.getOutputs().size(); i++) {
            TeaPileRecipe.ChanceOutput out = recipe.getOutputs().get(i);
            String percent = (int) (out.chance() * 100) + "%";
            int pWidth = font.width(percent);
            int pColor = out.rotten() ? 0xFFA04040 : (recipe.getOutputs().size() > 1 && i == 0 && recipe.getOutputs().size() == 3 ? 0xFF206020 : 0xFF404040);
            guiGraphics.drawString(font, percent, xPositions[i] + 9 - pWidth / 2, 41, pColor, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<TeaPileRecipe> recipeHolder, IRecipeSlotsView recipeSlots, double mouseX, double mouseY) {
        // Precise hitbox around the arrow and weather icon
        if (mouseX >= 38 && mouseX <= 64 && mouseY >= 18 && mouseY <= 38) {
            tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.weather_info").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.weather_rain_stop").withStyle(ChatFormatting.GRAY));
        }
    }
}
