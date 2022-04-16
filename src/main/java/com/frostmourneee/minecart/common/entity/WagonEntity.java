package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.debugging_minecart.core.init.dmItemInit;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class WagonEntity extends AbstractCart {

    public WagonEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }

    public static final EntityDataAccessor<Boolean> DATA_CLAMP_OR_NOT = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_FRONTCART_EXISTS = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_LOCOMOTIVE_EXISTS = SynchedEntityData.defineId(WagonEntity.class, EntityDataSerializers.BOOLEAN);

    public BlockPos posOfFrontCart;
    public boolean shouldHasFrontCart = false;
    public BlockPos posOfLocomotive;
    public boolean shouldHasLocomotive = false;

    public boolean isClamped = false;
    public boolean isFirst = false;

    public WagonEntity frontCart = null;
    public LocomotiveEntity locomotive = null;

    @Override
    public void tick() {
        super.tick();

        this.cartsRestoreAfterRestart();

        if (this.isClamped) {
            clampProcessing();
        }
    }

    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand interactionHand) {
        InteractionResult ret = super.interact(player, interactionHand);
        if (ret.consumesAction()) return ret;
        ItemStack itemStack = player.getItemInHand(interactionHand);

        if (!level.isClientSide) {
            if (this.canBeClamped(player, itemStack)) {
                if (this.isClamped) {
                    clampInfoReset(true, false, true, true, true);
                } else {
                    tryingToClamp();
                }
            }

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

    /*@Override
    protected void comeOffTrack() {
        if (this.backCart != null) {
            this.backCart.isClamped = false;
            this.backCart.entityData.set(DATA_CLAMP_OR_NOT, false);
            this.backCart.frontCart = null;
            this.backCart.getEntityData().set(DATA_FRONTCART_EXISTS, false);
            this.backCart.setDeltaMovement(this.getDeltaMovement());
        }

        if (this.frontCart != null) {
            this.frontCart.getEntityData().set(DATA_BACKCART_EXISTS, false);
            this.frontCart.backCart = null;
        }
        if (this.locomotive != null) {
            this.locomotive.getEntityData().set(LocomotiveEntity.DATA_BACKCART_EXISTS, false);
            this.locomotive.backCart = null;
        }

        this.remove(RemovalReason.KILLED);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(ccItemInit.WAGON_ITEM.get());
        }
    }*/

    public void clampProcessing() {
        if (!this.isFirst && this.frontCart != null) {
            this.poseConfiguration(Direction.NORTH, this.frontCart, 0, 1);
            this.poseConfiguration(Direction.SOUTH, this.frontCart, 0, -1);
            this.poseConfiguration(Direction.EAST, this.frontCart, -1, 0);
            this.poseConfiguration(Direction.WEST, this.frontCart, 1, 0);
        }
        else if (this.locomotive != null) {
            this.poseConfiguration(Direction.NORTH, this.locomotive, 0, 1);
            this.poseConfiguration(Direction.SOUTH, this.locomotive, 0, -1);
            this.poseConfiguration(Direction.EAST, this.locomotive, -1, 0);
            this.poseConfiguration(Direction.WEST, this.locomotive, 1, 0);
        }
    }

    //////////////////////////////////////TECHNICAL METHODS//////////////////////////

    public<CartType extends AbstractMinecart> void poseConfiguration(Direction direction, CartType cart, int i1, int i3) {
        if (this.getDirection().equals(direction)) {
            if (this.getDirection().equals(cart.getDirection()) && ccUtil.bothUpOrDownOrForward(this, cart)) {
                if (ccUtil.goesFlat(this) && ccUtil.goesFlat(cart) && !ccUtil.isRotating(this)) {
                    if (this.recursiveLocomotive() != null && !ccUtil.isStopped(this.recursiveLocomotive())) this.setDeltaMovement(cart.deltaMovement);
                    if (this.recursiveLocomotive() == null && !ccUtil.isStopped(this.frontCart)) this.setDeltaMovement(cart.deltaMovement);
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
                    if (this.recursiveLocomotive() != null && !ccUtil.isStopped(this.recursiveLocomotive())) this.setDeltaMovement(-i1, 0.0D, -i3);
                    if (this.recursiveLocomotive() == null && !ccUtil.isStopped(this.frontCart)) this.setDeltaMovement(-i1, 0.0D, -i3);
                }
                else {
                    if (this.recursiveLocomotive() != null && !ccUtil.isStopped(this.recursiveLocomotive())) this.setDeltaMovement(-i1, 0.0D, -i3 * cart.deltaMovement.length());
                    if (this.recursiveLocomotive() == null && !ccUtil.isStopped(this.frontCart)) this.setDeltaMovement(-i1, 0.0D, -i3 * cart.deltaMovement.length());
                }
            }
        }
    }
    @Override
    public void setDeltaMovement(@NotNull Vec3 vec) {
        if (Math.abs(this.deltaMovement.horizontalDistance()) < 1.0E-2) {
            if (this.isClamped && Math.abs(vec.horizontalDistance()) < 51.0E-3) this.deltaMovement = Vec3.ZERO;
            else this.deltaMovement = vec;
        } else {
            if (this.isClamped && Math.abs(vec.horizontalDistance()) < 2.0E-2) this.deltaMovement = Vec3.ZERO;
            else this.deltaMovement = vec;
        }
    }
    @Override
    public void setPos(double p_20210_, double p_20211_, double p_20212_) {
        this.setPosRaw(p_20210_, p_20211_, p_20212_);
        this.setBoundingBox(this.makeBoundingBox());
    }
    @Override
    public Vec3 getPos(double p_38180_, double p_38181_, double p_38182_) { //Used in Renderer class
        int i = Mth.floor(p_38180_);
        int j = Mth.floor(p_38181_);
        int k = Mth.floor(p_38182_);
        if (this.level.getBlockState(new BlockPos(i, j + 1, k)).is(BlockTags.RAILS)) {
            ++j;
        } else if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockState blockstate = this.level.getBlockState(new BlockPos(i, j, k));
        if (BaseRailBlock.isRail(blockstate)) {
            RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, this.level, new BlockPos(i, j, k), this);
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
                d9 = p_38182_ - (double)k;
            } else if (d8 == 0.0D) {
                d9 = p_38180_ - (double)i;
            } else {
                double d10 = p_38180_ - d0;
                double d11 = p_38182_ - d2;
                d9 = (d10 * d6 + d11 * d8) * 2.0D;
            }

            p_38180_ = d0 + d6 * d9;
            p_38181_ = d1 + d7 * d9;
            p_38182_ = d2 + d8 * d9;
            if (d7 < 0.0D) {
                ++p_38181_;
            } else if (d7 > 0.0D) {
                p_38181_ += 0.5D;
            }

            return new Vec3(p_38180_, p_38181_, p_38182_);
        } else {
            return null;
        }
    }
    @Override
    public Vec3 getPosOffs(double p_38097_, double p_38098_, double p_38099_, double p_38100_) {
        int i = Mth.floor(p_38097_);
        int j = Mth.floor(p_38098_);
        int k = Mth.floor(p_38099_);
        if (this.level.getBlockState(new BlockPos(i, j + 1, k)).is(BlockTags.RAILS)) {
            ++j;
        } else if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
            --j;
        }

        BlockState blockstate = this.level.getBlockState(new BlockPos(i, j, k));
        if (BaseRailBlock.isRail(blockstate)) {
            RailShape railshape = ((BaseRailBlock)blockstate.getBlock()).getRailDirection(blockstate, this.level, new BlockPos(i, j, k), this);
            p_38098_ = j;
            if (railshape.isAscending()) {
                p_38098_ = j + 1;
            }

            Pair<Vec3i, Vec3i> pair = exits(railshape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i1 = pair.getSecond();
            double d0 = vec3i1.getX() - vec3i.getX();
            double d1 = vec3i1.getZ() - vec3i.getZ();
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            d0 /= d2;
            d1 /= d2;
            p_38097_ += d0 * p_38100_;
            p_38099_ += d1 * p_38100_;
            if (vec3i.getY() != 0 && Mth.floor(p_38097_) - i == vec3i.getX() && Mth.floor(p_38099_) - k == vec3i.getZ()) {
                p_38098_ += vec3i.getY();
            } else if (vec3i1.getY() != 0 && Mth.floor(p_38097_) - i == vec3i1.getX() && Mth.floor(p_38099_) - k == vec3i1.getZ()) {
                p_38098_ += vec3i1.getY();
            }

            return this.getPos(p_38097_, p_38098_, p_38099_);
        } else {
            return null;
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ccItemInit.WAGON_ITEM.get());
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.entityData.get(DATA_BACKCART_EXISTS) || this.entityData.get(DATA_CLAMP_OR_NOT) && this.isAlive();
    }

    public void spawnAfterCartLeaving() {
        if (this.isClamped && this.getPassengers().isEmpty()) {
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
    public void tryingToClamp() {
        ArrayList<LocomotiveEntity> frontLocomotiveEntities;
        frontLocomotiveEntities = this.frontOnRailNewCarts(new LocomotiveEntity(ccEntityInit.LOCOMOTIVE_ENTITY.get(), level), new BlockPos(this.position()));

        if (frontLocomotiveEntities.isEmpty()) {
            ArrayList<WagonEntity> frontWagonEntities;
            frontWagonEntities = this.frontOnRailNewCarts(this, new BlockPos(this.getX(), this.getY(), this.getZ()));

            frontWagonEntities.removeIf(wagonEntity -> wagonEntity.equals(this));

            if (!frontWagonEntities.isEmpty()) {
                this.wagonOrLoc(frontWagonEntities, this.frontCart);
            }
        } else {
            this.wagonOrLoc(frontLocomotiveEntities, this.locomotive);
        }
    }
    public <CartType extends AbstractMinecart> ArrayList<CartType> frontOnRailNewCarts(CartType cartType, BlockPos blockPos) {
        return (ArrayList<CartType>) level.
                getEntitiesOfClass(cartType.getClass(), new AABB(blockPos.relative(this.getDirection())).inflate(0.9D, 0.0D, 0.9D));
    }
    public<CartType extends AbstractMinecart> void wagonOrLoc(ArrayList<CartType> arrayList, CartType cart) {
        if (arrayList.get(0).getDirection().equals(this.getDirection())) {

            if (arrayList.get(0) instanceof WagonEntity) {
                this.frontCart = (WagonEntity) arrayList.get(0);
                cart = (CartType) this.frontCart;
                this.getEntityData().set(DATA_FRONTCART_EXISTS, true);
                this.frontCart.getEntityData().set(DATA_BACKCART_EXISTS, true);
                this.frontCart.backCart = this;
            } else {
                this.isFirst = true;
                this.locomotive = (LocomotiveEntity) arrayList.get(0);
                cart = (CartType) this.locomotive;
                this.getEntityData().set(DATA_LOCOMOTIVE_EXISTS, true);
                this.locomotive.getEntityData().set(LocomotiveEntity.DATA_BACKCART_EXISTS, true);
                this.locomotive.backCart = this;
            }

            this.isClamped = true;
            this.entityData.set(DATA_CLAMP_OR_NOT, true);

            if (this.getDirection().equals(Direction.WEST))
                this.setPos(cart.position().add(1.625D, 0.0D, 0.0D));
            if (this.getDirection().equals(Direction.EAST))
                this.setPos(cart.position().add(-1.625D, 0.0D, 0.0D));
            if (this.getDirection().equals(Direction.NORTH))
                this.setPos(cart.position().add(0.0D, 0.0D, 1.625D));
            if (this.getDirection().equals(Direction.SOUTH))
                this.setPos(cart.position().add(0.0D, 0.0D, -1.625D));
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public void discard() { //in creative
        //this.clampInfoReset(false, true, true, true, false);
        this.remove(Entity.RemovalReason.DISCARDED);
    }
    @Override
    public void destroy(@NotNull DamageSource damageSource) { //in survival
        //this.clampInfoReset(false, true, true, true, false);
        this.remove(Entity.RemovalReason.KILLED);

        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            ItemStack itemstack = new ItemStack(ccItemInit.WAGON_ITEM.get());
            if (this.hasCustomName()) {
                itemstack.setHoverName(this.getCustomName());
            }

            this.spawnAtLocation(itemstack);
        }
    }
    /*public void clampInfoReset(boolean selfInfo, boolean backCartInfo, boolean frontCartInfo, boolean locomotiveInfo, boolean unclampFromFrontOnly) {
        if (backCartInfo && this.backCart != null) {
            this.backCart.isClamped = false;
            this.backCart.entityData.set(DATA_CLAMP_OR_NOT, false);
            this.backCart.frontCart = null;
            this.backCart.setDeltaMovement(this.getDeltaMovement());
        }

        if (frontCartInfo && this.frontCart != null) {
            this.frontCart.getEntityData().set(DATA_BACKCART_EXISTS, false);
            this.frontCart.backCart = null;
        }

        if (locomotiveInfo && this.locomotive != null) {
            this.locomotive.getEntityData().set(LocomotiveEntity.DATA_BACKCART_EXISTS, false);
            this.locomotive.backCart = null;
        }

        if (selfInfo) {
            this.isFirst = false;
            this.isClamped = false;
            this.entityData.set(DATA_CLAMP_OR_NOT, false);

            this.frontCart = null;
            this.entityData.set(DATA_FRONTCART_EXISTS, false);
            this.locomotive = null;
            this.entityData.set(DATA_LOCOMOTIVE_EXISTS, false);

            if (!unclampFromFrontOnly) {
                this.backCart = null;
                this.entityData.set(DATA_BACKCART_EXISTS, false);
            }
        }
    }*/

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_FRONTCART_EXISTS, false);
        this.entityData.define(DATA_LOCOMOTIVE_EXISTS, false);
        this.entityData.define(DATA_BACKCART_EXISTS, false);
        this.entityData.define(DATA_CLAMP_OR_NOT, false);
        this.entityData.define(DATA_DEBUG_MODE, false);

        this.entityData.define(DATA_HORIZONTAL_ROTATION_ANGLE, 0.0F);
        this.entityData.define(DATA_VERTICAL_ROTATION_ANGLE, 0.0F);
    } //TODO remove debug
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putBoolean("Clamp", this.isClamped);
        compoundTag.putBoolean("First", this.isFirst);
        compoundTag.putBoolean("Debug", this.debugMode);
        compoundTag.putFloat("HAngle", this.entityData.get(DATA_HORIZONTAL_ROTATION_ANGLE));
        compoundTag.putFloat("VAngle", this.entityData.get(DATA_VERTICAL_ROTATION_ANGLE));

        this.saveNearCartData(this.backCart, compoundTag, "BackCartExists", DATA_BACKCART_EXISTS);
        this.saveNearCartData(this.frontCart, compoundTag, "FrontCartExists", DATA_FRONTCART_EXISTS);
        this.saveNearCartData(this.locomotive, compoundTag, "LocomotiveCartExists", DATA_LOCOMOTIVE_EXISTS);
    } //TODO remove debug
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        this.isClamped = compoundTag.getBoolean("Clamp");
        this.entityData.set(DATA_CLAMP_OR_NOT, this.isClamped);
        this.isFirst = compoundTag.getBoolean("First");
        this.debugMode = compoundTag.getBoolean("Debug");
        this.entityData.set(DATA_DEBUG_MODE, this.debugMode);
        this.horAngle = compoundTag.getFloat("HAngle");
        this.entityData.set(DATA_HORIZONTAL_ROTATION_ANGLE, this.horAngle);
        this.vertAngle = compoundTag.getFloat("VAngle");
        this.entityData.set(DATA_VERTICAL_ROTATION_ANGLE, this.vertAngle);

        if (compoundTag.getBoolean("BackCartExists")) {
            this.shouldHasBackCart = true;
            int[] cartPos;
            cartPos = compoundTag.getIntArray("BackCartExistsPos");
            this.posOfBackCart = new BlockPos(cartPos[0], cartPos[1], cartPos[2]);
        }

        if (compoundTag.getBoolean("FrontCartExists")) {
            this.shouldHasFrontCart = true;
            int[] cartPos;
            cartPos = compoundTag.getIntArray("FrontCartExistsPos");
            this.posOfFrontCart = new BlockPos(cartPos[0], cartPos[1], cartPos[2]);
        }

        if (compoundTag.getBoolean("LocomotiveCartExists")) {
            this.shouldHasLocomotive = true;
            int[] cartPos;
            cartPos = compoundTag.getIntArray("LocomotiveCartExistsPos");
            this.posOfLocomotive = new BlockPos(cartPos[0], cartPos[1], cartPos[2]);
        }
    } //TODO remove debug
    public<CartType extends AbstractMinecart> void saveNearCartData(CartType cart, CompoundTag compoundTag, String name, EntityDataAccessor<Boolean> accessor) {
        if (this.entityData.get(accessor) && cart != null) {
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
    public void cartsRestoreAfterRestart() {
        if (this.shouldHasBackCart && this.backCart == null) {
            ArrayList<WagonEntity> rangeCart = (ArrayList<WagonEntity>) level.getEntitiesOfClass(WagonEntity.class, new AABB(this.posOfBackCart));
            if (!rangeCart.isEmpty()) {
                this.backCart = rangeCart.get(0);
                this.entityData.set(DATA_BACKCART_EXISTS, true);
                this.shouldHasBackCart = false;
            }
        }

        if (this.shouldHasFrontCart && this.frontCart == null) {
            ArrayList<WagonEntity> rangeCart = (ArrayList<WagonEntity>) level.getEntitiesOfClass(WagonEntity.class, new AABB(this.posOfFrontCart));
            if (!rangeCart.isEmpty()) {
                this.frontCart = rangeCart.get(0);
                this.entityData.set(DATA_FRONTCART_EXISTS, true);
                this.shouldHasFrontCart = false;
            }
        }

        if (this.shouldHasLocomotive && this.locomotive == null) {
            ArrayList<LocomotiveEntity> rangeCart = (ArrayList<LocomotiveEntity>) level.getEntitiesOfClass(LocomotiveEntity.class, new AABB(this.posOfLocomotive));
            if (!rangeCart.isEmpty()) {
                this.locomotive = rangeCart.get(0);
                this.entityData.set(DATA_LOCOMOTIVE_EXISTS, true);
                this.shouldHasLocomotive = false;
            }
        }
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

    public LocomotiveEntity recursiveLocomotive() {
        if (this.frontCart == null && this.locomotive == null) return null;
        else if (this.locomotive != null) return this.locomotive;
        else return this.frontCart.recursiveLocomotive();
    }
    /*public WagonEntity recursiveBackCart() {
        if (this.backCart == null && !this.isClamped) return  null;
        else if (this.backCart == null) return this;
        else if (this.backCart.backCart == null) return this.backCart;
        else return this.backCart.recursiveBackCart();
    }*/
    /*public int wagonsLength(int tmp) {
        int counter = tmp;

        if (this.backCart != null) {
            if (counter == 1) {
                return this.recursiveBackCart().wagonsLength(1);
            } else {
                if (this.frontCart == null) return counter;
                else {
                    counter++;
                    return this.frontCart.wagonsLength(counter);
                }
            }
        } else {
            if (this.frontCart == null) return counter;
            else {
                counter++;
                return this.frontCart.wagonsLength(counter);
            }
        }
    } //Should be provided with 1 as an argument to count proper length, SERVER ONLY
    public int trainLength() {
        if (this.recursiveLocomotive() != null) return this.wagonsLength(1) + 1;
        else return wagonsLength(1);
    }*/
}