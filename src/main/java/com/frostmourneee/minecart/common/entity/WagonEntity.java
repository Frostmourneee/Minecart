package com.frostmourneee.minecart.common.entity;

import com.frostmourneee.minecart.core.init.ccItemInit;
import com.frostmourneee.minecart.core.init.ccSoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


public class WagonEntity extends AbstractCart {

    public WagonEntity(EntityType entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void tick() {
        super.tick();

        //===================== MY CODE STARTS ======================
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult ret = super.interact(player, hand);
        if (ret.consumesAction()) return ret;
        ItemStack itemStack = player.getItemInHand(hand);

        if (itemStack.getItem().equals(ccItemInit.DEBUG_ITEM.get())) {
            setDebugMode(!debugMode); //TODO remove
        }

        if (itemStack.getItem().equals(ccItemInit.CLAMP.get()) && isClamping) {
            return player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } //Second click via clamp item when cart is clamping works just as clicking with no clamp item

        if (canBeClamped(itemStack)) {
            if (hasFrontCart()) {
                cartSound(ccSoundInit.CART_UNCLAMP.get());
                setIsClamping(false);

                if (!level.isClientSide) {
                    entityData.set(DATA_NUMBER_BEFORE_EXIT, 1);
                    entityData.set(DATA_TRAIN_LENGTH, cartsBehind() + 1);
                    entityData.set(DATA_FIRST_CART_ID, getId());

                    AbstractCart tmp = this;
                    while (tmp.hasBackCart()) {
                        tmp = tmp.backCart;
                        tmp.getEntityData().set(DATA_NUMBER_BEFORE_EXIT, tmp.frontCart.getEntityData().get(DATA_NUMBER_BEFORE_EXIT) + 1);
                        tmp.getEntityData().set(DATA_TRAIN_LENGTH, cartsBehind() + 1);
                        tmp.getEntityData().set(DATA_FIRST_CART_ID, getId());
                    }

                    tmp = this;
                    while (tmp.hasFrontCart()) {
                        tmp = tmp.frontCart;
                        tmp.getEntityData().set(DATA_TRAIN_LENGTH, cartsAhead());
                    }
                }

                frontCart.resetBack();
                resetFront();
            } else {
                tryingToClamp();
            }
        }

        if (player.isSecondaryUseActive() && !itemStack.getItem().equals(ccItemInit.CLAMP.get()) && !itemStack.getItem().equals(ccItemInit.DEBUG_ITEM.get())) {
            return InteractionResult.PASS;
        } else if (isVehicle()) {
            return InteractionResult.PASS;
        } else if (!level.isClientSide && !itemStack.getItem().equals(ccItemInit.CLAMP.get()) && !itemStack.getItem().equals(ccItemInit.DEBUG_ITEM.get())) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    public void collisionProcessing() {
        super.collisionProcessing();

        spawnAfterCartLeaving();
    }

    //////////////////////////////////////TECHNICAL METHODS//////////////////////////
    @Override
    public float getMaxCartSpeedOnRail() {
        return 0.3f;
    } //TODO change
    @Override
    public void setDeltaMovement(@NotNull Vec3 vec) {
        if (hasFrontCart()) {
            deltaMovement = frontCart.deltaMovement;
            return;
        }

        super.setDeltaMovement(vec);
    }

    public void spawnAfterCartLeaving() {
        if (hasFrontCart() && getPassengers().isEmpty()) {
            AABB miniBox;
            miniBox = getBoundingBox().deflate(0.1D, 0.0F, 0.1D);

            ArrayList<Player> previousPassenger = (ArrayList<Player>) level.getEntitiesOfClass(Player.class, miniBox);
            if (!previousPassenger.isEmpty()) previousPassenger.get(0).setPos(previousPassenger.get(0).position().add(0.0D, 1.0D, 0.0D));
        }
    }

    public boolean canBeClamped(ItemStack itemStack) {
        BlockPos blockPos = new BlockPos(position());
        BlockState blockState = level.getBlockState(blockPos);

        return itemStack.getItem().equals(ccItemInit.CLAMP.get())
                && blockState.is(BlockTags.RAILS)
                && !anyRailShape(blockState, blockPos).isAscending();
    }

    public void activateMinecart(int int1, int int2, int int3, boolean bool1) {
        if (bool1) {
            if (isVehicle()) {
                ejectPassengers();
            }

            if (getHurtTime() == 0) {
                setHurtDir(-getHurtDir());
                setHurtTime(10);
                setDamage(50.0F);
                markHurt();
            }
        }
    }

    public AbstractMinecart.@NotNull Type getMinecartType() {
        return AbstractMinecart.Type.RIDEABLE;
    }
    @Override
    public AbstractCart.Type getCartType() {
        return Type.WAGON;
    }
    @Override
    public ItemStack getCartItem() {
        return new ItemStack(ccItemInit.WAGON_ITEM.get());
    }
}