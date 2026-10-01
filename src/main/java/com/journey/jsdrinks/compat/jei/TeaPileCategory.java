package com.journey.jsdrinks.compat.jei;

import java.util.List;
import com.journey.jsdrinks.JourneyDrinks;
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
import net.dries007.tfc.compat.jei.category.BaseRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class TeaPileCategory extends AbstractRecipeCategory<TeaPileRecipe> {
    public static final RecipeType<TeaPileRecipe> TYPE = RecipeType.create(JourneyDrinks.MODID, "tea_pile", TeaPileRecipe.class);

    private static final int WIDTH = 156;
    private static final int HEIGHT = 56;

    private final IDrawableStatic arrow;
    private final IDrawableAnimated arrowAnimated;

    public TeaPileCategory(IGuiHelper helper) {
        super(
            TYPE,
            Component.translatable("jsdrinks.jei.tea_pile"),
            helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(JSDItems.BRUISED_TEA_LEAF.get())),
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
    public void setRecipe(IRecipeLayoutBuilder builder, TeaPileRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder inputSlot = builder.addSlot(RecipeIngredientRole.INPUT, 7, 7)
            .addItemStack(recipe.input())
            .setStandardSlotBackground();

        if (recipe.requiresLargeLeaf()) {
            inputSlot.addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.requires_large_leaf").withStyle(ChatFormatting.GOLD));
            });
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 7, 31)
            .addItemStacks(recipe.surfaceBlocks())
            .setStandardSlotBackground()
            .addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(recipe.surfaceDescription());
            });

        int[] xPositions = getOutputXPositions(recipe.outputs().size());
        for (int i = 0; i < recipe.outputs().size(); i++) {
            TeaPileRecipe.ChanceOutput out = recipe.outputs().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, xPositions[i], 19)
                .addItemStack(out.stack())
                .setStandardSlotBackground()
                .addRichTooltipCallback((slotView, tooltip) -> {
                    int pct = (int) (out.chance() * 100);
                    if (out.isSpoilage()) {
                        tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.chance_spoilage", pct).withStyle(ChatFormatting.RED));
                    } else {
                        tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.chance", pct).withStyle(ChatFormatting.GREEN));
                    }
                });
        }
    }

    @Override
    public void draw(TeaPileRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        // Draw duration text above arrow
        int timeWidth = font.width(recipe.durationText());
        guiGraphics.drawString(font, recipe.durationText(), 51 - timeWidth / 2, 7, 0xFF404040, false);

        // Draw arrow
        arrow.draw(guiGraphics, 40, 20);
        arrowAnimated.draw(guiGraphics, 40, 20);

        // Draw weather text below arrow
        Component weather = Component.translatable("jsdrinks.jei.tea_pile.weather");
        int weatherWidth = font.width(weather);
        guiGraphics.drawString(font, weather, 51 - weatherWidth / 2, 40, 0xFF666666, false);

        // Draw percentage under each output slot
        int[] xPositions = getOutputXPositions(recipe.outputs().size());
        for (int i = 0; i < recipe.outputs().size(); i++) {
            TeaPileRecipe.ChanceOutput out = recipe.outputs().get(i);
            String percent = (int) (out.chance() * 100) + "%";
            int pWidth = font.width(percent);
            int pColor = out.isSpoilage() ? 0xFFA04040 : (recipe.outputs().size() > 1 && i == 0 && recipe.outputs().size() == 3 ? 0xFF206020 : 0xFF404040);
            guiGraphics.drawString(font, percent, xPositions[i] + 9 - pWidth / 2, 41, pColor, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, TeaPileRecipe recipe, IRecipeSlotsView recipeSlots, double mouseX, double mouseY) {
        // Precise hitbox around the arrow and weather icon
        if (mouseX >= 38 && mouseX <= 64 && mouseY >= 18 && mouseY <= 38) {
            tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.weather_info").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.weather_rain_stop").withStyle(ChatFormatting.GRAY));
        }
    }
}
