package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.minecart.core.ccUtil;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.frostmourneee.minecart.core.init.ccItemInit.LOCOMOTIVE_ITEM;
import static com.frostmourneee.minecart.core.init.ccItemInit.WAGON_ITEM;

public abstract class AbstractCart extends AbstractMinecart {

    public AbstractCart(EntityType entityType, Level level) {
        super(entityType, level);
    }

    public static final EntityDataAccessor<Boolean> DATA_BACKCART_EXISTS = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_FRONTCART_EXISTS = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_HORIZONTAL_ROTATION_ANGLE = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_VERTICAL_ROTATION_ANGLE = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> DATA_DEBUG_MODE = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN); //TODO remove debug

    public float horAngle; //CLIENTSIDE ONLY
    public float vertAngle; //CLIENTSIDE ONLY
    public BlockPos posOfBackCart;
    public boolean hasBackCart = false;
    public BlockPos posOfFrontCart;
    public boolean hasFrontCart = false;

    public boolean debugMode = true; //TODO remove debug

    public AbstractCart backCart = null;
    public AbstractCart frontCart = null;

    @Override
    public void tick() {
        vanillaTick();

        //My code starts
        collisionProcessing();
    }

    public void vanillaTick() {
        if (getHurtTime() > 0) {
            setHurtTime(getHurtTime() - 1);
        }

        if (getDamage() > 0.0F) {
            setDamage(getDamage() - 1.0F);
        }

        checkOutOfWorld();
        handleNetherPortal();
        if (level.isClientSide) {
            if (lSteps > 0) {
                double d5 = getX() + (lx - getX()) / (double)lSteps;
                double d6 = getY() + (ly - getY()) / (double)lSteps;
                double d7 = getZ() + (lz - getZ()) / (double)lSteps;
                --lSteps;
                setPos(d5, d6, d7);
            } else {
                reapplyPosition();
            }

        } else {
            if (!isNoGravity()) {
                double d0 = isInWater() ? -0.005D : -0.04D;
                setDeltaMovement(getDeltaMovement().add(0.0D, d0, 0.0D));
            }

            int k = Mth.floor(getX());
            int i = Mth.floor(getY());
            int j = Mth.floor(getZ());
            if (level.getBlockState(new BlockPos(k, i - 1, j)).is(BlockTags.RAILS)) {
                --i;
            }

            BlockPos blockpos = new BlockPos(k, i, j);
            BlockState blockstate = level.getBlockState(blockpos);
            if (canUseRail() && BaseRailBlock.isRail(blockstate)) {
                moveAlongTrack(blockpos, blockstate);
                if (blockstate.getBlock() instanceof PoweredRailBlock && ((PoweredRailBlock) blockstate.getBlock()).isActivatorRail()) {
                    activateMinecart(k, i, j, blockstate.getValue(PoweredRailBlock.POWERED));
                }
            } else {
                comeOffTrack();
            }

            checkInsideBlocks();

            double d4 = Mth.wrapDegrees(getYRot() - yRotO);
            if (d4 < -170.0D || d4 >= 170.0D) {
                flipped = !flipped;
            }

            updateInWaterStateAndDoFluidPushing();
            if (isInLava()) {
                lavaHurt();
                fallDistance *= 0.5F;
            }

            firstTick = false;
        }
    }

    public void collisionProcessing() {
        AABB box;

        if (getCollisionHandler() != null) box = getCollisionHandler().getMinecartCollisionBox(this);
        else box = getBoundingBox().inflate(0.2D, 0.0D, 0.2D);

        if (canBeRidden() && getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
            List<Entity> list = level.getEntities(this, box, EntitySelector.pushableBy(this));
            if (!list.isEmpty()) {
                for (Entity entity1 : list) {
                    if (!(entity1 instanceof Player) && !(entity1 instanceof IronGolem) && !(entity1 instanceof AbstractMinecart) && !isVehicle() && !entity1.isPassenger()) {
                        entity1.startRiding(this);
                    } else {
                        entityPushingBySelf(entity1);
                    }
                }
            }
        } else {
            for(Entity entity : level.getEntities(this, box)) {
                if (!hasPassenger(entity) && entity.isPushable() && entity instanceof AbstractMinecart) {
                    if (!entity.isPassengerOfSameVehicle(this)) {
                        if (!entity.noPhysics && !noPhysics) {
                            selfPushingByEntity(entity);
                        }
                    }
                }
            }
        }
    }

    public void entityPushingBySelf(Entity entity) {
        double d0 = getX() - entity.getX();
        double d1 = getZ() - entity.getZ();
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
                case WAGON -> {
                    if (!entity.isVehicle()) {
                        if ((entity instanceof WagonEntity && ((WagonEntity) entity).backCart == null) ||
                                (entity instanceof LocomotiveEntity && ((LocomotiveEntity) entity).backCart == null)) {
                            entity.push(-d0, 0.0D, -d1);
                        } else if (!(entity instanceof WagonEntity) && !(entity instanceof LocomotiveEntity)) {
                            entity.push(-d0, 0.0D, -d1);
                        }
                    }
                    if (!isVehicle() && backCart == null && !hasFrontCart) {
                        push(d0 / 5, 0.0D, d1 / 5); //TODO change
                    }
                }
                case LOCOMOTIVE -> {
                    if (!entity.isVehicle()) {
                        entity.push(-d0, 0.0D, -d1);
                    }
                    if (!isVehicle() && backCart == null) {
                        push(d0, 0.0D, d1);
                    }
                }
            }
        }
    }
    public void selfPushingByEntity(Entity entity) {
        double d0 = getX() - entity.getX();
        double d1 = getZ() - entity.getZ();
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

            if (!entity.isVehicle()) {
                if (entity instanceof AbstractCart && ((AbstractCart) entity).backCart == null) {
                    entity.push(-d0, 0.0D, -d1);
                } else if (!(entity instanceof AbstractCart)) {
                    entity.push(-d0, 0.0D, -d1);
                }
            }

            switch (getCartType()) {
                case WAGON -> {
                    if (!isVehicle() && backCart == null && !hasFrontCart) {
                        push(d0 / 5, 0.0D, d1 / 5); //TODO change
                    }
                }
                case LOCOMOTIVE -> {
                    if (!ccUtil.zeroDeltaMovementBigIndent(this) && ccUtil.zeroDeltaMovementBigIndent((AbstractCart) entity)) {
                        if (backCart != null) {
                            backCart.resetFront();
                            backCart.setDeltaMovement(getDeltaMovement());
                        }

                        remove(RemovalReason.KILLED);
                        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                            spawnAtLocation(LOCOMOTIVE_ITEM.get());
                        }
                    }
                }
            }
        }
    }

    public abstract AbstractCart.Type getCartType();

    public void resetFront() {
        entityData.set(DATA_FRONTCART_EXISTS, false);
        hasFrontCart = false;
        frontCart = null;
    }
    public void resetBack() {
        entityData.set(DATA_BACKCART_EXISTS, false);
        hasBackCart = false;
        backCart = null;
    }
    public void resetFull() {
        resetFront();
        resetBack();
    }

    public void connectFront(AbstractCart cart) {
        frontCart = cart;
        hasFrontCart = true;
        entityData.set(DATA_FRONTCART_EXISTS, true);
    }
    public void connectBack(AbstractCart cart) {
        backCart = cart;
        hasBackCart = true;
        entityData.set(DATA_BACKCART_EXISTS, true);
    }

    @Override
    public Vec3 getPos(double x, double y, double z) { //Used in Renderer class
        int i = Mth.floor(x);
        int j = Mth.floor(y);
        int k = Mth.floor(z);
        if (level.getBlockState(new BlockPos(i, j + 1, k)).is(BlockTags.RAILS)) {
            ++j;
        } else if (level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockState blockstate = level.getBlockState(new BlockPos(i, j, k));
        if (BaseRailBlock.isRail(blockstate)) {
            RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, level, new BlockPos(i, j, k), this);
            Pair<Vec3i, Vec3i> pair = exits(railshape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i1 = pair.getSecond();
            double d0 = (double)i + 0.5D + (double)vec3i.getX() * 0.5D;
            double d1 = (double)j + 0.0625D + (double)vec3i.getY() * 0.5D;
            double d2 = (double)k + 0.5D + (double)vec3i.getZ() * 0.5D;
            double d3 = (double)i + 0.5D + (double)vec3i1.getX() * 0.5D;
            double d4 = (double)j + 0.0625D + (double)vec3i1.getY() * 0.5D;
            double d5 = (double)k + 0.5D + (double)vec3i1.getZ() * 0.5D;
            double d6 = d3 - d0;
            double d7 = (d4 - d1) * 2.0D;
            double d8 = d5 - d2;
            double d9;
            if (d6 == 0.0D) {
                d9 = z - (double)k;
            } else if (d8 == 0.0D) {
                d9 = x - (double)i;
            } else {
                double d10 = x - d0;
                double d11 = z - d2;
                d9 = (d10 * d6 + d11 * d8) * 2.0D;
            }

            x = d0 + d6 * d9;
            y = d1 + d7 * d9;
            z = d2 + d8 * d9;
            if (d7 < 0.0D) {
                ++y;
            } else if (d7 > 0.0D) {
                y += 0.5D;
            }

            return new Vec3(x, y, z);
        } else {
            return null;
        }
    }
    @Override
    public Vec3 getPosOffs(double x, double y, double z, double factor) {
        int i = Mth.floor(x);
        int j = Mth.floor(y);
        int k = Mth.floor(z);
        if (level.getBlockState(new BlockPos(i, j + 1, k)).is(BlockTags.RAILS)) {
            ++j;
        } else if (level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockState blockstate = level.getBlockState(new BlockPos(i, j, k));
        if (BaseRailBlock.isRail(blockstate)) {
            RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, level, new BlockPos(i, j, k), this);
            y = j;
            if (railshape.isAscending()) {
                y = j + 1;
            }

            Pair<Vec3i, Vec3i> pair = exits(railshape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i1 = pair.getSecond();
            double d0 = vec3i1.getX() - vec3i.getX();
            double d1 = vec3i1.getZ() - vec3i.getZ();
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            d0 /= d2;
            d1 /= d2;
            x += d0 * factor;
            z += d1 * factor;
            if (vec3i.getY() != 0 && Mth.floor(x) - i == vec3i.getX() && Mth.floor(z) - k == vec3i.getZ()) {
                y += vec3i.getY();
            } else if (vec3i1.getY() != 0 && Mth.floor(x) - i == vec3i1.getX() && Mth.floor(z) - k == vec3i1.getZ()) {
                y += vec3i1.getY();
            }

            return getPos(x, y, z);
        } else {
            return null;
        }
    }

    public void tryingToClamp() {

        ArrayList<AbstractCart> frontAbstractCart;
        frontAbstractCart = frontOnRailCart(new BlockPos(position()));

        frontAbstractCart.removeIf(cart -> cart.equals(this));

        if (!frontAbstractCart.isEmpty()) connection(frontAbstractCart);
    }
    public ArrayList<AbstractCart> frontOnRailCart(BlockPos blockPos) {
        return (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class,
                new AABB(blockPos.relative(getDirection())).inflate(0.9D, 0.0D, 0.9D));
    }
    public void connection(ArrayList<AbstractCart> frontAbstractCart) {

        if (frontAbstractCart.get(0).getDirection().equals(getDirection())) {

            connectFront(frontAbstractCart.get(0));
            frontCart.connectBack(this);

            switch (getDirection()) {
                case EAST -> setPos(frontCart.position().add(-1.625D, 0.0D, 0.0D));
                case NORTH -> setPos(frontCart.position().add(0.0D, 0.0D, 1.625D));
                case WEST -> setPos(frontCart.position().add(1.625D, 0.0D, 0.0D));
                case SOUTH -> setPos(frontCart.position().add(0.0D, 0.0D, -1.625D));
            }
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    protected void comeOffTrack() {
        if (backCart != null) {
            backCart.resetFront();
            backCart.setDeltaMovement(getDeltaMovement());
            resetBack();
        }

        if (frontCart != null) {
            frontCart.resetBack();
            resetFront();
        }

        remove(RemovalReason.KILLED);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            switch (getCartType()) {
                case WAGON -> spawnAtLocation(WAGON_ITEM.get());
                case LOCOMOTIVE -> spawnAtLocation(LOCOMOTIVE_ITEM.get());
            }
        }
    }
    @Override
    public void discard() { //in creative
        if (hasBackCart) backCart.resetFront();
        if (hasFrontCart) frontCart.resetBack();
        remove(Entity.RemovalReason.DISCARDED);
    }
    @Override
    public void destroy(@NotNull DamageSource damageSource) { //in survival
        if (hasBackCart) backCart.resetFront();
        if (hasFrontCart) frontCart.resetBack();
        remove(Entity.RemovalReason.KILLED);

        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            switch (getCartType()) {
                case WAGON -> spawnAtLocation(WAGON_ITEM.get());
                case LOCOMOTIVE -> spawnAtLocation(LOCOMOTIVE_ITEM.get());
            }
        }
    }

    @Override
    public ItemStack getPickResult() {
        return switch (getCartType()) {
            case WAGON ->  new ItemStack(WAGON_ITEM.get());
            case LOCOMOTIVE -> new ItemStack(LOCOMOTIVE_ITEM.get());
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        entityData.define(DATA_FRONTCART_EXISTS, false);
        entityData.define(DATA_BACKCART_EXISTS, false);
        entityData.define(DATA_DEBUG_MODE, true);

        entityData.define(DATA_HORIZONTAL_ROTATION_ANGLE, 0.0F);
        entityData.define(DATA_VERTICAL_ROTATION_ANGLE, 0.0F);
    } //TODO remove debug
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putBoolean("HasFrontCart", hasFrontCart);
        compoundTag.putBoolean("HasBackCart", hasBackCart);
        compoundTag.putBoolean("Debug", debugMode);
        compoundTag.putFloat("HorAngle", entityData.get(DATA_HORIZONTAL_ROTATION_ANGLE));
        compoundTag.putFloat("VertAngle", entityData.get(DATA_VERTICAL_ROTATION_ANGLE));

        saveNearCartData(backCart, compoundTag, "BackCartExists", DATA_BACKCART_EXISTS);
        saveNearCartData(frontCart, compoundTag, "FrontCartExists", DATA_FRONTCART_EXISTS);
    } //TODO remove debug
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        debugMode = compoundTag.getBoolean("Debug");
        entityData.set(DATA_DEBUG_MODE, debugMode);
        horAngle = compoundTag.getFloat("HorAngle");
        entityData.set(DATA_HORIZONTAL_ROTATION_ANGLE, horAngle);
        vertAngle = compoundTag.getFloat("VertAngle");
        entityData.set(DATA_VERTICAL_ROTATION_ANGLE, vertAngle);
        hasFrontCart = compoundTag.getBoolean("FrontCartExists");
        entityData.set(DATA_FRONTCART_EXISTS, hasFrontCart);
        hasBackCart = compoundTag.getBoolean("BackCartExists");
        entityData.set(DATA_BACKCART_EXISTS, hasBackCart);

        if (hasBackCart) {
            int[] cartPos;
            cartPos = compoundTag.getIntArray("BackCartExistsPos");
            posOfBackCart = new BlockPos(cartPos[0], cartPos[1], cartPos[2]);

            ArrayList<AbstractCart> rangeCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class, new AABB(posOfBackCart));
            if (!rangeCart.isEmpty()) {
                AbstractCart backCart = rangeCart.get(0);
                backCart.connectFront(this);
                connectBack(backCart);
            }
        }

        if (hasFrontCart) {
            int[] cartPos;
            cartPos = compoundTag.getIntArray("FrontCartExistsPos");
            posOfFrontCart = new BlockPos(cartPos[0], cartPos[1], cartPos[2]);

            ArrayList<AbstractCart> rangeCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class, new AABB(posOfFrontCart));
            if (!rangeCart.isEmpty()) {
                AbstractCart frontCart = rangeCart.get(0);
                frontCart.connectBack(this);
                connectFront(frontCart);
            }
        }
    } //TODO remove debug
    public void saveNearCartData(AbstractCart cart, CompoundTag compoundTag, String name, EntityDataAccessor<Boolean> accessor) {
        if (entityData.get(accessor) && cart != null) {
            compoundTag.putBoolean(name, true);
            int[] cartPos = new int[3];
            cartPos[0] = cart.getBlockX();
            cartPos[1] = cart.getBlockY();
            cartPos[2] = cart.getBlockZ();
            compoundTag.putIntArray(name + "Pos", cartPos);
        } else {
            compoundTag.putBoolean(name, false);
        }
    }

    public AbstractCart getLocomotive() {
        AbstractCart cart = this;
        while (cart.frontCart != null) {
            cart = cart.frontCart;
        }
        if (cart.getCartType() == Type.LOCOMOTIVE) {
            return cart;
        } else {
            return null;
        }
    }
    public AbstractCart getLastCart() {
        AbstractCart cart = this;
        while (cart.backCart != null) {
            cart = cart.backCart;
        }
        return cart;
    }
    /*public int wagonsLength() {
        //TODO realize
    } //SERVER ONLY
    public int trainLength() {
        if (recursiveLocomotive() != null) return wagonsLength(1) + 1;
            else return wagonsLength(1);
    }*/

    public enum Type {
        WAGON,
        LOCOMOTIVE
    }
}
