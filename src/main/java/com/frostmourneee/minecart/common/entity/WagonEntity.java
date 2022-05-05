package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.debugging_minecart.core.init.dmItemInit;
import com.frostmourneee.minecart.core.init.ccItemInit;
import com.frostmourneee.minecart.core.init.ccSoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class WagonEntity extends AbstractCart {

    public WagonEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();

        //===================== MY CODE STARTS ======================

    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult ret = super.interact(player, hand);
        if (ret.consumesAction()) return ret;
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.getItem().equals(dmItemInit.DebugItem.get())) {
            debugMode = !debugMode; //TODO remove debug
            entityData.set(DATA_DEBUG_MODE, debugMode);
        }

        if (canBeClamped(itemStack)) {
            if (hasFrontCart) {
                cartSound(5.5F, ccSoundInit.CART_UNCLAMP.get());
                frontCart.resetBack();
                resetFront();
            } else {
                tryingToClamp();
            }
        }

        if (player.isSecondaryUseActive() && !itemStack.getItem().equals(ccItemInit.CLAMP.get()) && !itemStack.getItem().equals(dmItemInit.DebugItem.get())) {
            return InteractionResult.PASS;
        } else if (isVehicle()) {
            return InteractionResult.PASS;
        } else if (!level.isClientSide && !itemStack.getItem().equals(ccItemInit.CLAMP.get()) && !itemStack.getItem().equals(dmItemInit.DebugItem.get())) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    public void collisionProcessing() {
        super.collisionProcessing();

        spawnAfterCartLeaving();
    }

    //////////////////////////////////////TECHNICAL METHODS//////////////////////////
    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.3f;
    } //TODO change
    @Override
    public void moveMinecartOnRail(BlockPos pos) {
        AbstractMinecart mc = this;
        double d24 = mc.isVehicle() ? 0.75D : 1.0D;
        double d25 = mc.getMaxSpeedWithRail();
        Vec3 vec3d1 = mc.getDeltaMovement();

        mc.move(MoverType.SELF, new Vec3(Mth.clamp(d24 * vec3d1.x, -d25, d25), 0.0D, Mth.clamp(d24 * vec3d1.z, -d25, d25)));
    }
    @Override
    public void setDeltaMovement(@NotNull Vec3 vec) {
        if (hasFrontCart) {
            double dist = frontCart.position().subtract(position()).length();

            if (frontCart.zeroDelta()) deltaMovement = Vec3.ZERO;
            else if (dist >= 1.625D)
                deltaMovement = frontCart.deltaMovement;
        } else deltaMovement = vec;

        if (deltaMovement.length() < 1.0E-4) deltaMovement = Vec3.ZERO;
    }

    @Override
    public boolean canBeCollidedWith() {
        return (hasBackCart || hasFrontCart) && isAlive();
    }

    public void spawnAfterCartLeaving() {
        if (hasFrontCart && getPassengers().isEmpty()) {
            AABB miniBox;
            miniBox = getBoundingBox().deflate(0.1D, 0.0F, 0.1D);

            ArrayList<Player> previousPassenger = (ArrayList<Player>) level.getEntitiesOfClass(Player.class, miniBox);
            if (!previousPassenger.isEmpty()) previousPassenger.get(0).setPos(previousPassenger.get(0).position().add(0.0D, 1.0D, 0.0D));
        }
    }

    public boolean canBeClamped(ItemStack itemStack) {
        BlockPos blockPos = new BlockPos(position());
        BlockState blockState = level.getBlockState(blockPos);

        return itemStack.getItem().equals(ccItemInit.CLAMP.get())
                && isRail(blockState)
                && !anyRailShape(blockState, blockPos).isAscending();
    }

    public void activateMinecart(int int1, int int2, int int3, boolean bool1) {
        if (bool1) {
            if (isVehicle()) {
                ejectPassengers();
            }

            if (getHurtTime() == 0) {
                setHurtDir(-getHurtDir());
                setHurtTime(10);
                setDamage(50.0F);
                markHurt();
            }
        }
    }

    public AbstractMinecart.@NotNull Type getMinecartType() {
        return AbstractMinecart.Type.RIDEABLE;
    }
    @Override
    public AbstractCart.Type getCartType() {
        return Type.WAGON;
    }
}