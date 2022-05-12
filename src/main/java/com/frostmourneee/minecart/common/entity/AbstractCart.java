package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.minecart.ccUtil;
import com.frostmourneee.minecart.core.init.ccSoundInit;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

import static com.frostmourneee.minecart.ccUtil.*;
import static com.frostmourneee.minecart.core.init.ccItemInit.LOCOMOTIVE_ITEM;
import static com.frostmourneee.minecart.core.init.ccItemInit.WAGON_ITEM;

public abstract class AbstractCart extends AbstractMinecart {

    public AbstractCart(EntityType entityType, Level level) {
        super(entityType, level);
    }

    public static final EntityDataAccessor<Boolean> DATA_BACKCART_EXISTS = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_FRONTCART_EXISTS = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_FINDING_BACK_CART_AFTER_REJOIN = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_IS_FINDING_FRONT_CART_AFTER_REJOIN = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);
    //public static final EntityDataAccessor<Boolean> DATA_IS_CLAMPING = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> DATA_DEBUG_MODE = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN); //TODO remove

    public Vec3 delta = Vec3.ZERO;

    public float horAngle = 0.0F; //USED ONLY IN RENDERER, HERE ALWAYS 0
    public float vertAngle = 0.0F;
    public ArrayList<Float> alpha = new ArrayList<>();

    public boolean hasBackCart = false;
    public boolean hasFrontCart = false;
    public boolean isFindingBackCartAfterRejoin = false;
    public boolean isFindingFrontCartAfterRejoin = false;
    public boolean isClamping = false;

    public boolean debugMode = false; //TODO remove
    public int debugCounter = 0;

    public AbstractCart backCart = null;
    public AbstractCart frontCart = null;

    @Override
    public void tick() {
        vanillaTick();

        //My code starts
        fieldsInitAndSidesSync();
        if (this instanceof WagonEntity) customPrint(this, isClamping);
        restoreRelativeCarts();
        clampingToFrontCart();
        posCorrectionToFrontCart();
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
    public void fieldsInitAndSidesSync() {
        /*
          Section where some basic fields are filling with values
         */
        delta = position().subtract(xOld, yOld, zOld);
        if (!zeroDeltaHorizontal()) setYRot(ccUtil.vecToDirection(delta).toYRot());
        /*
          Section for carts' isClamping restoring after rejoining to the game. If (field, data) == (true, false) then clamp process,
          syncing is forbidden. If (field, data) == (false, true) then restore after rejoining, then sync.
         */
        /*if (isClamping != entityData.get(DATA_IS_CLAMPING) && !isClamping) {
            isClamping = entityData.get(DATA_IS_CLAMPING);
        }*/
    }
    public void clampingToFrontCart() {
        if (isClamping) {
            ArrayList<AbstractCart> frontAbstractCart;
            AABB areaOfSearch = getAABBBetweenBlocks(new BlockPos(position()).relative(getDirection()), new BlockPos(position()).relative(getDirection(), 4));
            frontAbstractCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class, areaOfSearch); //LOOKING FOR CARTS IN 4 FRONT BLOCKS
            frontAbstractCart.removeIf(cart -> cart.equals(this));

            if (frontAbstractCart.isEmpty()) {
                clampingFail();
                return;
            }
            AbstractCart potentialFrontCart = frontAbstractCart.get(0);
            for (int i = 1; i < frontAbstractCart.size(); i++) {
                if (frontAbstractCart.get(i).distanceTo(this) < potentialFrontCart.distanceTo(this)) {
                    potentialFrontCart = frontAbstractCart.get(i);
                }
            } //SEARCHING FOR THE NEAREST CART

            ArrayList<BlockPos> furtherBlockPos = getAllBlockPosesInBox
                    (new BlockPos(position()).relative(getDirection()), new BlockPos(potentialFrontCart.position()));

            boolean canScanForFrontCart = true;
            for (BlockPos blockPos : furtherBlockPos) {
                if (!level.getBlockState(blockPos).is(BlockTags.RAILS)) {
                    canScanForFrontCart = false;
                    break;
                }
            } //Checks if blocks except rails are between this and potential frontCart

            if (!canScanForFrontCart) {
                clampingFail();
                return;
            }
            if (!potentialFrontCart.zeroDelta() && !potentialFrontCart.getDirection().equals(getDirection())) {
                clampingFail();
                return;
            }

            smoothClampingFunction(potentialFrontCart);
        }
    } //ONLY WITHOUT REJOIN
    public void smoothClampingFunction(AbstractCart potentialFrontCart) {
        if (distanceTo(potentialFrontCart) > 2.0D) {
            setDeltaMovement(potentialFrontCart.dirToVec3().scale(0.1D));
        } else if (distanceTo(potentialFrontCart) > 1.85D && distanceTo(potentialFrontCart) <= 2.0D) {
            setDeltaMovement(potentialFrontCart.dirToVec3().scale(0.07D));
        } else if (distanceTo(potentialFrontCart) > 1.75D && distanceTo(potentialFrontCart) <= 1.85D) {
            setDeltaMovement(potentialFrontCart.dirToVec3().scale(0.05D));
        } else if (distanceTo(potentialFrontCart) > 1.7D && distanceTo(potentialFrontCart) <= 1.75D) {
            setDeltaMovement(potentialFrontCart.dirToVec3().scale(0.03D));
        } else if (distanceTo(potentialFrontCart) > 1.65D && distanceTo(potentialFrontCart) <= 1.7D) {
            setDeltaMovement(potentialFrontCart.dirToVec3().scale(0.02D));
        }
        if (distanceTo(potentialFrontCart) >= 1.625D && distanceTo(potentialFrontCart) <= 1.65D
                || distanceTo(potentialFrontCart) < 1.625D) {
            setDeltaMovement(Vec3.ZERO);
            potentialFrontCart.connectBack(this);
            connectFront(potentialFrontCart);
            setPos(potentialFrontCart.position().add(potentialFrontCart.oppDirToVec3().scale(1.625D)));

            cartSound(5.5F, ccSoundInit.CART_CLAMP.get());

            isClamping = false;
        }
    }
    public boolean isCommonActing() {
        return !isFindingFrontCartAfterRejoin && !isFindingBackCartAfterRejoin && !isClamping;
    }
    public void clampingFail() {
        setDeltaMovement(getDeltaMovement().scale(0.2D));
        isClamping = false;
    }

    public void collisionProcessing() {
        AABB box;
        if (getCollisionHandler() != null) box = getCollisionHandler().getMinecartCollisionBox(this);
        else box = getBoundingBox().inflate(0.2D, 0.0D, 0.2D);

        if (canBeRidden() && deltaMovement.horizontalDistanceSqr() > 0.01D) {
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

            if (!entity.isVehicle()) {
                if ((entity instanceof AbstractCart && !((AbstractCart)entity).hasFrontCart && !((AbstractCart)entity).hasBackCart)) {
                    entity.push(-d0, 0.0D, -d1);
                } else if (!(entity instanceof AbstractCart)) {
                    entity.push(-d0, 0.0D, -d1);
                }
            }

            switch (getCartType()) {
                case WAGON, LOCOMOTIVE -> {
                    if (!isVehicle() && !hasBackCart && !hasFrontCart) {
                        push(d0, 0.0D, d1); //TODO change
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
                if (entity instanceof AbstractCart && !((AbstractCart) entity).hasBackCart && !((AbstractCart) entity).hasFrontCart) {
                    entity.push(-d0, 0.0D, -d1);
                } else if (!(entity instanceof AbstractCart)) {
                    entity.push(-d0, 0.0D, -d1);
                }
            }

            switch (getCartType()) {
                case WAGON -> {
                    if (!isVehicle() && !hasBackCart && !hasFrontCart) {
                        push(d0 / 5, 0.0D, d1 / 5); //TODO change
                    }
                }
                case LOCOMOTIVE -> {
                    if (entity instanceof AbstractCart) {
                        if (!zeroDeltaBigIndent() && ((AbstractCart) entity).zeroDeltaBigIndent()) {
                            if (hasBackCart) {
                                backCart.resetFront();
                                resetBack();
                            }

                            cartSound(10.0F, ccSoundInit.CART_DEATH.get());
                            remove(RemovalReason.KILLED);
                            if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                spawnAtLocation(LOCOMOTIVE_ITEM.get());
                            }
                        }
                    }
                    else {
                        if (!zeroDeltaBigIndent() && nearZero(entity.deltaMovement, 5.0E-1)) {
                            if (hasBackCart) {
                                backCart.resetFront();
                                resetFront();
                            }

                            cartSound(10.0F, ccSoundInit.CART_DEATH.get());
                            remove(RemovalReason.KILLED);
                            if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                                spawnAtLocation(LOCOMOTIVE_ITEM.get());
                            }
                        }
                    }
                }
            }
        }
    }
    @Override
    public void push(double d1, double d2, double d3) {
        if (isClamping) {
            setDeltaMovement(Vec3.ZERO);
            isClamping = false;
        }
        if (!hasBackCart && !hasFrontCart) {
            this.setDeltaMovement(this.getDeltaMovement().add(d1, d2, d3));
            this.hasImpulse = true;
        }
    }
    public boolean isCollide() {
        return hasFrontCart || hasBackCart;
    }

    public abstract AbstractCart.Type getCartType();

    public void resetFront() {
        setHasFrontCart(false);
        frontCart = null;
    }
    public void resetBack() {
        setHasBackCart(false);
        backCart = null;
    }
    public void resetFull() {
        resetFront();
        resetBack();
    }
    public void connectFront(AbstractCart cart) {
        frontCart = cart;
        setHasFrontCart(true);
    }
    public void connectBack(AbstractCart cart) {
        backCart = cart;
        setHasBackCart(true);
    }

    public void posCorrectionToFrontCart() {
        if (hasFrontCart) {
            /*
              Only for straight line moving section
             */
            if (goesUp()) {
                setPos(frontCart.position().add(frontCart.oppDirToVec3().subtract(0.0D, 1.0D, 0.0D).scale(1.149D)));
            }
            if (goesDown()) {
                setPos(frontCart.position().add(frontCart.oppDirToVec3().add(0.0D, 1.0D, 0.0D).scale(1.149D)));
            }
            if (isOnHorizontalLine()) {
                setPos(frontCart.position().add(frontCart.oppDirToVec3().scale(1.625D)));
            }
        }
    }

    public Vec3 dirToVec3() {
        return new Vec3(getDirection().getNormal().getX(), getDirection().getNormal().getY(), getDirection().getNormal().getZ());
    }
    public Vec3 oppDirToVec3() {
        return dirToVec3().reverse();
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
    @Override
    public void moveMinecartOnRail(BlockPos pos) { //Non-default because getMaximumSpeed is protected
        AbstractMinecart mc = this;
        double d24 = mc.isVehicle() && !isCollide() ? 0.75D : 1.0D;
        double d25 = mc.getMaxSpeedWithRail();
        Vec3 vec3d1 = mc.getDeltaMovement();
        mc.move(MoverType.SELF, new Vec3(Mth.clamp(d24 * vec3d1.x, -d25, d25), 0.0D, Mth.clamp(d24 * vec3d1.z, -d25, d25)));
    }
    public float rotAngle() {
        if (zeroDelta()) {
            return -1 * getYRot() + 270.0F;
        } else {
            if (nearZero(delta.x, 1.0E-3)) return delta.z > 0 ? 270.0F : 90.0F;
            if (nearZero(delta.z, 1.0E-3)) return delta.x > 0 ? 0.0F : 180.0F;

            if (delta.x > 1.0E-3 && delta.z > 1.0E-3) return -1 * (float) Math.toDegrees(Math.atan(delta.z / delta.x)) + 360.0F;
            if (delta.x > 1.0E-3 && delta.z < -1.0E-3) return -1 * (float) Math.toDegrees(Math.atan(delta.z / delta.x));
            if (delta.x < -1.0E-3 && delta.z > 1.0E-3) return -1 * (float) Math.toDegrees(Math.atan(delta.z / delta.x)) + 180.0F;
            if (delta.x < -1.0E-3 && delta.z < -1.0E-3) return -1 * (float) Math.toDegrees(Math.atan(delta.z / delta.x)) + 180.0F;

            return 0.0F;
        }
    }
    public float risingAngle() {
        if (goesFlat()) return 0.0F;
        return (float) Math.toDegrees(Math.atan(delta.y / delta.horizontalDistance()));
    }

    public void tryingToClamp() {
        ArrayList<AbstractCart> frontAbstractCart;
        AABB areaOfSearch = getAABBBetweenBlocks(new BlockPos(position()).relative(getDirection()), new BlockPos(position()).relative(getDirection(), 4));
        frontAbstractCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class, areaOfSearch); //LOOKING FOR CARTS IN 4 FRONT BLOCKS
        frontAbstractCart.removeIf(cart -> cart.equals(this));

        if (frontAbstractCart.isEmpty()) {
            cartSound(5.5F, ccSoundInit.CART_CLAMP_FAIL.get());
            return;
        }
        AbstractCart potentialFrontCart = frontAbstractCart.get(0);
        for (int i = 1; i < frontAbstractCart.size(); i++) {
            if (frontAbstractCart.get(i).distanceTo(this) < potentialFrontCart.distanceTo(this)) {
                potentialFrontCart = frontAbstractCart.get(i);
            }
        } //SEARCHING FOR THE NEAREST CART

        ArrayList<BlockPos> furtherBlockPos = getAllBlockPosesInBox
                (new BlockPos(position()).relative(getDirection()), new BlockPos(potentialFrontCart.position()));

        boolean canScanForFrontCart = true;
        for (BlockPos blockPos : furtherBlockPos) {
            if (!level.getBlockState(blockPos).is(BlockTags.RAILS)) {
                canScanForFrontCart = false;
                break;
            }
        }

        if (!canScanForFrontCart) {
            cartSound(5.5F, ccSoundInit.CART_CLAMP_FAIL.get());
            return;
        }
        if (!potentialFrontCart.getDirection().equals(getDirection())) {
            cartSound(5.5F, ccSoundInit.CART_CLAMP_FAIL.get());
            return;
        }

        setDeltaMovement(Vec3.ZERO);
        potentialFrontCart.setDeltaMovement(Vec3.ZERO);

        if (distanceTo(potentialFrontCart) > 1.625D) isClamping = true;
        else if (!hasBackCart) {
            potentialFrontCart.connectBack(this);
            connectFront(potentialFrontCart);
            setPos(potentialFrontCart.position().add(potentialFrontCart.oppDirToVec3().scale(1.625D)));
            cartSound(5.5F, ccSoundInit.CART_CLAMP.get());
        } else {
            cartSound(5.5F, ccSoundInit.CART_CLAMP_FAIL.get());
        }
    }

    @Override
    protected void comeOffTrack() {
        death();

        remove(RemovalReason.KILLED);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            switch (getCartType()) {
                case WAGON -> spawnAtLocation(WAGON_ITEM.get());
                case LOCOMOTIVE -> spawnAtLocation(LOCOMOTIVE_ITEM.get());
            }
        }
    }  //SERVER ONLY
    @Override
    public void discard() {
        death();

        remove(Entity.RemovalReason.DISCARDED);
    } //in creative  //SERVER ONLY
    @Override
    public void destroy(@NotNull DamageSource damageSource) {
        death();

        remove(Entity.RemovalReason.KILLED);
        if (level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            switch (getCartType()) {
                case WAGON -> spawnAtLocation(WAGON_ITEM.get());
                case LOCOMOTIVE -> spawnAtLocation(LOCOMOTIVE_ITEM.get());
            }
        }
    } //in survival  //SERVER ONLY
    public void death() {
        if (backCart != null) {
            backCart.resetFront();
            resetBack();
        }

        if (frontCart != null) {
            frontCart.resetBack();
            resetFront();
        }

        cartSound(0.0F, ccSoundInit.CART_DEATH.get());
    }  //SERVER ONLY

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
        entityData.define(DATA_IS_FINDING_BACK_CART_AFTER_REJOIN, false);
        entityData.define(DATA_IS_FINDING_FRONT_CART_AFTER_REJOIN, false);
        //entityData.define(DATA_IS_CLAMPING, false);

        entityData.define(DATA_DEBUG_MODE, false); //TODO remove
    }
    @Override
    public void onSyncedDataUpdated(@NotNull EntityDataAccessor<?> data) {
        if (DATA_IS_FINDING_FRONT_CART_AFTER_REJOIN.equals(data)) {
            this.isFindingFrontCartAfterRejoin = (boolean)entityData.get(data);
        }
        if (DATA_IS_FINDING_BACK_CART_AFTER_REJOIN.equals(data)) {
            this.isFindingBackCartAfterRejoin = (boolean)entityData.get(data);
        }

        if (DATA_BACKCART_EXISTS.equals(data) && isCommonActing()) {
            if ((boolean)entityData.get(data)) {

            } else {
                hasBackCart = false;
                backCart = null;
            }
        }
        if (DATA_FRONTCART_EXISTS.equals(data) && isCommonActing()) {
            if ((boolean)entityData.get(data)) {

            } else {
                hasFrontCart = false;
                frontCart = null;
            }
        }
        if (DATA_DEBUG_MODE.equals(data)) {
            this.debugMode = (boolean)entityData.get(data);
        }

        super.onSyncedDataUpdated(data);
    }
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putBoolean("hasFrontCart", hasFrontCart);
        compoundTag.putBoolean("hasBackCart", hasBackCart);
        compoundTag.putBoolean("isFindingBackCartAfterRejoin", hasBackCart);
        compoundTag.putBoolean("isFindingFrontCartAfterRejoin", hasFrontCart);
        if (isClamping) setDeltaMovement(getDeltaMovement().scale(0.2D));
        //compoundTag.putBoolean("isClamping", isClamping);

        compoundTag.putBoolean("debug", debugMode); //TODO remove
    } //SERVER ONLY
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        entityData.set(DATA_DEBUG_MODE, compoundTag.getBoolean("debug")); //TODO remove

        entityData.set(DATA_BACKCART_EXISTS, compoundTag.getBoolean("hasBackCart"));
        entityData.set(DATA_FRONTCART_EXISTS, compoundTag.getBoolean("hasFrontCart"));
        entityData.set(DATA_IS_FINDING_BACK_CART_AFTER_REJOIN, compoundTag.getBoolean("isFindingBackCartAfterRejoin"));
        entityData.set(DATA_IS_FINDING_FRONT_CART_AFTER_REJOIN, compoundTag.getBoolean("isFindingFrontCartAfterRejoin"));
        //entityData.set(DATA_IS_CLAMPING, compoundTag.getBoolean("isClamping"));
    } //SERVER ONLY

    public void restoreRelativeCarts() {
        if (backCart != null) isFindingBackCartAfterRejoin = false;
        if (frontCart != null) isFindingFrontCartAfterRejoin = false;
        if (backCart == null && isFindingBackCartAfterRejoin) {
            AbstractCart backCart = findingNearestCartInArea(getAABBBetweenBlocks(
                    new BlockPos(position()).relative(getDirection().getOpposite()).relative(getDirection().getClockWise()),
                    new BlockPos(position()).relative(getDirection().getOpposite(), 2).relative(getDirection().getCounterClockWise()))
            );
            backCart.connectFront(this);
            connectBack(backCart);

            isFindingBackCartAfterRejoin = false;
            backCart.isFindingFrontCartAfterRejoin = false;
        }

        if (frontCart == null && isFindingFrontCartAfterRejoin) {
            AbstractCart frontCart = findingNearestCartInArea(getAABBBetweenBlocks(
                    new BlockPos(position()).relative(getDirection()).relative(getDirection().getClockWise()),
                    new BlockPos(position()).relative(getDirection(), 2).relative(getDirection().getCounterClockWise()))
            );
            frontCart.connectBack(this);
            connectFront(frontCart);

            isFindingFrontCartAfterRejoin = false;
            frontCart.isFindingBackCartAfterRejoin = false;
        }
    }
    public AbstractCart findingNearestCartInArea(AABB areaOfSearch) {
        ArrayList<AbstractCart> rangeCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class, areaOfSearch);
        rangeCart.removeIf(cart -> cart == this);

        if (!rangeCart.isEmpty()) {
            AbstractCart tmpCart = rangeCart.get(0);
            for (int i = 1; i < rangeCart.size(); i++) { //SEARCHING FOR THE NEAREST
                if (rangeCart.get(i).distanceTo(this) < tmpCart.distanceTo(this)) {
                    tmpCart = rangeCart.get(i);
                }
            }

            return tmpCart;
        } else return null;
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
    public AbstractCart getFirstCart() {
        AbstractCart cart = this;
        while (cart.frontCart != null) {
            cart = cart.frontCart;
        }
        if (cart.getCartType() == Type.LOCOMOTIVE) {
            return cart.backCart;
        } else {
            return cart;
        }
    }
    public AbstractCart getLastCart() {
        AbstractCart cart = this;
        while (cart.backCart != null) {
            cart = cart.backCart;
        }
        return cart;
    }

    public int cartsAhead() {
        int i = 0;
        AbstractCart cart = this;

        while (cart.frontCart != null) {
            cart = cart.frontCart;
            i++;
        }
        return i;
    }
    public int cartsBehind() {
        int i = 0;
        AbstractCart cart = this;

        while (cart.backCart != null) {
            cart = cart.backCart;
            i++;
        }
        return i;
    }
    public int wagonsAhead() {
        return getLocomotive() == null ? cartsAhead() : cartsAhead() - 1;
    }
    public int wagonsBehind() {
        return cartsBehind();
    }
    public int wagonsLength() {
        return wagonsAhead() + wagonsBehind() + 1;}
    public int trainLength() {
        return getLocomotive() == null ? wagonsLength() : wagonsLength() + 1;
    }

    public boolean goesUp() {
        return delta.y > 0;
    }
    public boolean goesDown() {
        return delta.y < 0;
    }
    public boolean goesFlat() {
        if (zeroDelta() && level.getBlockState(blockPosition()).is(BlockTags.RAILS)) {
            return !anyRailShape(level.getBlockState(blockPosition()), blockPosition()).isAscending();
        } else return nearZero(delta.y, 1.0E-3);
    }

    public boolean bothUpOrDownOrForward() {
        if (zeroDelta() || frontCart.zeroDelta()) {
            return anyRailShape(level.getBlockState(blockPosition()), blockPosition()).equals
                    (anyRailShape(frontCart.level.getBlockState(frontCart.blockPosition()), frontCart.blockPosition()));
        } else return (goesUp() && frontCart.goesUp()) ||
                    (goesDown() && frontCart.goesDown()) ||
                    (goesFlat() && frontCart.goesFlat());
    }
    public boolean isOnHorizontalLine() {
        if (hasFrontCart) return Math.abs(getY() - frontCart.getY()) < 1.0E-4
                && (Math.abs(getX() - frontCart.getX()) < 1.0E-4 || Math.abs(getZ() - frontCart.getZ()) < 1.0E-4);
        else return false;
    }

    public boolean zeroDelta() {
        return nearZero(delta, 1.0E-4);
    }
    public boolean zeroDeltaBigIndent() {
        return nearZero(delta, 5.0E-2);
    }
    public boolean zeroDeltaHorizontal() {
        return nearZero(delta.subtract(0.0D, delta.y, 0.0D), 1.0E-3);
    }
    public boolean zeroDeltaHorizontalBigIndent() {
        return nearZero(delta.subtract(0.0D, delta.y, 0.0D), 5.0E-2);
    }
    public boolean isStopped() {
        return delta == Vec3.ZERO;
    }
    public boolean isRotating() {
        BlockPos blockPos = getOnPos().above();
        BlockState blockState = level.getBlockState(blockPos);

        if (blockState.is(BlockTags.RAILS)) {
            RailShape shape = anyRailShape(blockState, blockPos);
            return railIsRotating(shape);
        } else return false;
    }

    public RailShape anyRailShape(BlockState blockState, BlockPos blockPos) {
        if (blockState.is(BlockTags.RAILS)) return ((BaseRailBlock)blockState.getBlock())
                .getRailDirection(blockState, this.level, blockPos, this);
        else return null;
    }

    public void setHasBackCart(boolean bool) {
        if (bool && backCart != null) {
            hasBackCart = true;
            entityData.set(DATA_BACKCART_EXISTS, true);
        } else {
            hasBackCart = false;
            entityData.set(DATA_BACKCART_EXISTS, false);
        }
    } //Make sure you setHasBackCart to true only if backCart != null
    public void setHasFrontCart(boolean bool) {
        if (bool && frontCart != null) {
            hasFrontCart = true;
            entityData.set(DATA_FRONTCART_EXISTS, true);
        } else {
            hasFrontCart = false;
            entityData.set(DATA_FRONTCART_EXISTS, false);
        }
    } //Make sure you setHasFrontCart to true only if frontCart != null

    public void cartSoundPrevious(float distance, SoundEvent soundEvent) {
        Player player = level.getNearestPlayer(this, distance);

        if (player != null) level.playSound(player, new BlockPos(position()),
                soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
        else if (distance == 0.0F) level.playSound(null, new BlockPos(position()),
                soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
    public void cartSound(float distance, SoundEvent soundEvent) {
        Player player = level.getNearestPlayer(this, distance);
        level.playSound(player, new BlockPos(position()), soundEvent, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    public enum Type {
        WAGON,
        LOCOMOTIVE
    }
}
