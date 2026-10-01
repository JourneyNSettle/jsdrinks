package com.journey.jsdrinks.client.render;

import com.eerussianguy.firmalife.common.blockentities.StompingBarrelBlockEntity;
import com.eerussianguy.firmalife.common.recipes.StompingRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import net.dries007.tfc.client.RenderHelpers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

public class JSDStompingBarrelRenderer implements BlockEntityRenderer<StompingBarrelBlockEntity> {

    @Override
    public void render(StompingBarrelBlockEntity barrel, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if (barrel.getLevel() == null) {
            return;
        }

        final ItemStack stack = barrel.readStack();
        if (stack.isEmpty()) {
            return;
        }

        ResourceLocation texture = barrel.getTexture();
        if (texture == null) {
            final StompingRecipe recipe = StompingRecipe.getRecipe(stack);
            if (recipe != null) {
                texture = barrel.isOutputMode() ? recipe.getOutputTexture() : recipe.getInputTexture();
            }
        }
        if (texture == null) {
            return;
        }

        poseStack.pushPose();

        final boolean smashed = barrel.isOutputMode();
        // Barrel base floor inside is at y = 1/16 (0.0625). Total fill height is 7/16.
        // We scale only the contents, ensuring the rendered surface is always strictly above the floor base (>= 1.02f / 16f).
        float contentHeight = (7f / 16f) * ((float) stack.getCount() / StompingBarrelBlockEntity.MAX_GRAPES);
        float progress = smashed ? 0.5f : Mth.lerp(barrel.getStomps() / 16f, 1f, 0.5f);
        float y = (1.02f / 16f) + (contentHeight * progress);

        RenderHelpers.renderTexturedFace(poseStack, buffer, 0xffffff, 2f / 16f, 2f / 16f, 14f / 16f, 14f / 16f, y, overlay, light, texture);

        poseStack.popPose();
    }
}
