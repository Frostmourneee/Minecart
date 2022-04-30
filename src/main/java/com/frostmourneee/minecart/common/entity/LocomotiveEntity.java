package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.debugging_minecart.core.init.dmItemInit;
import com.frostmourneee.minecart.ccUtil;
import com.frostmourneee.minecart.core.init.ccItemInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;

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

    @Override
    public void tick() {
        super.tick();

        //My code starts
        Vec3 delta = getDeltaMovement();

        stopBeforeTurnWhenSlow(delta);
        fuelControl();
        smokeAnim();
        addFuelByHopper(72); //TODO change
    }

    @Override
    protected void moveAlongTrack(BlockPos blockPos, BlockState blockState) {
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
    public InteractionResult interact(Player player, InteractionHand interactionHand) {
        InteractionResult ret = super.interact(player, interactionHand);
        if (ret.consumesAction()) return ret;
        ItemStack itemstack = player.getItemInHand(interactionHand);

        if (INGREDIENT.test(itemstack) && fuel + 3600 <= 32000) {
            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
            fuel += 3600; //TODO change
        }

        if (fuel > 0) {
            xPush = getX() - player.getX();
            zPush = getZ() - player.getZ();
        }

        if (itemstack.getItem().equals(dmItemInit.DebugItem.get())) {
            if (debugMode) {
                debugMode = false;
                entityData.set(DATA_DEBUG_MODE, false);
            } else {
                debugMode = true;
                entityData.set(DATA_DEBUG_MODE, true);
            }
        } //TODO remove debug

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

                    if (ccUtil.vecToDirection(vecToLocomotive) != null) if (isRail(level.getBlockState(blockPos.relative(ccUtil.vecToDirection(vecToLocomotive)))))
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
    public boolean canBeCollidedWith() {
        return entityData.get(DATA_BACKCART_EXISTS) && isAlive();
    }

    @Override
    public void setDeltaMovement(Vec3 vec) {
        deltaMovement = vec;

        if (deltaMovement.length() < 1.0E-3) deltaMovement = Vec3.ZERO;
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
    protected void applyNaturalSlowdown() {
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
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);

        compoundTag.putDouble("PushX", xPush);
        compoundTag.putDouble("PushZ", zPush);
        compoundTag.putShort("Fuel", (short)fuel);
    } //TODO remove debug
    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);

        xPush = compoundTag.getDouble("PushX");
        zPush = compoundTag.getDouble("PushZ");
        fuel = compoundTag.getShort("Fuel");
    } //TODO remove debug

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

    public AbstractCart.Type getCartType() {
        return AbstractCart.Type.LOCOMOTIVE;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, Direction.NORTH).setValue(FurnaceBlock.LIT, hasFuel());
    }
}