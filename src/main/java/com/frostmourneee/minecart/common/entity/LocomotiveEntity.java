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

public class LocomotiveEntity extends AbstractCart {

    public LocomotiveEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }

    public static final EntityDataAccessor<Boolean> DATA_ID_FUEL = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.BOOLEAN);

    private int fuel;
    public double xPush;
    public double zPush;

    public static Ingredient INGREDIENT = Ingredient.of(Items.APPLE, Items.CHARCOAL);

    @Override
    public void tick() {
        super.tick();

        //My code starts
        Vec3 delta = this.getDeltaMovement();

        this.stopBeforeTurnWhenSlow(delta);
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
            switch (this.getDirection()) {
                case EAST -> this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() - 0.19D, this.getY() + 1.3D, this.getZ() - 0.19D, 0.0D, 0.0D, 0.0D);
                case NORTH -> this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() - 0.19D, this.getY() + 1.3D, this.getZ() + 0.19D, 0.0D, 0.0D, 0.0D);
                case WEST -> this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 0.19D, this.getY() + 1.3D, this.getZ() + 0.19D, 0.0D, 0.0D, 0.0D);
                case SOUTH -> this.level.addParticle(ParticleTypes.LARGE_SMOKE, this.getX() + 0.19D, this.getY() + 1.3D, this.getZ() - 0.19D, 0.0D, 0.0D, 0.0D);
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
    protected void defineSynchedData() {
        super.defineSynchedData();

        this.entityData.define(DATA_ID_FUEL, false);
    }
    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putDouble("PushX", this.xPush);
        compoundTag.putDouble("PushZ", this.zPush);
        compoundTag.putShort("Fuel", (short)this.fuel);
    } //TODO remove debug
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        this.xPush = compoundTag.getDouble("PushX");
        this.zPush = compoundTag.getDouble("PushZ");
        this.fuel = compoundTag.getShort("Fuel");
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
}