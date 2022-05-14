package com.frostmourneee.minecart.client.renderer;

import com.frostmourneee.minecart.client.renderer.model.DebugWagonModel;
import com.frostmourneee.minecart.client.renderer.model.LocomotiveEntityModel;
import com.frostmourneee.minecart.common.entity.AbstractCart;
import com.frostmourneee.minecart.common.entity.LocomotiveEntity;
import com.frostmourneee.minecart.minecart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class LocomotiveEntityRenderer extends AbstractCartRenderer {

    public static ResourceLocation TEXTURE = new ResourceLocation(minecart.MOD_ID, "textures/entities/locomotive.png");
    public static ResourceLocation TEXTURE_DEBUG_GREEN = new ResourceLocation(minecart.MOD_ID, "textures/entities/debug_wagon_green.png"); //TODO remove debug
    public static ResourceLocation TEXTURE_DEBUG_RED = new ResourceLocation(minecart.MOD_ID, "textures/entities/debug_wagon_red.png"); //TODO remove debug

    private final LocomotiveEntityModel model;
    private final DebugWagonModel debugModel; //TODO remove debug

    public LocomotiveEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        model = new LocomotiveEntityModel<>(context.bakeLayer(LocomotiveEntityModel.LAYER_LOCATION));
        debugModel = new DebugWagonModel(context.bakeLayer(DebugWagonModel.LAYER_LOCATION)); //TODO remove debug
    }

    @Override
    public void render(@NotNull AbstractCart cart, float float1, float float2, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int int6) {
        super.render(cart, float1, float2, poseStack, buffer, int6);

        furnace(cart, poseStack, buffer, int6);
        buffering(cart, poseStack, buffer, int6);
    }

    //======================================TECHNICAL METHODS==========================================================

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
        if (cart.debugMode) return cart.hasBackCart ? TEXTURE_DEBUG_GREEN : TEXTURE_DEBUG_RED;
        else return TEXTURE;
    }

    public void furnace(AbstractCart cart, PoseStack poseStack, MultiBufferSource buffer, int int6) {
        int j = cart.getDisplayOffset();
        BlockState blockstate = cart.getDisplayBlockState();
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE && !cart.debugMode) {
            poseStack.pushPose();
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(-0.5D, (double)((float)(j - 8) / 16.0F) + 1.0D / 8.0D, -0.5D);
            poseStack.translate(0.0D, -0.125D, 0.0D);
            renderFurnace(Blocks.FURNACE.defaultBlockState()
                    .setValue(FurnaceBlock.FACING, Direction.EAST)
                    .setValue(FurnaceBlock.LIT, ((LocomotiveEntity)cart).hasFuel()), poseStack, buffer, int6);
            poseStack.popPose();
        }
    }

    public void renderFurnace(BlockState blockState, PoseStack poseStack, MultiBufferSource buffer, int int4) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, poseStack, buffer, int4, OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
    }
}