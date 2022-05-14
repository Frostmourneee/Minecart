package com.frostmourneee.minecart.client.renderer;

import com.frostmourneee.minecart.client.renderer.model.DebugWagonModel;
import com.frostmourneee.minecart.client.renderer.model.WagonEntityModel;
import com.frostmourneee.minecart.common.entity.AbstractCart;
import com.frostmourneee.minecart.minecart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class WagonEntityRenderer extends AbstractCartRenderer {

    public static ResourceLocation TEXTURE_RED = new ResourceLocation(minecart.MOD_ID, "textures/entities/wagon_red.png");
    public static ResourceLocation TEXTURE_GREEN = new ResourceLocation(minecart.MOD_ID, "textures/entities/wagon_green.png");
    public static ResourceLocation TEXTURE_DEBUG_RED = new ResourceLocation(minecart.MOD_ID, "textures/entities/debug_wagon_red.png"); //TODO remove debug
    public static ResourceLocation TEXTURE_DEBUG_GREEN = new ResourceLocation(minecart.MOD_ID, "textures/entities/debug_wagon_green.png"); //TODO remove debug

    private final WagonEntityModel model;
    private final DebugWagonModel debugModel; //TODO remove debug

    public WagonEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        model = new WagonEntityModel<>(context.bakeLayer(WagonEntityModel.LAYER_LOCATION));
        debugModel = new DebugWagonModel<>(context.bakeLayer(DebugWagonModel.LAYER_LOCATION)); //TODO remove debug
    }

    @Override
    public void render(@NotNull AbstractCart cart, float float1, float float2, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int int6) {
        super.render(cart, float1, float2, poseStack, buffer, int6);

        buffering(cart, poseStack, buffer, int6);
    }

    //============================================TECHNICAL METHODS====================================================

    public void buffering(AbstractCart cart, PoseStack poseStack, MultiBufferSource buffer, int int6) {
        if (cart.debugMode) {
            VertexConsumer vertexconsumer = buffer.getBuffer(debugModel.renderType(getTextureLocation(cart)));
            debugModel.renderToBuffer(poseStack, vertexconsumer, int6, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            VertexConsumer vertexconsumer = buffer.getBuffer(model.renderType(getTextureLocation(cart)));
            model.renderToBuffer(poseStack, vertexconsumer, int6, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } //TODO remove debug
        poseStack.popPose();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AbstractCart cart) {
        if (cart.debugMode) {
            return cart.hasFrontCart ? TEXTURE_DEBUG_GREEN : TEXTURE_DEBUG_RED;
        } else return cart.hasFrontCart ? TEXTURE_GREEN : TEXTURE_RED;
    }
}