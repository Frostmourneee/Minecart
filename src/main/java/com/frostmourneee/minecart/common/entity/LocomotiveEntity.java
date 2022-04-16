package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.debugging_minecart.core.init.dmItemInit;
import com.frostmourneee.minecart.core.ccUtil;
import com.frostmourneee.minecart.core.init.ccItemInit;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.level.block.HopperBlock.FACING;

public class LocomotiveEntity extends AbstractMinecart {

    public LocomotiveEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }

    private static final EntityDataAccessor<Boolean> DATA_ID_FUEL = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> DATA_BACKCART_EXISTS = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> DATA_HORIZONTAL_ROTATION_ANGLE = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> DATA_VERTICAL_ROTATION_ANGLE = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.FLOAT);

    public static final EntityDataAccessor<Boolean> DATA_DEBUG_MODE = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.BOOLEAN); //TODO remove

    public float horAngle; //CLIENT SIDE ONLY
    public float vertAngle; //CLIENTSIDE ONLY
    public BlockPos posOfBackCart;
    public boolean shouldHasBackCart = false;

    private int fuel;
    public double xPush;
    public double zPush;

    public boolean debugMode = false; //TODO remove

    public static Ingredient INGREDIENT = Ingredient.of(Items.APPLE, Items.CHARCOAL);

    public WagonEntity backCart = null;

    @Override
    public void tick() {
        this.vanillaTickContent();

        //My code starts
        Vec3 delta = this.getDeltaMovement();

        this.stopBeforeTurnWhenSlow(delta);
        this.CartsRestoreAfterRestart();
        this.collisionProcessing();
        this.fuelControl();
        this.smokeAnim();
        this.addFuelByHopper(72); //TODO change
    }

    @Override
    protected void moveAlongTrack(BlockPos blockPos, BlockState blockState) {
        super.moveAlongTrack(blockPos, blockState);

        Vec3 vec3 = this.getDeltaMovement();
        double d2 = vec3.horizontalDistanceSqr();
        double d3 = this.xPush * this.xPush + this.zPush * this.zPush;

        if (d3 > 1.0E-4D && d2 > 0.001D) {
            double d4 = Math.sqrt(d2);
            double d5 = Math.sqrt(d3);
            this.xPush = vec3.x / d4 * d5;
            this.zPush = vec3.z / d4 * d5;
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand interactionHand) {
        InteractionResult ret = super.interact(player, interactionHand);
        if (ret.consumesAction()) return ret;
        ItemStack itemstack = player.getItemInHand(interactionHand);

        if (INGREDIENT.test(itemstack) && this.fuel + 3600 <= 32000) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            this.fuel += 72; //TODO change
        }

        if (this.fuel > 0) {
            this.xPush = this.getX() - player.getX();
            this.zPush = this.getZ() - player.getZ();
        }

        if (itemstack.getItem().equals(dmItemInit.DebugItem.get())) {
            if (this.debugMode) {
                this.debugMode = false;
                this.entityData.set(DATA_DEBUG_MODE, false);
            } else {
                this.debugMode = true;
                this.entityData.set(DATA_DEBUG_MODE, true);
            }
        } //TODO remove debug

        return InteractionResult.sidedSuccess(this.level.isClientSide);
    }

    @Override
    protected void comeOffTrack() {
        this.clampInfoReset();

        this.remove(RemovalReason.KILLED);
        if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(ccItemInit.LOCOMOTIVE_ITEM.get());
        }
    }

    public void addFuelByHopper(int plusFuel) {
        BlockPos thisBlockPos = new BlockPos(this.position());

        for (BlockPos blockPos : ccUtil.nearsBlockPos(thisBlockPos)) {
            BlockState blockState = this.level.getBlockState(blockPos);

            if (blockState.is(Blocks.HOPPER)) {
                for (int i = 0; i < 5; i++) {
                    ArrayList<LocomotiveEntity> rangeNewMinecartEntities =
                            (ArrayList<LocomotiveEntity>) level.getEntitiesOfClass(LocomotiveEntity.class, new AABB(blockPos).inflate(2));

                    HopperBlockEntity hopperBlockEntity = null;
                    Vec3 vecToLocomotive = new Vec3(rangeNewMinecartEntities.get(0).getX() - (blockPos.getX() + 0.5D), rangeNewMinecartEntities.get(0).getY() - blockPos.getY(), rangeNewMinecartEntities.get(0).getZ() - (blockPos.getZ() + 0.5D));
                    System.out.println(vecToLocomotive);
                    System.out.println(ccUtil.vecToDirection(vecToLocomotive));
                    if (ccUtil.vecToDirection(vecToLocomotive) != null) if (ccUtil.anyRail(this.level.getBlockState(blockPos.relative(ccUtil.vecToDirection(vecToLocomotive)))))
                        hopperBlockEntity = HopperRotation(ccUtil.vecToDirection(vecToLocomotive), blockPos);

                    if (hopperBlockEntity != null && hasFuelItem(hopperBlockEntity, i)) {
                        if (this.fuel <= plusFuel + 100) {
                            this.fuel += plusFuel;
                            hopperBlockEntity.getItem(i).shrink(1);

                            switch (this.getDirection()) {
                                case NORTH:
                                    this.xPush = 0.0D;
                                    this.zPush = -1.0D;
                                    break;
                                case EAST:
                                    this.xPush = 1.0D;
                                    this.zPush = 0.0D;
                                    break;
                                case SOUTH:
                                    this.xPush = 0.0D;
                                    this.zPush = 1.0D;
                                    break;
                                case WEST:
                                    this.xPush = -1.0D;
                                    this.zPush = 0.0D;
                                    break;
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
    public HopperBlockEntity HopperRotation(Direction direction, BlockPos blockPos) {
        if (this.level.getBlockState(blockPos).is(Blocks.HOPPER)) {
            HopperBlockEntity hopperBlockEntity = (HopperBlockEntity) level.getBlockEntity(blockPos);
            ArrayList<ItemStack>  itemsInHopper = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                itemsInHopper.add(hopperBlockEntity.getItem(i));
                hopperBlockEntity.setItem(i, new ItemStack(Items.AIR));
            }

            level.removeBlock(blockPos, true);
            level.setBlock(blockPos, Blocks.HOPPER.defaultBlockState().setValue(FACING, direction), 3);
            hopperBlockEntity = (HopperBlockEntity) level.getBlockEntity(blockPos);
            for (int i = 0; i < 5; i++) hopperBlockEntity.setItem(i, itemsInHopper.get(i));

            return hopperBlockEntity;
        }

        return null;
    }

    public void collisionProcessing() {
        AABB box;

        if (getCollisionHandler() != null) box = getCollisionHandler().getMinecartCollisionBox(this);
        else box = this.getBoundingBox().inflate(0.2F, 0.0D, 0.2F);

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
            for (Entity entity : this.level.getEntities(this, box)) {
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

    //////////////////////////////////////TECHNICAL METHODS//////////////////////////

    public void stopBeforeTurnWhenSlow(Vec3 delta) {
        if (this.level.getBlockState(this.getOnPos().above().relative(this.getDirection())).is(Blocks.RAIL)) {
            BlockPos blockPos = this.getOnPos().above().relative(this.getDirection());
            BlockState blockState = this.level.getBlockState(blockPos);

            RailShape shape = ccUtil.anyRailShape(blockState, blockPos, this);
            if (!this.getDeltaMovement().equals(Vec3.ZERO) && delta.length() < 1.0E-1 && ccUtil.railIsRotating(shape)) this.setDeltaMovement(Vec3.ZERO);
        }
    }
    public void fuelControl() {
        if (!this.level.isClientSide()) {
            if (this.fuel > 0) {
                --this.fuel;
            } else  {
                xPush = 0.0D;
                zPush = 0.0D;
            }

            this.setHasFuel(this.fuel > 0);
        }
    }
    public void smokeAnim() {
        if (this.hasFuel() && this.random.nextInt(4) == 0) {
            if (this.getDirection() == Direction.WEST)
                this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 0.19D, this.getY() + 1.3D, this.getZ() + 0.19D, 0.0D, 0.0D, 0.0D);
            if (this.getDirection() == Direction.EAST)
                this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() - 0.19D, this.getY() + 1.3D, this.getZ() - 0.19D, 0.0D, 0.0D, 0.0D);
            if (this.getDirection() == Direction.SOUTH)
                this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 0.19D, this.getY() + 1.3D, this.getZ() - 0.19D, 0.0D, 0.0D, 0.0D);
            if (this.getDirection() == Direction.NORTH)
                this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() - 0.19D, this.getY() + 1.3D, this.getZ() + 0.19D, 0.0D, 0.0D, 0.0D);
        }
    }
    public void CartsRestoreAfterRestart() {
        if (this.shouldHasBackCart && this.backCart == null) {
            ArrayList<WagonEntity> rangeBackWagon = (ArrayList<WagonEntity>) level.getEntitiesOfClass(WagonEntity.class, new AABB(this.posOfBackCart));
            if (!rangeBackWagon.isEmpty()) {
                this.backCart = rangeBackWagon.get(0);
                this.entityData.set(DATA_BACKCART_EXISTS, true);
                this.shouldHasBackCart = false;
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.entityData.get(DATA_BACKCART_EXISTS) && this.isAlive();
    }

    @Override
    protected double getMaxSpeed() {
        return (this.isInWater() ? 4.0D : 8.0D) / 20.0D;
    }
    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.3f;
    } //TODO change
    @Override
    protected void applyNaturalSlowdown() {
        double d0 = this.xPush * this.xPush + this.zPush * this.zPush;
        if (d0 > 1.0E-7D) {
            d0 = Math.sqrt(d0);
            this.xPush /= d0;
            this.zPush /= d0;
            Vec3 vec3 = this.getDeltaMovement().multiply(0.8D, 0.0D, 0.8D).add(this.xPush, 0.0D, this.zPush);

            if (this.isInWater()) {
                vec3 = vec3.scale(0.1D);
            }

            this.setDeltaMovement(vec3);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.98D, 0.0D, 0.98D));
        }

        super.applyNaturalSlowdown();
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
            p_38098_ = (double)j;
            if (railshape.isAscending()) {
                p_38098_ = (double)(j + 1);
            }

            Pair<Vec3i, Vec3i> pair = exits(railshape);
            Vec3i vec3i = pair.getFirst();
            Vec3i vec3i1 = pair.getSecond();
            double d0 = (double)(vec3i1.getX() - vec3i.getX());
            double d1 = (double)(vec3i1.getZ() - vec3i.getZ());
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            d0 /= d2;
            d1 /= d2;
            p_38097_ += d0 * p_38100_;
            p_38099_ += d1 * p_38100_;
            if (vec3i.getY() != 0 && Mth.floor(p_38097_) - i == vec3i.getX() && Mth.floor(p_38099_) - k == vec3i.getZ()) {
                p_38098_ += (double)vec3i.getY();
            } else if (vec3i1.getY() != 0 && Mth.floor(p_38097_) - i == vec3i1.getX() && Mth.floor(p_38099_) - k == vec3i1.getZ()) {
                p_38098_ += (double)vec3i1.getY();
            }

            return this.getPos(p_38097_, p_38098_, p_38099_);
        } else {
            return null;
        }
    }

    @Override
    public void discard() { //in creative
        this.clampInfoReset();
        this.remove(Entity.RemovalReason.DISCARDED);
    }
    @Override
    public void destroy(DamageSource damageSource) {
        this.clampInfoReset();
        this.remove(RemovalReason.KILLED);

        if (!damageSource.isExplosion() && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.spawnAtLocation(ccItemInit.LOCOMOTIVE_ITEM.get());
        }
    }
    public void clampInfoReset() {
        if (this.backCart != null) {
            this.backCart.isClamped = false; //server
            this.backCart.getEntityData().set(WagonEntity.DATA_CLAMP_OR_NOT, false);
            this.backCart.isFirst = false;
            this.backCart.locomotive = null;
            this.backCart.getEntityData().set(WagonEntity.DATA_LOCOMOTIVE_EXISTS, false);
            this.backCart.setDeltaMovement(this.getDeltaMovement());
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ccItemInit.LOCOMOTIVE_ITEM.get());
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_BACKCART_EXISTS, false);
        this.entityData.define(DATA_ID_FUEL, false);
        this.entityData.define(DATA_DEBUG_MODE, false);
        this.entityData.define(DATA_HORIZONTAL_ROTATION_ANGLE, 0.0F);
        this.entityData.define(DATA_VERTICAL_ROTATION_ANGLE, 0.0F);
    } //TODO remove debug
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putDouble("PushX", this.xPush);
        compoundTag.putDouble("PushZ", this.zPush);
        compoundTag.putShort("Fuel", (short)this.fuel);
        compoundTag.putBoolean("Debug", this.debugMode);
        compoundTag.putFloat("HAngle", this.entityData.get(DATA_HORIZONTAL_ROTATION_ANGLE));
        compoundTag.putFloat("VAngle", this.entityData.get(DATA_VERTICAL_ROTATION_ANGLE));

        if (this.entityData.get(DATA_BACKCART_EXISTS) && this.backCart != null) {
            compoundTag.putBoolean("BackCartExists", true);

            int[] wagonPos = new int[3];
            wagonPos[0] = this.backCart.getBlockX();
            wagonPos[1] = this.backCart.getBlockY();
            wagonPos[2] = this.backCart.getBlockZ();
            compoundTag.putIntArray("BackCartPos", wagonPos);
        }
        else compoundTag.putBoolean("BackCartExists", false);
    } //TODO remove debug
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        this.xPush = compoundTag.getDouble("PushX");
        this.zPush = compoundTag.getDouble("PushZ");
        this.fuel = compoundTag.getShort("Fuel");
        this.debugMode = compoundTag.getBoolean("Debug");
        this.entityData.set(DATA_DEBUG_MODE, this.debugMode);
        this.horAngle = compoundTag.getFloat("HAngle");
        this.entityData.set(DATA_HORIZONTAL_ROTATION_ANGLE, this.horAngle);
        this.vertAngle = compoundTag.getFloat("VAngle");
        this.entityData.set(DATA_VERTICAL_ROTATION_ANGLE, this.vertAngle);

        if (compoundTag.getBoolean("BackCartExists")) {
            this.shouldHasBackCart = true;
            int[] wagonPos;
            wagonPos = compoundTag.getIntArray("BackCartPos");
            this.posOfBackCart = new BlockPos(wagonPos[0], wagonPos[1], wagonPos[2]);
        }
    } //TODO remove debug

    public boolean hasFuel() {
        return this.entityData.get(DATA_ID_FUEL);
    }
    protected void setHasFuel(boolean bool) {
        this.entityData.set(DATA_ID_FUEL, bool);
    }

    public boolean hasFuelItem(HopperBlockEntity hopperBlockEntity, int counter) {
        for (int j = 0; j < INGREDIENT.getItems().length; j++) {
            if (hopperBlockEntity.getItem(counter).is(INGREDIENT.getItems()[j].getItem())) {
                return true;
            }
        }
        return false;
    }

    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.FURNACE;
    }

    public AbstractCart.Type getCartType() {
        return AbstractCart.Type.LOCOMOTIVE;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH).setValue(FurnaceBlock.LIT, this.hasFuel());
    }
    public void vanillaTickContent() {
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

    public void entityPushingBySelf(Entity entity) {
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

            d0 = d0 * d3;
            d1 = d1 * d3;
            d0 = d0 * (double) 0.05F;
            d1 = d1 * (double) 0.05F;

            if (!entity.isVehicle()) {
                entity.push(-d0, 0.0D, -d1);
            }
            if (!this.isVehicle() && this.backCart == null) {
                this.push(d0, 0.0D, d1);
            }
        }
    }
    public void selfPushingByEntity(Entity entity) {
        double d0 = this.getX() - entity.getX();
        double d1 = this.getZ() - entity.getZ();
        double d2 = Mth.absMax(d0, d1);
        if (d2 >= (double) 0.01F) {
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

            if (!ccUtil.zeroDeltaMovementBigIndent(this) && ccUtil.zeroDeltaMovementBigIndent((AbstractMinecart) entity)) {
                if (this.backCart != null) {
                    this.backCart.isClamped = false;
                    this.backCart.getEntityData().set(WagonEntity.DATA_CLAMP_OR_NOT, false);
                    this.backCart.isFirst = false;
                    this.backCart.locomotive = null;
                    this.backCart.setDeltaMovement(this.getDeltaMovement());
                }

                this.remove(RemovalReason.KILLED);
                if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                    this.spawnAtLocation(ccItemInit.LOCOMOTIVE_ITEM.get());
                }
            }

            if (!entity.isVehicle()) {
                if ((entity instanceof WagonEntity && ((WagonEntity) entity).backCart == null) ||
                        (entity instanceof LocomotiveEntity && ((LocomotiveEntity) entity).backCart == null)) {
                    entity.push(-d0, 0.0D, -d1);
                } else if (!(entity instanceof WagonEntity) && !(entity instanceof LocomotiveEntity)) {
                    entity.push(-d0, 0.0D, -d1);
                }
            }
        }
    }
}