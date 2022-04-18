package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.debugging_minecart.core.init.dmItemInit;
import com.frostmourneee.minecart.core.ccUtil;
import com.frostmourneee.minecart.core.init.ccItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

        if (hasFrontCart) {
            clampProcessing();
        }
    }

    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand interactionHand) {
        InteractionResult ret = super.interact(player, interactionHand);
        if (ret.consumesAction()) return ret;
        ItemStack itemStack = player.getItemInHand(interactionHand);

        if (player.isSecondaryUseActive() && itemStack.getItem().equals(dmItemInit.DebugItem.get())) {
            if (debugMode) {
                debugMode = false;
                entityData.set(DATA_DEBUG_MODE, false);
            }
            else {
                debugMode = true;
                entityData.set(DATA_DEBUG_MODE, true);
            }
        } //TODO remove debug

        if (!level.isClientSide) {
            if (canBeClamped(player, itemStack)) {
                if (hasFrontCart) {
                    frontCart.resetBack();
                    resetFront();
                } else {
                    tryingToClamp();
                }
            }
        }

        if (player.isSecondaryUseActive() && !itemStack.getItem().equals(ccItemInit.CLAMP.get()) && !itemStack.getItem().equals(dmItemInit.DebugItem.get())) {
            return InteractionResult.PASS;
        } else if (isVehicle()) {
            return InteractionResult.PASS;
        } else if (!level.isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    public void collisionProcessing() {
        super.collisionProcessing();

        spawnAfterCartLeaving();
    }

    public void clampProcessing() {

        if (hasFrontCart) {
            poseConfiguration(Direction.NORTH, frontCart, 0, 1);
            poseConfiguration(Direction.SOUTH, frontCart, 0, -1);
            poseConfiguration(Direction.EAST, frontCart, -1, 0);
            poseConfiguration(Direction.WEST, frontCart, 1, 0);
        }
    }

    //////////////////////////////////////TECHNICAL METHODS//////////////////////////

    public void poseConfiguration(Direction direction, AbstractCart cart, int i1, int i3) {
        if (getDirection().equals(direction)) {
            if (getDirection().equals(cart.getDirection()) && ccUtil.bothUpOrDownOrForward(this, cart)) {
                if (ccUtil.goesFlat(this) && ccUtil.goesFlat(cart) && !ccUtil.isRotating(this)) {
                    if (getLocomotive() != null && !ccUtil.isStopped(getLocomotive())) setDeltaMovement(cart.deltaMovement);
                    if (getLocomotive() == null && !ccUtil.isStopped(frontCart)) setDeltaMovement(cart.deltaMovement);
                    setPos(cart.position().add(i1 * 1.625D, 0.0D, i3 * 1.625D));
                }
                else if (ccUtil.goesUpper(cart)) {
                    setPos(cart.position().add(i1 * 1.149D, -1.149D, i3 * 1.149D));
                }
                else if (!ccUtil.goesUpper(cart)) {
                    setPos(cart.position().add(i1 * 1.149D, 1.149D, i3 * 1.149D));
                }
            }
            else {
                if (cart.deltaMovement.length() > 1.0D) {
                    if (getLocomotive() != null && !ccUtil.isStopped(getLocomotive())) setDeltaMovement(-i1, 0.0D, -i3);
                    if (getLocomotive() == null && !ccUtil.isStopped(frontCart)) setDeltaMovement(-i1, 0.0D, -i3);
                }
                else {
                    if (getLocomotive() != null && !ccUtil.isStopped(getLocomotive())) setDeltaMovement(-i1, 0.0D, -i3 * cart.deltaMovement.length());
                    if (getLocomotive() == null && !ccUtil.isStopped(frontCart)) setDeltaMovement(-i1, 0.0D, -i3 * cart.deltaMovement.length());
                }
            }
        }
    }
    @Override
    public void setDeltaMovement(@NotNull Vec3 vec) {
        if (Math.abs(deltaMovement.horizontalDistance()) < 1.0E-2) {
            if (hasFrontCart && Math.abs(vec.horizontalDistance()) < 51.0E-3) deltaMovement = Vec3.ZERO;
            else deltaMovement = vec;
        } else {
            if (hasFrontCart && Math.abs(vec.horizontalDistance()) < 2.0E-2) deltaMovement = Vec3.ZERO;
            else deltaMovement = vec;
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return entityData.get(DATA_BACKCART_EXISTS) || entityData.get(DATA_FRONTCART_EXISTS) && isAlive();
    }

    public void spawnAfterCartLeaving() {
        if (hasFrontCart && getPassengers().isEmpty()) {
            AABB miniBox;
            miniBox = getBoundingBox().deflate(0.1D, 0.0F, 0.1D);

            ArrayList<Player> previousPassenger = (ArrayList<Player>) level.getEntitiesOfClass(Player.class, miniBox);
            if (!previousPassenger.isEmpty()) previousPassenger.get(0).setPos(previousPassenger.get(0).position().add(0.0D, 1.0D, 0.0D));
        }
    }

    public boolean canBeClamped(Player player, ItemStack itemStack) {
        if (player.isSecondaryUseActive()) { //WITH SHIFT PRESSED
            BlockPos blockPos = new BlockPos(getX(), getY(), getZ());
            BlockState blockState = level.getBlockState(blockPos);

            return itemStack.getItem().equals(ccItemInit.CLAMP.get())
                    && ccUtil.isRail(blockState)
                    && !ccUtil.anyRailShape(blockState, blockPos, this).isAscending()
                    && ccUtil.zeroDeltaMovement(this);
        }
        else return false;
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

    public AbstractCart.Type getCartType() {
        return Type.WAGON;
    }
}