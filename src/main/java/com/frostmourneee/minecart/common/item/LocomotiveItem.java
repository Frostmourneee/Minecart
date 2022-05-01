package com.frostmourneee.minecart.common.item;

import com.frostmourneee.minecart.common.entity.AbstractCart;
import com.frostmourneee.minecart.common.entity.LocomotiveEntity;
import com.frostmourneee.minecart.common.entity.WagonEntity;
import com.frostmourneee.minecart.core.init.ccEntityInit;
import com.frostmourneee.minecart.core.init.ccSoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import static com.frostmourneee.minecart.common.entity.AbstractCart.*;


public class LocomotiveItem extends Item {

    private static final DispenseItemBehavior DISPENSE_ITEM_BEHAVIOR = new DefaultDispenseItemBehavior() {
        private final DefaultDispenseItemBehavior defaultDispenseItemBehavior = new DefaultDispenseItemBehavior();

        public ItemStack execute(BlockSource blockSource, ItemStack itemStack) {
            Direction direction = blockSource.getBlockState().getValue(DispenserBlock.FACING);
            Level level = blockSource.getLevel();
            BlockPos blockpos = blockSource.getPos().relative(direction);
            BlockState blockstate = level.getBlockState(blockpos);

            if (!blockstate.is(BlockTags.RAILS)) {
                if (!blockstate.isAir() || !level.getBlockState(blockpos.below()).is(BlockTags.RAILS)) {
                    return defaultDispenseItemBehavior.dispense(blockSource, itemStack);
                }
            }

            itemStack.shrink(1);
            return itemStack;
        }

        protected void playSound(BlockSource blockSource) {
            blockSource.getLevel().levelEvent(1000, blockSource.getPos(), 0);
        }
    };

    public LocomotiveItem(Item.Properties itemProperties) {
        super(itemProperties);

        DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
    }

    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);

        if (!blockstate.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        } else {
            ItemStack itemstack = context.getItemInHand();
            BaseRailBlock rail = (BaseRailBlock) blockstate.getBlock();

            if (!railIsRotating(rail.getRailDirection(blockstate, level, blockpos, null))) {
                level.playSound(player, blockpos, ccSoundInit.CART_PUT.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);

                if (!level.isClientSide) {
                    LocomotiveEntity locomotive = new LocomotiveEntity(ccEntityInit.LOCOMOTIVE_ENTITY.get(), level);
                    locomotive.setPos(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D);

                    if (rail.getRailDirection(blockstate, level, blockpos, null).equals(RailShape.NORTH_SOUTH)) {
                        if (context.getPlayer().getZ() > locomotive.getZ()) {
                            locomotive.setYRot(180.0F);
                        } else {
                            locomotive.setYRot(0.0F);
                        }
                    } else {
                        if (context.getPlayer().getX() > locomotive.getX()) {
                            locomotive.setYRot(90.0F);
                        } else {
                            locomotive.setYRot(270.0F);
                        }
                    }

                    level.addFreshEntity(locomotive);
                    level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }

                itemstack.shrink(1);
            } else return InteractionResult.FAIL;

            return InteractionResult.sidedSuccess(level.isClientSide);

            /*ItemStack itemstack = context.getItemInHand();

            BaseRailBlock rail = (BaseRailBlock) blockstate.getBlock();
            if (!AbstractCart.railIsRotating(rail.getRailDirection(blockstate, level, blockpos, null))) {
                level.playSound(player, blockpos, ccSoundInit.CART_PUT.get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            if (!level.isClientSide) {
                LocomotiveEntity locomotiveEntity = new LocomotiveEntity(ccEntityInit.LOCOMOTIVE_ENTITY.get(), level);

                if (rail.getRailDirection(blockstate, level, blockpos, null).equals(RailShape.NORTH_SOUTH)) {
                    locomotiveEntity.setPos(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D);
                    if (context.getPlayer().getZ() > locomotiveEntity.getZ()) locomotiveEntity.setYRot(180.0F);
                    else locomotiveEntity.setYRot(0.0F);
                    locomotiveEntity.setBoundingBox(new AABB(-0.6D, -0.35D, -0.4D, 0.6D, 0.35D, 0.4D));
                    level.addFreshEntity(locomotiveEntity);
                    level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }
                else if (rail.getRailDirection(blockstate, level, blockpos, null).equals(RailShape.EAST_WEST)){
                    locomotiveEntity.setPos(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D);
                    if (context.getPlayer().getX() > locomotiveEntity.getX()) locomotiveEntity.setYRot(90.0F);
                    else locomotiveEntity.setYRot(270.0F);
                    locomotiveEntity.setBoundingBox(new AABB(-0.6D, -0.35D, -0.4D, 0.6D, 0.35D, 0.4D));
                    level.addFreshEntity(locomotiveEntity);
                    level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }
            }

            itemstack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);*/
        }
    }

}