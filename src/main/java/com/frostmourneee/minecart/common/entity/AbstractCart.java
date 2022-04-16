package com.frostmourneee.minecart.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public abstract class AbstractCart extends AbstractMinecart {

    public AbstractCart(EntityType entityType, Level level) {
        super(entityType, level);
    }

    public static final EntityDataAccessor<Boolean> DATA_BACKCART_EXISTS = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_HORIZONTAL_ROTATION_ANGLE = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_VERTICAL_ROTATION_ANGLE = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> DATA_DEBUG_MODE = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.BOOLEAN); //TODO remove debug

    public float horAngle; //CLIENTSIDE ONLY
    public float vertAngle; //CLIENTSIDE ONLY
    public BlockPos posOfBackCart;
    public boolean shouldHasBackCart = false;

    public boolean debugMode = false; //TODO remove debug

    public AbstractCart backCart = null;
    public AbstractCart frontCart = null;

    @Override
    public void tick() {
        this.vanillaTick();

        //My code starts
        this.collisionProcessing();
    }

    public void vanillaTick() {
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        this.checkOutOfWorld();
        this.handleNetherPortal();
        if (this.level.isClientSide) {
            if (this.lSteps > 0) {
                double d5 = this.getX() + (this.lx - this.getX()) / (double)this.lSteps;
                double d6 = this.getY() + (this.ly - this.getY()) / (double)this.lSteps;
                double d7 = this.getZ() + (this.lz - this.getZ()) / (double)this.lSteps;
                --this.lSteps;
                this.setPos(d5, d6, d7);
            } else {
                this.reapplyPosition();
            }

        } else {
            if (!this.isNoGravity()) {
                double d0 = this.isInWater() ? -0.005D : -0.04D;
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, d0, 0.0D));
            }

            int k = Mth.floor(this.getX());
            int i = Mth.floor(this.getY());
            int j = Mth.floor(this.getZ());
            if (this.level.getBlockState(new BlockPos(k, i - 1, j)).is(BlockTags.RAILS)) {
                --i;
            }

            BlockPos blockpos = new BlockPos(k, i, j);
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (canUseRail() && BaseRailBlock.isRail(blockstate)) {
                this.moveAlongTrack(blockpos, blockstate);
                if (blockstate.getBlock() instanceof PoweredRailBlock && ((PoweredRailBlock) blockstate.getBlock()).isActivatorRail()) {
                    this.activateMinecart(k, i, j, blockstate.getValue(PoweredRailBlock.POWERED));
                }
            } else {
                this.comeOffTrack();
            }

            this.checkInsideBlocks();

            double d4 = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            if (d4 < -170.0D || d4 >= 170.0D) {
                this.flipped = !this.flipped;
            }

            this.updateInWaterStateAndDoFluidPushing();
            if (this.isInLava()) {
                this.lavaHurt();
                this.fallDistance *= 0.5F;
            }

            this.firstTick = false;
        }
    }

    public void collisionProcessing() {
        AABB box;

        if (getCollisionHandler() != null) box = getCollisionHandler().getMinecartCollisionBox(this);
        else box = this.getBoundingBox().inflate(0.2D, 0.0D, 0.2D);

        if (canBeRidden() && this.getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
            List<Entity> list = this.level.getEntities(this, box, EntitySelector.pushableBy(this));
            if (!list.isEmpty()) {
                for (Entity entity1 : list) {
                    if (!(entity1 instanceof Player) && !(entity1 instanceof IronGolem) && !(entity1 instanceof AbstractMinecart) && !this.isVehicle() && !entity1.isPassenger()) {
                        entity1.startRiding(this);
                    } else {
                        entityPushingBySelf(entity1);
                    }
                }
            }
        } else {
            for(Entity entity : this.level.getEntities(this, box)) {
                if (!this.hasPassenger(entity) && entity.isPushable() && entity instanceof AbstractMinecart) {
                    if (!entity.isPassengerOfSameVehicle(this)) {
                        if (!entity.noPhysics && !this.noPhysics) {
                            selfPushingByEntity(entity);
                        }
                    }
                }
            }
        }
    }

    public void entityPushingBySelf(Entity entity){
        double d0 = this.getX() - entity.getX();
        double d1 = this.getZ() - entity.getZ();
        double d2 = Mth.absMax(d0, d1);

        if (d2 >= (double) 0.01F) {
            d2 = Math.sqrt(d2);
            d0 = d0 / d2;
            d1 = d1 / d2;
            double d3 = 1.0D / d2;

            if (d3 > 1.0D) {
                d3 = 1.0D;
            }

            d0 *= d3;
            d1 *= d3;
            d0 *= 0.05F;
            d1 *= 0.05F;

            switch (getCartType()) {
                case WAGON:
                    if (!entity.isVehicle()) {
                        if ((entity instanceof WagonEntity && ((WagonEntity) entity).backCart == null) ||
                                (entity instanceof LocomotiveEntity && ((LocomotiveEntity) entity).backCart == null)) {
                            entity.push(-d0, 0.0D, -d1);
                        } else if (!(entity instanceof WagonEntity) && !(entity instanceof LocomotiveEntity)) {
                         entity.push(-d0, 0.0D, -d1);
                        }
                    }
                    if (!this.isVehicle() && this.backCart == null && !((WagonEntity)this).isClamped) {
                        this.push(d0 / 5, 0.0D, d1 / 5); //TODO change
                    }
                    break;
                case LOCOMOTIVE:

            }
        }
    }
    public void selfPushingByEntity(Entity entity){
        double d0 = this.getX() - entity.getX();
        double d1 = this.getZ() - entity.getZ();
        double d2 = Mth.absMax(d0, d1);
        if (d2 >= (double)0.01F) {
            d2 = Math.sqrt(d2);
            d0 /= d2;
            d1 /= d2;
            double d3 = 1.0D / d2;

            if (d3 > 1.0D) {
                d3 = 1.0D;
            }

            d0 *= d3;
            d1 *= d3;
            d0 *= 0.05F;
            d1 *= 0.05F;

            switch (this.getCartType()) {
                case WAGON:
                    if (!entity.isVehicle()) {
                        if ((entity instanceof WagonEntity && ((WagonEntity) entity).backCart == null) ||
                            (entity instanceof LocomotiveEntity && ((LocomotiveEntity) entity).backCart == null)) {
                            entity.push(-d0, 0.0D, -d1);
                        } else if (!(entity instanceof WagonEntity) && !(entity instanceof LocomotiveEntity)) {
                            entity.push(-d0, 0.0D, -d1);
                        }
                    }

                    if (!this.isVehicle() && this.backCart == null && !((WagonEntity)this).isClamped) {
                        this.push(d0 / 5, 0.0D, d1 / 5); //TODO change
                    }
                    break;
                case LOCOMOTIVE:

            }
        }
    }

    public abstract AbstractCart.Type getCartType();

    public void resetBack() {
        this.entityData.set(DATA_BACKCART_EXISTS, false);
        this.backCart = null;
    }

    public void resetFront() {

    }

    public enum Type {
        WAGON,
        LOCOMOTIVE
    }
}
