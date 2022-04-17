package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.debugging_minecart.core.init.dmItemInit;
import com.frostmourneee.minecart.common.item.WagonItem;
import com.frostmourneee.minecart.core.ccUtil;
import com.frostmourneee.minecart.core.init.ccEntityInit;
import com.frostmourneee.minecart.core.init.ccItemInit;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class WagonEntity extends AbstractCart {

    public WagonEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();

        if (frontConnection.isConnected()) {
            clampProcessing();
        }
    }

    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand interactionHand) {
        InteractionResult ret = super.interact(player, interactionHand);
        if (ret.consumesAction()) return ret;
        ItemStack itemStack = player.getItemInHand(interactionHand);

        if (player.isSecondaryUseActive() && itemStack.getItem().equals(dmItemInit.DebugItem.get())) {
            if (this.debugMode) {
                this.debugMode = false;
                this.entityData.set(DATA_DEBUG_MODE, false);
            }
            else {
                this.debugMode = true;
                this.entityData.set(DATA_DEBUG_MODE, true);
            }
        } //TODO remove debug

        if (!level.isClientSide) {
            if (this.canBeClamped(player, itemStack)) {
                if (frontConnection.isConnected()) {
                    frontConnection.getCart().resetBack();
                    this.resetFront();
                } else {
                    this.tryingToClamp();
                }
            }
        }

        if (player.isSecondaryUseActive() && !itemStack.getItem().equals(ccItemInit.CLAMP.get()) && !itemStack.getItem().equals(dmItemInit.DebugItem.get())) {
            return InteractionResult.PASS;
        } else if (this.isVehicle()) {
            return InteractionResult.PASS;
        } else if (!this.level.isClientSide) {
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
        var frontCart = frontConnection.getCart();
        if (frontCart != null) {
            this.poseConfiguration(Direction.NORTH, frontCart, 0, 1);
            this.poseConfiguration(Direction.SOUTH, frontCart, 0, -1);
            this.poseConfiguration(Direction.EAST, frontCart, -1, 0);
            this.poseConfiguration(Direction.WEST, frontCart, 1, 0);
        }
    }

    //////////////////////////////////////TECHNICAL METHODS//////////////////////////

    public void poseConfiguration(Direction direction, AbstractCart cart, int i1, int i3) {
        if (this.getDirection().equals(direction)) {
            if (this.getDirection().equals(cart.getDirection()) && ccUtil.bothUpOrDownOrForward(this, cart)) {
                if (ccUtil.goesFlat(this) && ccUtil.goesFlat(cart) && !ccUtil.isRotating(this)) {
                    if (this.getLocomotive() != null && !ccUtil.isStopped(this.getLocomotive())) this.setDeltaMovement(cart.deltaMovement);
                    if (this.getLocomotive() == null && !ccUtil.isStopped(this.frontConnection.getCart())) this.setDeltaMovement(cart.deltaMovement);
                    this.setPos(cart.position().add(i1 * 1.625D, 0.0D, i3 * 1.625D));
                }
                else if (ccUtil.goesUpper(cart)) {
                    this.setPos(cart.position().add(i1 * 1.149D, -1.149D, i3 * 1.149D));
                }
                else if (!ccUtil.goesUpper(cart)) {
                    this.setPos(cart.position().add(i1 * 1.149D, 1.149D, i3 * 1.149D));
                }
            }
            else {
                if (cart.deltaMovement.length() > 1.0D) {
                    if (this.getLocomotive() != null && !ccUtil.isStopped(this.getLocomotive())) this.setDeltaMovement(-i1, 0.0D, -i3);
                    if (this.getLocomotive() == null && !ccUtil.isStopped(this.frontConnection.getCart())) this.setDeltaMovement(-i1, 0.0D, -i3);
                }
                else {
                    if (this.getLocomotive() != null && !ccUtil.isStopped(this.getLocomotive())) this.setDeltaMovement(-i1, 0.0D, -i3 * cart.deltaMovement.length());
                    if (this.getLocomotive() == null && !ccUtil.isStopped(this.frontConnection.getCart())) this.setDeltaMovement(-i1, 0.0D, -i3 * cart.deltaMovement.length());
                }
            }
        }
    }
    @Override
    public void setDeltaMovement(@NotNull Vec3 vec) {
        if (Math.abs(this.deltaMovement.horizontalDistance()) < 1.0E-2) {
            if (frontConnection.isConnected() && Math.abs(vec.horizontalDistance()) < 51.0E-3) this.deltaMovement = Vec3.ZERO;
            else this.deltaMovement = vec;
        } else {
            if (frontConnection.isConnected() && Math.abs(vec.horizontalDistance()) < 2.0E-2) this.deltaMovement = Vec3.ZERO;
            else this.deltaMovement = vec;
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.entityData.get(DATA_BACKCART_EXISTS) || this.entityData.get(DATA_FRONTCART_EXISTS) && this.isAlive();
    }

    public void spawnAfterCartLeaving() {
        if (frontConnection.isConnected() && this.getPassengers().isEmpty()) {
            AABB miniBox;
            miniBox = this.getBoundingBox().deflate(0.1D, 0.0F, 0.1D);

            ArrayList<Player> previousPassenger = (ArrayList<Player>) this.level.getEntitiesOfClass(Player.class, miniBox);
            if (!previousPassenger.isEmpty()) previousPassenger.get(0).setPos(previousPassenger.get(0).position().add(0.0D, 1.0D, 0.0D));
        }
    }

    public boolean canBeClamped(Player player, ItemStack itemStack) {
        if (player.isSecondaryUseActive()) { //WITH SHIFT PRESSED
            BlockPos blockPos = new BlockPos(this.getX(), this.getY(), this.getZ());
            BlockState blockState = this.level.getBlockState(blockPos);

            return itemStack.getItem().equals(ccItemInit.CLAMP.get())
                    && ccUtil.anyRail(blockState)
                    && !ccUtil.anyRailShape(blockState, blockPos, this).isAscending()
                    && ccUtil.zeroDeltaMovement(this);
        }
        else return false;
    }

    public void activateMinecart(int int1, int int2, int int3, boolean bool1) {
        if (bool1) {
            if (this.isVehicle()) {
                this.ejectPassengers();
            }

            if (this.getHurtTime() == 0) {
                this.setHurtDir(-this.getHurtDir());
                this.setHurtTime(10);
                this.setDamage(50.0F);
                this.markHurt();
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