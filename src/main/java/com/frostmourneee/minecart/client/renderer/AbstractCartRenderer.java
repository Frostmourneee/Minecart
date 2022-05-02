package com.frostmourneee.minecart.client.renderer;

import com.frostmourneee.debugging_minecart.core.dmUtil;
import com.frostmourneee.minecart.common.entity.AbstractCart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractCartRenderer extends EntityRenderer<AbstractCart> {

    public AbstractCartRenderer(EntityRendererProvider.Context context) {
        super(context);

        shadowRadius = 0.7F;
    }

    @Override
    public void render(AbstractCart cart, float float1, float float2, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int int6) {
        super.render(cart, float1, float2, poseStack, buffer, int6);

        debugMode(cart, poseStack, buffer, int6); //TODO remove debug

        vertRotationAndAlphaAngle(cart, poseStack, float2);
        horRotation(cart, poseStack);
        hurtAnim(cart, poseStack, float2);
    }

    public void vertRotation(AbstractCart cart, PoseStack poseStack, float vertAngle) {
        if (cart.alpha.get(cart.alpha.size() - 1) == -90.0F) {
            if (vertAngle < -2.0F) poseStack.translate(0.0D, 0.15D, 0.23D);
            else if (vertAngle > 2.0F) poseStack.translate(0.0D,0.15D, -0.23D);

            poseStack.mulPose(Vector3f.XP.rotationDegrees(vertAngle));
        }
        else if (cart.alpha.get(cart.alpha.size() - 1) == 180.0F) {
            if (vertAngle < -2.0F) poseStack.translate(0.23D, 0.15D, 0.0D);
            else if (vertAngle > 2.0F) poseStack.translate(-0.23D, 0.15D, 0.0D);

            poseStack.mulPose(Vector3f.ZP.rotationDegrees(-vertAngle));
        }
    }
    public void horRotation(AbstractCart cart, PoseStack poseStack) {
        if (cart.alpha.size() == 3) cart.alpha.remove(0);
        findHorizontalAngle(cart);

        if (!cart.zeroDeltaHorizontal()) cart.setYRot(-cart.horAngle - 90.0F); //CLIENTSIDE ONLY
        poseStack.mulPose(Vector3f.YP.rotationDegrees(cart.horAngle)); //BASIS FOR COORDINATE SYSTEM
    }
    public void findHorizontalAngle(AbstractCart cart) {
        if (cart.isStopped()) { //STAYING
             switch (cart.getDirection()) {
                case EAST -> cart.horAngle = 0.0F;
                case NORTH -> cart.horAngle = 90.0F;
                case WEST -> cart.horAngle = 180.0F;
                case SOUTH -> cart.horAngle = 270.0F;
            }
        }
        else { //MOVING
            if (cart.alpha.get(cart.alpha.size() - 1) == -90.0F) { //NORTH-SOUTH MOVEMENT
                if (cart.delta.z < -1.0E-4) cart.horAngle = 90.0F;
                if (cart.delta.z > 1.0E-4) cart.horAngle = 270.0F;
            }
            else if (cart.alpha.get(cart.alpha.size() - 1) == 180.0F) { //WEST-EAST Movement
                if (cart.delta.x < -1.0E-4) cart.horAngle = 180.F;
                if (cart.delta.x > 1.0E-4) cart.horAngle = 0.0F;
            }
            else if (cart.alpha.size() == 2) { //Rotating movement
                float deltaAlpha = cart.alpha.get(1) - cart.alpha.get(0);
                if (Math.abs(deltaAlpha) < 30.0F) cart.horAngle -= deltaAlpha;
            }
        }
    }

    //============================================TECHNICAL METHODS====================================================

    public void vertRotationAndAlphaAngle(AbstractCart cart, PoseStack poseStack, float float2)  {
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
                cart.alpha.add((float)(Math.atan2(vec33.z, vec33.x) * 180.0D / Math.PI));
                cart.vertAngle = (float)(Math.atan(vec33.y) * 73.0D);
            }
        }

        vertRotation(cart, poseStack, cart.vertAngle);
    }

    public void debugMode (AbstractCart cart, PoseStack poseStack, MultiBufferSource buffer, int int6) {
        if (cart.debugMode)
            dmUtil.renderVisibleIndicator(Blocks.REDSTONE_BLOCK.defaultBlockState(), poseStack, buffer, int6);
    }

    public void hurtAnim(AbstractCart cart, PoseStack poseStack, float float2) {
        float f5 = (float) cart.getHurtTime() - float2;
        float f6 = cart.getDamage() - float2;
        if (f6 < 0.0F) {
            f6 = 0.0F;
        }

        if (f5 > 0.0F) {
            poseStack.mulPose(Vector3f.XP.rotationDegrees(Mth.sin(f5) * f5 * f6 / 10.0F * (float) cart.getHurtDir()));
        }
    }

    public abstract void buffering(AbstractCart cart, PoseStack poseStack, MultiBufferSource buffer, int int6);

    @Override
    public abstract ResourceLocation getTextureLocation(AbstractCart cart);
}