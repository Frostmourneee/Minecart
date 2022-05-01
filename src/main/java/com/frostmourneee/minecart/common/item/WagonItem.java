package com.frostmourneee.minecart.common.item;

import com.frostmourneee.minecart.common.entity.WagonEntity;
import com.frostmourneee.minecart.core.init.ccEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

public class WagonItem extends Item {
    public WagonItem(Properties properties) {
        super(properties);
    }

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

    public WagonItem(AbstractMinecart.Type p_42938_, Item.Properties p_42939_) {
        super(p_42939_);

        DispenserBlock.registerBehavior(this, DISPENSE_ITEM_BEHAVIOR);
    }

    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);

        if (!blockstate.is(BlockTags.RAILS)) {
            return InteractionResult.FAIL;
        } else {
            ItemStack itemstack = context.getItemInHand();
            if (!level.isClientSide) {
                WagonEntity wagonEntity = new WagonEntity(ccEntityInit.WAGON_ENTITY.get(), level);
                BaseRailBlock rail = (BaseRailBlock) blockstate.getBlock();

                if (rail.getRailDirection(blockstate, level, blockpos, null).equals(RailShape.NORTH_SOUTH)) {
                    wagonEntity.setPos(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D);
                    if (context.getPlayer().getZ() > wagonEntity.getZ()) {
                        wagonEntity.setYRot(180.0F);
                    }
                    else {
                        wagonEntity.setYRot(0.0F);
                    }
                    level.addFreshEntity(wagonEntity);
                    level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }
                else if (rail.getRailDirection(blockstate, level, blockpos, null).equals(RailShape.EAST_WEST)) {
                    wagonEntity.setPos(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D);
                    if (context.getPlayer().getX() > wagonEntity.getX()) {
                        wagonEntity.setYRot(90.0F);
                    }
                    else {
                        wagonEntity.setYRot(270.0F);
                    }
                    level.addFreshEntity(wagonEntity);
                    level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }
            }

            itemstack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
}