package com.frostmourneee.minecart.client.renderer;

import com.frostmourneee.debugging_minecart.core.dmUtil;
import com.frostmourneee.minecart.client.renderer.model.DebugWagonModel;
import com.frostmourneee.minecart.client.renderer.model.WagonEntityModel;
import com.frostmourneee.minecart.common.entity.WagonEntity;
import com.frostmourneee.minecart.minecart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

import static com.frostmourneee.minecart.common.entity.WagonEntity.DATA_HORIZONTAL_ROTATION_ANGLE;
import static com.frostmourneee.minecart.common.entity.WagonEntity.DATA_VERTICAL_ROTATION_ANGLE;

public class WagonEntityRenderer extends EntityRenderer<WagonEntity> {

    public static ResourceLocation TEXTURE_RED = new ResourceLocation(minecart.MOD_ID, "textures/entities/wagon_red.png");
    public static ResourceLocation TEXTURE_GREEN = new ResourceLocation(minecart.MOD_ID, "textures/entities/wagon_green.png");
    public static ResourceLocation TEXTURE_DEBUG_RED = new ResourceLocation(minecart.MOD_ID, "textures/entities/debug_wagon_red.png"); //TODO remove debug
    public static ResourceLocation TEXTURE_DEBUG_GREEN = new ResourceLocation(minecart.MOD_ID, "textures/entities/debug_wagon_green.png"); //TODO remove debug

    private final WagonEntityModel model;
    private final DebugWagonModel debugModel; //TODO remove debug

    public ArrayList<Float> alpha = new ArrayList<>();
    public float straightAngle;

    public WagonEntityRenderer(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0.7F;
        this.model = new WagonEntityModel<>(context.bakeLayer(WagonEntityModel.LAYER_LOCATION));
        this.debugModel = new DebugWagonModel<>(context.bakeLayer(DebugWagonModel.LAYER_LOCATION)); //TODO remove debug
    }

    @Override
    public void render(WagonEntity wagonEntity, float float1, float float2, PoseStack poseStack, MultiBufferSource buffer, int int6) {
        super.render(wagonEntity, float1, float2, poseStack, buffer, int6);

        this.debugMode(wagonEntity, poseStack, buffer, int6); //TODO remove debug

        this.vertRotationAndAlphaAngle(wagonEntity, poseStack, float2);
        this.horRotation(wagonEntity, poseStack);
        this.hurtAnim(wagonEntity, poseStack, float2);
        this.buffering(wagonEntity, poseStack, buffer, int6);
    }

    public void findHorizontalAngle(WagonEntity cart) {
        if (cart.deltaMovement.length() == 0) { //STAYING
            switch (cart.getDirection()) {
                case EAST -> this.straightAngle = 0.0F;
                case NORTH -> this.straightAngle = 90.0F;
                case WEST -> this.straightAngle = 180.0F;
                case SOUTH -> this.straightAngle = 270.0F;
            }
            cart.horAngle = this.straightAngle;
        }
        else { //MOVING
            if (this.alpha.get(this.alpha.size() - 1) == -90.0F) { //NORTH-SOUTH MOVEMENT
                if (cart.deltaMovement.z < 0) this.straightAngle = 90.0F;
                if (cart.deltaMovement.z > 0) this.straightAngle = 270.0F;

                cart.horAngle = this.straightAngle;
            }
            else if (this.alpha.get(this.alpha.size() - 1) == 180.0F) { //WEST-EAST Movement
                if (cart.deltaMovement.x < 0) this.straightAngle = 180.F;
                if (cart.deltaMovement.x > 0) this.straightAngle = 0.0F;

                cart.horAngle = this.straightAngle;
            }
            else if (this.alpha.size() == 2) { //Rotating movement
                float deltaAlpha = this.alpha.get(1) - this.alpha.get(0);
                if (Math.abs(deltaAlpha) < 30.0F) cart.horAngle -= deltaAlpha;
            }
        }
    }

    public void vertRotation(PoseStack poseStack, float f3) {
        if (this.alpha.get(this.alpha.size() - 1) == -90.0F) {
            if (f3 < -2.0F) poseStack.translate(0.0D, 0.15D, 0.23D);
            else if (f3 > 2.0F) poseStack.translate(0.0D,0.15D, -0.23D);

            poseStack.mulPose(Vector3f.XP.rotationDegrees(f3));
        }
        else if (this.alpha.get(this.alpha.size() - 1) == 180.0F) {
            if (f3 < -2.0F) poseStack.translate(0.23D, 0.15D, 0.0D);
            else if (f3 > 2.0F) poseStack.translate(-0.23D, 0.15D, 0.0D);

            poseStack.mulPose(Vector3f.ZP.rotationDegrees(-f3));
        }
    }

