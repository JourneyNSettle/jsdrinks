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

    private static final int WIDTH = 144;
    private static final int HEIGHT = 52;

    private final IDrawableStatic slot;
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
        this.slot = helper.getSlotDrawable();
        this.arrow = helper.createDrawable(BaseRecipeCategory.ICONS, 0, 14, 22, 16);
        IDrawableStatic arrowAnim = helper.createDrawable(BaseRecipeCategory.ICONS, 22, 14, 22, 16);
        this.arrowAnimated = helper.createAnimatedDrawable(arrowAnim, 80, IDrawableAnimated.StartDirection.LEFT, false);
    }

    private static int[] getOutputXPositions(int count) {
        return switch (count) {
            case 1 -> new int[]{92};
            case 2 -> new int[]{78, 106};
            case 3 -> new int[]{68, 92, 116};
            default -> new int[]{92};
        };
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TeaPileRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotBuilder inputSlot = builder.addSlot(RecipeIngredientRole.INPUT, 6, 6)
            .addItemStack(recipe.input())
            .setBackground(slot, -1, -1);

        if (recipe.requiresLargeLeaf()) {
            inputSlot.addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.requires_large_leaf").withStyle(ChatFormatting.GOLD));
            });
        }

        builder.addSlot(RecipeIngredientRole.CATALYST, 6, 28)
            .addItemStacks(recipe.surfaceBlocks())
            .setBackground(slot, -1, -1)
            .addRichTooltipCallback((slotView, tooltip) -> {
                tooltip.add(recipe.surfaceDescription());
            });

        int[] xPositions = getOutputXPositions(recipe.outputs().size());
        for (int i = 0; i < recipe.outputs().size(); i++) {
            TeaPileRecipe.ChanceOutput out = recipe.outputs().get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, xPositions[i], 17)
                .addItemStack(out.stack())
                .setBackground(slot, -1, -1)
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
        guiGraphics.drawString(font, recipe.durationText(), 47 - timeWidth / 2, 6, 0xFF404040, false);

        // Draw arrow
        arrow.draw(guiGraphics, 36, 18);
        arrowAnimated.draw(guiGraphics, 36, 18);

        // Draw weather text below arrow
        Component weather = Component.translatable("jsdrinks.jei.tea_pile.weather");
        int weatherWidth = font.width(weather);
        guiGraphics.drawString(font, weather, 47 - weatherWidth / 2, 38, 0xFF666666, false);

        // Draw percentage under each output slot
        int[] xPositions = getOutputXPositions(recipe.outputs().size());
        for (int i = 0; i < recipe.outputs().size(); i++) {
            TeaPileRecipe.ChanceOutput out = recipe.outputs().get(i);
            String percent = (int) (out.chance() * 100) + "%";
            int pWidth = font.width(percent);
            int pColor = out.isSpoilage() ? 0xFFA04040 : (recipe.outputs().size() > 1 && i == 0 && recipe.outputs().size() == 3 ? 0xFF206020 : 0xFF404040);
            guiGraphics.drawString(font, percent, xPositions[i] + 9 - pWidth / 2, 38, pColor, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, TeaPileRecipe recipe, IRecipeSlotsView recipeSlots, double mouseX, double mouseY) {
        if (mouseX >= 34 && mouseX <= 60 && mouseY >= 16 && mouseY <= 48) {
            tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.weather_info").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.translatable("jsdrinks.jei.tea_pile.weather_rain_stop").withStyle(ChatFormatting.GRAY));
        }
    }
}
