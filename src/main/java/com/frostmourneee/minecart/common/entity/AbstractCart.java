package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.minecart.ccUtil;
import com.frostmourneee.minecart.core.init.ccSoundInit;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
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

    public static final EntityDataAccessor<Boolean> DATA_DEBUG_MODE = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN); //TODO remove debug

    public Vec3 delta = Vec3.ZERO;
    public ArrayList<Integer> verticalMovementType = new ArrayList<>(); //1 = up; 0 = flat; -1 = down
    public boolean isPosCorrected = true;

    public float horAngle = 0.0F; //USED ONLY IN RENDERER, HERE ALWAYS 0
    public float vertAngle = 0.0F;
    public ArrayList<Float> alpha = new ArrayList<>();

    public boolean hasBackCart = false;
    public boolean hasFrontCart = false;
    public boolean hadBackCart = false;
    public boolean hadFrontCart = false;
    public boolean initTick = true;

    public boolean debugMode = false; //TODO remove debug
    public int debugCounter = 0;

    public AbstractCart backCart = null;
    public AbstractCart frontCart = null;

    @Override
    public void tick() {
        vanillaTick();

        //My code starts
        delta = position().subtract(xOld, yOld, zOld);
        verticalMovementType.add(goesUp() ? 1 : goesFlat() ? 0 : -1);
        if (verticalMovementType.size() == 3) verticalMovementType.remove(0);
        if (!zeroDeltaHorizontal()) setYRot(ccUtil.vecToDirection(delta).toYRot());
        if (initTick) {
            if (entityData.get(DATA_FRONTCART_EXISTS)) {
                hadFrontCart = true;
            }
            if (entityData.get(DATA_BACKCART_EXISTS)) {
                hadBackCart = true;
            }

            debugMode = entityData.get(DATA_DEBUG_MODE); //TODO debug
            initTick = false;
        }

        restoreRelativeCarts();
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
                                backCart.setDeltaMovement(getDeltaMovement());
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
                                backCart.setDeltaMovement(getDeltaMovement());
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
        if (!hasBackCart && !hasFrontCart) {
            this.setDeltaMovement(this.getDeltaMovement().add(d1, d2, d3));
            this.hasImpulse = true;
        }
    }

    public abstract AbstractCart.Type getCartType();

    public void resetFront() {
        hasFrontCart = false;
        frontCart = null;
    }
    public void resetBack() {
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
    }
    public void connectBack(AbstractCart cart) {
        backCart = cart;
        hasBackCart = true;
    }

    public void posCorrectionToFrontCart() {
        if (hasFrontCart && verticalMovementType.size() == 2) {
            if (!verticalMovementType.get(0).equals(verticalMovementType.get(1))) {
                isPosCorrected = false;
            }
            double dist = frontCart.position().subtract(position()).length();

            if (!isPosCorrected) {
                if (nearZero(dist - 2.298D, 1.0E-1)) {
                    if (goesUp()) {
                        setPos(frontCart.position().add(frontCart.oppDirToVec3().subtract(0.0D, 1.0D, 0.0D).scale(1.149D)));
                    }
                    if (goesDown()) {
                        setPos(frontCart.position().add(frontCart.oppDirToVec3().add(0.0D, 1.0D, 0.0D).scale(1.149D)));
                    }
                    isPosCorrected = true;
                }
                if (isOnHorizontalLine()) {
                    setPos(frontCart.position().add(frontCart.oppDirToVec3().scale(1.625D)));
                    isPosCorrected = true;
                }
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

    public void tryingToClamp() {
        ArrayList<AbstractCart> frontAbstractCart;
        AABB areaOfSearch = getAABBBetweenBlocks(new BlockPos(position()).relative(getDirection()), new BlockPos(position()).relative(getDirection(), 4));
        frontAbstractCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class, areaOfSearch); //LOOKING FOR CARTS IN 3 FRONT BLOCKS
        frontAbstractCart.removeIf(cart -> cart.equals(this));

        if (frontAbstractCart.isEmpty()) {
            cartSound(5.5F, ccSoundInit.CART_CLAMP_FAIL.get());
        } else {
            connection(frontAbstractCart);
        }
    }
    public void connection(ArrayList<AbstractCart> frontAbstractCart) {
        for (int i = 1; i < frontAbstractCart.size(); i++) { //SEARCHING FOR THE NEAREST
            if (frontAbstractCart.get(i).distanceTo(this) < frontAbstractCart.get(0).distanceTo(this)) {
                frontAbstractCart.set(0, frontAbstractCart.get(i));
            }
        }

        if (frontAbstractCart.get(0).getDirection().equals(getDirection())) {
            setDeltaMovement(Vec3.ZERO);
            connectFront(frontAbstractCart.get(0));
            frontCart.setDeltaMovement(Vec3.ZERO);
            frontCart.connectBack(this);

            if (distanceTo(frontCart) < 1.625D) {
                double distDelta = 1.625D - distanceTo(frontCart);
                frontCart.setPos(frontCart.position().add(frontCart.dirToVec3().scale(distDelta)));
            }
            setPos(frontCart.position().add(oppDirToVec3().scale(1.625D)));
            cartSound(5.5F, ccSoundInit.CART_CLAMP.get());

            AbstractCart cart = this; //PULLING BACK CARTS UP TO CORRECT COORDS
            while (cart.backCart != null) {
                cart = cart.backCart;
                cart.setPos(cart.frontCart.position().add(oppDirToVec3().scale(1.625D)));
            }
        } else {
            cartSound(5.5F, ccSoundInit.CART_CLAMP_FAIL.get());
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

        cartSound(10.0F, ccSoundInit.CART_DEATH.get());
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

        cartSound(10.0F, ccSoundInit.CART_DEATH.get());
        remove(Entity.RemovalReason.DISCARDED);
    }
    @Override
    public void destroy(@NotNull DamageSource damageSource) { //in survival
        if (hasBackCart) backCart.resetFront();
        if (hasFrontCart) frontCart.resetBack();

        cartSound(10.0F, ccSoundInit.CART_DEATH.get());
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
        entityData.define(DATA_DEBUG_MODE, false);
    } //TODO remove debug
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) { //SERVER ONLY
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putBoolean("HasFrontCart", hasFrontCart);
        compoundTag.putBoolean("HasBackCart", hasBackCart);

        compoundTag.putBoolean("Debug", debugMode);
    } //TODO remove debug
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) { //SERVER ONLY
        super.readAdditionalSaveData(compoundTag);

        entityData.set(DATA_DEBUG_MODE, compoundTag.getBoolean("Debug"));

        entityData.set(DATA_BACKCART_EXISTS, compoundTag.getBoolean("HasBackCart"));
        entityData.set(DATA_FRONTCART_EXISTS, compoundTag.getBoolean("HasFrontCart"));
    } //TODO remove debug

    public void restoreRelativeCarts() {
        if (backCart != null) hadBackCart = false;
        if (frontCart != null) hadFrontCart = false;
        if (backCart == null && hadBackCart) {
            ArrayList<AbstractCart> rangeCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class,
                    getAABBBetweenBlocks(new BlockPos(position()).relative(getDirection().getOpposite()).relative(getDirection().getClockWise()),
                    new BlockPos(position()).relative(getDirection().getOpposite(), 2).relative(getDirection().getCounterClockWise()))
            );

            rangeCart.removeIf(cart -> cart == this);
            if (!rangeCart.isEmpty()) {
                for (int i = 1; i < rangeCart.size(); i++) { //SEARCHING FOR THE NEAREST
                    if (rangeCart.get(i).distanceTo(this) < rangeCart.get(0).distanceTo(this)) {
                        rangeCart.set(0, rangeCart.get(i));
                    }
                }
                AbstractCart backCart = rangeCart.get(0);
                backCart.connectFront(this);
                connectBack(backCart);

                hasBackCart = true;
                hadBackCart = false;
            }
        }

        if (frontCart == null && hadFrontCart) {
            ArrayList<AbstractCart> rangeCart = (ArrayList<AbstractCart>) level.getEntitiesOfClass(AbstractCart.class,
                    getAABBBetweenBlocks(new BlockPos(position()).relative(getDirection()).relative(getDirection().getClockWise()),
                    new BlockPos(position()).relative(getDirection(), 2).relative(getDirection().getCounterClockWise()))
            );

            rangeCart.removeIf(cart -> cart == this);
            if (!rangeCart.isEmpty()) {
                for (int i = 1; i < rangeCart.size(); i++) { //SEARCHING FOR THE NEAREST
                    if (rangeCart.get(i).distanceTo(this) < rangeCart.get(0).distanceTo(this)) {
                        rangeCart.set(0, rangeCart.get(i));
                    }
                }
                AbstractCart frontCart = rangeCart.get(0);
                frontCart.connectBack(this);
                connectFront(frontCart);

                hasFrontCart = true;
                hadFrontCart = false;
            }
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
        return wagonsAhead() + wagonsBehind() + 1;
    } //SERVER ONLY
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
        if (zeroDelta() && isRail(level.getBlockState(blockPosition()))) {
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

        if (isRail(blockState)) {
            RailShape shape = anyRailShape(blockState, blockPos);
            return railIsRotating(shape);
        } else return false;
    }

    public static boolean isRail(BlockState blockState) {
        return blockState.is(Blocks.RAIL) || blockState.is(Blocks.POWERED_RAIL) || blockState.is(Blocks.ACTIVATOR_RAIL) || blockState.is(Blocks.DETECTOR_RAIL);
    }
    public static boolean railIsRotating(RailShape shape) {
        return shape.equals(RailShape.NORTH_EAST) || shape.equals(RailShape.NORTH_WEST) || shape.equals(RailShape.SOUTH_EAST) || shape.equals(RailShape.SOUTH_WEST);
    }
    public RailShape anyRailShape(BlockState blockState, BlockPos blockPos) {
        if (isRail(blockState)) return ((BaseRailBlock)blockState.getBlock())
                .getRailDirection(blockState, this.level, blockPos, this);
        else return null;
    }

    public static ArrayList<BlockPos> nearsBlockPos(BlockPos blockPos) {
        ArrayList<BlockPos> tmp = new ArrayList<>();

        tmp.add(blockPos.relative(Direction.UP));
        tmp.add(blockPos.relative(Direction.EAST));
        tmp.add(blockPos.relative(Direction.NORTH));
        tmp.add(blockPos.relative(Direction.WEST));
        tmp.add(blockPos.relative(Direction.SOUTH));
        tmp.add(blockPos.relative(Direction.DOWN));

        return tmp;
    }

    public void cartSound(float distance, SoundEvent soundEvent) {
        Player player = level.getNearestPlayer(this, distance);

        if (player != null) level.playSound(player, new BlockPos(position()),
                soundEvent, SoundSource.BLOCKS, 1.0F - distanceTo(player) / distance + 0.1F, 1.0F);
    }

    public enum Type {
        WAGON,
        LOCOMOTIVE
    }
}