    //============================================TECHNICAL METHODS====================================================

    public void debugMode (WagonEntity cart, PoseStack poseStack, MultiBufferSource buffer, int int6) {
        if (cart.getEntityData().get(WagonEntity.DATA_DEBUG_MODE))
            dmUtil.renderVisibleIndicator(Blocks.REDSTONE_BLOCK.defaultBlockState(), poseStack, buffer, int6);
    }

    public void vertRotationAndAlphaAngle(WagonEntity cart, PoseStack poseStack, float float2)  {
        poseStack.pushPose();
        long i = (long)cart.getId() * 493286711L;
        i = i * i * 4392167121L + i * 98761L;
        float f = (((float)(i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f1 = (((float)(i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        float f2 = (((float)(i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
        poseStack.translate(f, f1, f2);
        double d0 = Mth.lerp(float2, cart.xOld, cart.getX());
        double d1 = Mth.lerp(float2, cart.yOld, cart.getY());
        double d2 = Mth.lerp(float2, cart.zOld, cart.getZ());
        Vec3 vec3 = cart.getPos(d0, d1, d2);
        cart.vertAngle = Mth.lerp(float2, cart.xRotO, cart.getXRot());
        if (vec3 != null) {
            Vec3 vec31 = cart.getPosOffs(d0, d1, d2, 0.3F);
            Vec3 vec32 = cart.getPosOffs(d0, d1, d2, -0.3F);
            if (vec31 == null) {
                vec31 = vec3;
            }

            if (vec32 == null) {
                vec32 = vec3;
            }

            poseStack.translate(vec3.x - d0, (vec31.y + vec32.y) / 2.0D - d1, vec3.z - d2);
            Vec3 vec33 = vec32.add(-vec31.x, -vec31.y, -vec31.z);
            if (vec33.length() != 0.0D) {
                vec33 = vec33.normalize();
                this.alpha.add((float)(Math.atan2(vec33.z, vec33.x) * 180.0D / Math.PI));
                cart.vertAngle = (float)(Math.atan(vec33.y) * 73.0D);
            }
        }

        this.vertRotation(poseStack, cart.vertAngle);
        cart.getEntityData().set(DATA_VERTICAL_ROTATION_ANGLE, cart.vertAngle);
    }

    public void horRotation(WagonEntity cart, PoseStack poseStack) {
        if (this.alpha.size() == 3) this.alpha.remove(0);
        this.findHorizontalAngle(cart);

        if (cart.deltaMovement.length() != 0.0F) cart.setYRot(-cart.horAngle - 90.0F);
        poseStack.mulPose(Vector3f.YP.rotationDegrees(cart.horAngle)); //BASIS FOR COORDINATE SYSTEM
        cart.getEntityData().set(DATA_HORIZONTAL_ROTATION_ANGLE, cart.horAngle);
    }

    public void hurtAnim(WagonEntity cart, PoseStack poseStack, float float2) {
        float f5 = (float) cart.getHurtTime() - float2;
        float f6 = cart.getDamage() - float2;
        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            poseStack.mulPose(Vector3f.XP.rotationDegrees(Mth.sin(f5) * f5 * f6 / 10.0F * (float) cart.getHurtDir()));
        }
    }

    public void buffering(WagonEntity cart, PoseStack poseStack, MultiBufferSource buffer, int int6) {
        if (cart.getEntityData().get(WagonEntity.DATA_DEBUG_MODE)) {
            VertexConsumer vertexconsumer = buffer.getBuffer(this.debugModel.renderType(this.getTextureLocation(cart)));
            this.debugModel.renderToBuffer(poseStack, vertexconsumer, int6, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            VertexConsumer vertexconsumer = buffer.getBuffer(this.model.renderType(this.getTextureLocation(cart)));
            this.model.renderToBuffer(poseStack, vertexconsumer, int6, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        } //TODO remove debug
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(WagonEntity wagonEntity) {
        if (wagonEntity.getEntityData().get(WagonEntity.DATA_DEBUG_MODE)) {
            return wagonEntity.getEntityData().get(WagonEntity.DATA_FRONTCART_EXISTS) ? TEXTURE_DEBUG_GREEN : TEXTURE_DEBUG_RED;
        } else return wagonEntity.getEntityData().get(WagonEntity.DATA_FRONTCART_EXISTS) ? TEXTURE_GREEN : TEXTURE_RED;
    }
}