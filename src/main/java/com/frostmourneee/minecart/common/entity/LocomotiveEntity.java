package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.minecart.Util.ccUtil;
import com.frostmourneee.minecart.core.init.ccItemInit;
import com.frostmourneee.minecart.core.init.ccSoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static com.frostmourneee.minecart.Util.ccUtil.*;
import static net.minecraft.world.level.block.HopperBlock.FACING;

public class LocomotiveEntity extends AbstractCart {

    public LocomotiveEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }

    public static final EntityDataAccessor<Boolean> DATA_ID_FUEL = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.BOOLEAN);

    private int fuel = 0;
    public double xPush = 0.0D;
    public double zPush = 0.0D;

    public static Ingredient INGREDIENT = Ingredient.of(Items.APPLE, Items.CHARCOAL);
    public static final int FUEL_ADD_BY_CLICK = 1800; //TODO change

    @Override
    public void tick() {
        super.tick();

        //My code starts

        stopBeforeTurnWhenSlow(deltaMovement);
        fuelControl();
        smokeAnim();
        addFuelByHopper(FUEL_ADD_BY_CLICK);
    }

    @Override
    protected void moveAlongTrack(@NotNull BlockPos blockPos, @NotNull BlockState blockState) {
        super.moveAlongTrack(blockPos, blockState);

        Vec3 vec3 = getDeltaMovement();
        double d2 = vec3.horizontalDistanceSqr();
        double d3 = xPush * xPush + zPush * zPush;

        if (d3 > 1.0E-4D && d2 > 0.001D) {
            double d4 = Math.sqrt(d2);
            double d5 = Math.sqrt(d3);
            xPush = vec3.x / d4 * d5;
            zPush = vec3.z / d4 * d5;
        }
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand interactionHand) {
        InteractionResult ret = super.interact(player, interactionHand);
        if (ret.consumesAction()) return ret;
        ItemStack itemstack = player.getItemInHand(interactionHand);

        if (INGREDIENT.test(itemstack) && fuel + FUEL_ADD_BY_CLICK <= 32000) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            if (fuel == 0 && zeroDeltaMovement()) cartSound(ccSoundInit.LOCOMOTIVE_START.get());
            fuel += FUEL_ADD_BY_CLICK;
        }

        if (fuel > 0) {
            xPush = hasBackCart() ? position().subtract(backCart.position()).x : getX() - player.getX();
            zPush = hasBackCart() ? position().subtract(backCart.position()).z : getZ() - player.getZ();
        }

        if (itemstack.getItem().equals(ccItemInit.DEBUG_ITEM.get())) {
            setDebugMode(!debugMode); //TODO remove
        }

        return itemstack.getItem().equals(ccItemInit.CLAMP.get()) ? InteractionResult.PASS : InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void addFuelByHopper(int plusFuel) {
        BlockPos thisBlockPos = new BlockPos(position());

        for (BlockPos blockPos : nearsBlockPos(thisBlockPos)) {
            BlockState blockState = level.getBlockState(blockPos);

            if (blockState.is(Blocks.HOPPER)) {
                for (int i = 0; i < 5; i++) {
                    ArrayList<LocomotiveEntity> rangeNewMinecartEntities =
                            (ArrayList<LocomotiveEntity>) level.getEntitiesOfClass(LocomotiveEntity.class, new AABB(blockPos).inflate(2));

                    HopperBlockEntity hopperBlockEntity = null;
                    Vec3 vecToLocomotive = new Vec3(rangeNewMinecartEntities.get(0).getX() - (blockPos.getX() + 0.5D), rangeNewMinecartEntities.get(0).getY() - blockPos.getY(), rangeNewMinecartEntities.get(0).getZ() - (blockPos.getZ() + 0.5D));

                    if (ccUtil.vecToDirection(vecToLocomotive) != null) if (level.getBlockState(blockPos.relative(ccUtil.vecToDirection(vecToLocomotive))).is(BlockTags.RAILS))
                        hopperBlockEntity = HopperRotation(ccUtil.vecToDirection(vecToLocomotive), blockPos);

                    if (hopperBlockEntity != null && hasFuelItem(hopperBlockEntity, i)) {
                        if (fuel <= plusFuel + 100) {
                            fuel += plusFuel;
                            hopperBlockEntity.getItem(i).shrink(1);

                            switch (getDirection()) {
                                case NORTH -> {
                                    xPush = 0.0D;
                                    zPush = -1.0D;
                                }
                                case EAST -> {
                                    xPush = 1.0D;
                                    zPush = 0.0D;
                                }
                                case SOUTH -> {
                                    xPush = 0.0D;
                                    zPush = 1.0D;
                                }
                                case WEST -> {
                                    xPush = -1.0D;
                                    zPush = 0.0D;
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
    public HopperBlockEntity HopperRotation(Direction direction, BlockPos blockPos) {
        if (level.getBlockState(blockPos).is(Blocks.HOPPER)) {
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

    //////////////////////////////////////TECHNICAL METHODS//////////////////////////

    public void stopBeforeTurnWhenSlow(Vec3 delta) {
        if (level.getBlockState(getOnPos().above().relative(getDirection())).is(Blocks.RAIL)) {
            BlockPos blockPos = getOnPos().above().relative(getDirection());
            BlockState blockState = level.getBlockState(blockPos);

            RailShape shape = anyRailShape(blockState, blockPos);
            if (!getDeltaMovement().equals(Vec3.ZERO) && delta.length() < 1.0E-1 && railIsRotating(shape)) setDeltaMovement(Vec3.ZERO);
        }
    }
    public void fuelControl() {
        if (!level.isClientSide()) {
            if (fuel > 0) {
                --fuel;
            } else  {
                xPush = 0.0D;
                zPush = 0.0D;
            }

            setHasFuel(fuel > 0);
        }
    }
    public void smokeAnim() {
        if (hasFuel() && random.nextInt(4) == 0) {
            switch (getDirection()) {
                case EAST -> level.addParticle(ParticleTypes.LARGE_SMOKE, getX() - 0.19D, getY() + 1.3D, getZ() - 0.19D, 0.0D, 0.0D, 0.0D);
                case NORTH -> level.addParticle(ParticleTypes.LARGE_SMOKE, getX() - 0.19D, getY() + 1.3D, getZ() + 0.19D, 0.0D, 0.0D, 0.0D);
                case WEST -> level.addParticle(ParticleTypes.LARGE_SMOKE, getX() + 0.19D, getY() + 1.3D, getZ() + 0.19D, 0.0D, 0.0D, 0.0D);
                case SOUTH -> level.addParticle(ParticleTypes.LARGE_SMOKE, getX() + 0.19D, getY() + 1.3D, getZ() - 0.19D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    @Override
    public void move(@NotNull MoverType pType, @NotNull Vec3 pPos) {
        if (this.noPhysics) {
            this.setPos(this.getX() + pPos.x, this.getY() + pPos.y, this.getZ() + pPos.z);
        } else {
            this.wasOnFire = this.isOnFire();
            if (pType == MoverType.PISTON) {
                pPos = this.limitPistonMovement(pPos);
                if (pPos.equals(Vec3.ZERO)) {
                    return;
                }
            }

            this.level.getProfiler().push("move");
            if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7D) {
                pPos = pPos.multiply(this.stuckSpeedMultiplier);
                this.stuckSpeedMultiplier = Vec3.ZERO;
                this.setDeltaMovement(Vec3.ZERO);
            }

            pPos = this.maybeBackOffFromEdge(pPos, pType);
            Vec3 vec3 = this.collide(pPos);
            if (vec3.lengthSqr() > 1.0E-7D) {
                this.setPos(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
            }

            this.level.getProfiler().pop();
            this.level.getProfiler().push("rest");
            this.horizontalCollision = !Mth.equal(pPos.x, vec3.x) || !Mth.equal(pPos.z, vec3.z);
            this.verticalCollision = pPos.y != vec3.y;
            if (this.horizontalCollision) {
                this.minorHorizontalCollision = this.isHorizontalCollisionMinor(vec3);
            } else {
                this.minorHorizontalCollision = false;
            }

            this.onGround = this.verticalCollision && pPos.y < 0.0D;
            BlockPos blockpos = this.getOnPos();
            BlockState blockstate = this.level.getBlockState(blockpos);
            this.checkFallDamage(vec3.y, this.onGround, blockstate, blockpos);
            if (this.isRemoved()) {
                this.level.getProfiler().pop();
            } else {
                Vec3 vec31 = this.getDeltaMovement();
                if (pPos.x != vec3.x && deltaMovement.horizontalDistanceSqr() < 0.2D) {
                    this.setDeltaMovement(0.0D, vec31.y, vec31.z);
                }
                if (pPos.z != vec3.z && deltaMovement.horizontalDistanceSqr() < 0.2D) {
                    this.setDeltaMovement(vec31.x, vec31.y, 0.0D);
                }

                Block block = blockstate.getBlock();
                if (pPos.y != vec3.y) {
                    block.updateEntityAfterFallOn(this.level, this);
                }

                if (this.onGround && !this.isSteppingCarefully()) {
                    block.stepOn(this.level, blockpos, blockstate, this);
                }

                Entity.MovementEmission entity$movementemission = this.getMovementEmission();
                if (entity$movementemission.emitsAnything() && !this.isPassenger()) {
                    double d0 = vec3.x;
                    double d1 = vec3.y;
                    double d2 = vec3.z;
                    this.flyDist = (float) ((double) this.flyDist + vec3.length() * 0.6D);
                    if (!blockstate.is(BlockTags.CLIMBABLE) && !blockstate.is(Blocks.POWDER_SNOW)) {
                        d1 = 0.0D;
                    }

                    this.walkDist += (float) vec3.horizontalDistance() * 0.6F;
                    this.moveDist += (float) Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 0.6F;
                    if (this.moveDist > this.nextStep && !blockstate.isAir()) {
                        this.nextStep = this.nextStep();
                        if (this.isInWater()) {
                            if (entity$movementemission.emitsSounds()) {
                                Entity entity = this.isVehicle() && this.getControllingPassenger() != null ? this.getControllingPassenger() : this;
                                float f = entity == this ? 0.35F : 0.4F;
                                Vec3 vec32 = entity.getDeltaMovement();
                                float f1 = Math.min(1.0F, (float)Math.sqrt(vec32.x * vec32.x * (double)0.2F + vec32.y * vec32.y + vec32.z * vec32.z * (double)0.2F) * f);
                                this.playSwimSound(f1);
                            }

                            if (entity$movementemission.emitsEvents()) {
                                this.gameEvent(GameEvent.SWIM);
                            }
                        } else {
                            if (entity$movementemission.emitsSounds()) {
                                this.playAmethystStepSound(blockstate);
                                this.playStepSound(blockpos, blockstate);
                            }

                            if (entity$movementemission.emitsEvents() && !blockstate.is(BlockTags.OCCLUDES_VIBRATION_SIGNALS)) {
                                this.gameEvent(GameEvent.STEP);
                            }
                        }
                    } else if (blockstate.isAir()) {
                        this.processFlappingMovement();
                    }
                }

                this.tryCheckInsideBlocks();
                float f2 = this.getBlockSpeedFactor();
                this.setDeltaMovement(this.getDeltaMovement().multiply((double) f2, 1.0D, (double) f2));
            }
            if (this.level.getBlockStatesIfLoaded(this.getBoundingBox().deflate(1.0E-6D)).noneMatch((p_20127_) -> p_20127_.is(BlockTags.FIRE) || p_20127_.is(Blocks.LAVA))) {
                if (this.getRemainingFireTicks() <= 0) {
                    this.setRemainingFireTicks(-this.getFireImmuneTicks());
                }

                if (this.wasOnFire && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                    this.playEntityOnFireExtinguishedSound();
                }
            }

            if (this.isOnFire() && (this.isInPowderSnow || this.isInWaterRainOrBubble())) {
                this.setRemainingFireTicks(-this.getFireImmuneTicks());
            }

            this.level.getProfiler().pop();
        }
    }

    @Override
    protected double getMaxSpeed() {
        return (isInWater() ? 4.0D : 8.0D) / 20.0D;
    }
    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.3f;
    } //TODO change
    @Override
    protected void applyNaturalSlowdown() { //STRANGE BUT WITHOUT THIS METHOD LOCOMOTIVE CAN'T START MOVING
        double d0 = xPush * xPush + zPush * zPush;
        if (d0 > 1.0E-7D) {
            d0 = Math.sqrt(d0);
            xPush /= d0;
            zPush /= d0;
            Vec3 vec3 = getDeltaMovement().multiply(0.8D, 0.0D, 0.8D).add(xPush, 0.0D, zPush);

            if (isInWater()) {
                vec3 = vec3.scale(0.1D);
            }

            setDeltaMovement(vec3);
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.98D, 0.0D, 0.98D));
        }

        super.applyNaturalSlowdown();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();

        entityData.define(DATA_ID_FUEL, false);
    }
    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putDouble("PushX", xPush);
        compoundTag.putDouble("PushZ", zPush);
        compoundTag.putShort("Fuel", (short)fuel);
    }
    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        xPush = compoundTag.getDouble("PushX");
        zPush = compoundTag.getDouble("PushZ");
        fuel = compoundTag.getShort("Fuel");
    }

    public boolean hasFuel() {
        return entityData.get(DATA_ID_FUEL);
    }
    protected void setHasFuel(boolean bool) {
        entityData.set(DATA_ID_FUEL, bool);
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

    @Override
    public AbstractCart.Type getCartType() {
        return AbstractCart.Type.LOCOMOTIVE;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH).setValue(FurnaceBlock.LIT, hasFuel());
    }

    @Override
    public ItemStack getCartItem() {
        return new ItemStack(ccItemInit.LOCOMOTIVE_ITEM.get());
    }
}