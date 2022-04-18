package com.frostmourneee.minecart.core;

import com.frostmourneee.minecart.common.entity.AbstractCart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class ccUtil {

    public ccUtil() {}

    public static boolean goesUpper(AbstractCart cart) {
        BlockPos blockPosBelow = new BlockPos(blockPosToVec3(cart.getOnPos()));
        BlockPos blockPosUp = new BlockPos(blockPosToVec3(cart.getOnPos().above()));
        BlockState blockStateBelow = cart.level.getBlockState(blockPosBelow);
        BlockState blockStateUp = cart.level.getBlockState(blockPosUp);
        Vec3 delta = cart.getDeltaMovement();

        if (directionsProcessing(delta, blockStateUp, blockPosUp, cart)) return true;
        else if (directionsProcessing(delta, blockStateBelow, blockPosBelow, cart)) return true;
        else return false;
    }

    public static boolean directionsProcessing(Vec3 delta, BlockState blockState, BlockPos blockPos, AbstractCart cart) {
        if (isRail(blockState)) {
            if (anyRailShape(blockState, blockPos, cart).isAscending()) {
                if (delta.x > 0 && delta.z == 0) {
                    BlockState blockStateFront = cart.level.getBlockState(new BlockPos(blockPosToVec3(blockPos).add(1.0D, 1.0D, 0.0D)));
                    return isRail(blockStateFront);
                }
                if (delta.x < 0 && delta.z == 0) {
                    BlockState blockStateFront = cart.level.getBlockState(new BlockPos(blockPosToVec3(blockPos).add(-1.0D, 1.0D, 0.0D)));
                    return isRail(blockStateFront);
                }
                if (delta.z > 0 && delta.x == 0) {
                    BlockState blockStateFront = cart.level.getBlockState(new BlockPos(blockPosToVec3(blockPos).add(0.0D, 1.0D, 1.0D)));
                    return isRail(blockStateFront);
                }
                if (delta.z < 0 && delta.x == 0) {
                    BlockState blockStateFront = cart.level.getBlockState(new BlockPos(blockPosToVec3(blockPos).add(0.0D, 1.0D, -1.0D)));
                    return isRail(blockStateFront);
                }
            }
        }

        return false;
    }

    public static boolean goesFlat(AbstractCart cart) {
        BlockPos blockPos = cart.blockPosition();
        BlockState blockState = cart.level.getBlockState(blockPos);

        if (isRail(blockState)) {
            return !anyRailShape(blockState, blockPos, cart).isAscending();
        }
        else return false;
    }

    public static boolean bothUpOrDownOrForward(AbstractCart backCart, AbstractCart frontCart) {

        BlockPos blockPos = new BlockPos(0.5D * (backCart.getX() + frontCart.getX()), 0.5D * (backCart.getY() + frontCart.getY()), 0.5D * (backCart.getZ() + frontCart.getZ()));
        BlockState blockState = backCart.level.getBlockState(blockPos);
        if (goesFlat(backCart) && goesFlat(frontCart) && bothOnOneLine(backCart, frontCart)) return true;
        else if (goesFlat(backCart) || goesFlat(frontCart)) return false;
        else if (goesUpper(backCart) && goesUpper(frontCart) && isRail(blockState) && anyRailShape(blockState, blockPos, backCart).isAscending())
            return true;
        else if (!goesUpper(backCart) && !goesUpper(frontCart) && isRail(blockState) && anyRailShape(blockState, blockPos, backCart).isAscending())
            return true;

        else return false;
    }

    public static boolean bothOnOneLine(AbstractCart backCart, AbstractCart frontCart) {
        return Math.abs(backCart.getY() - frontCart.getY()) < 1.0E-4
                && (Math.abs(backCart.getX() - frontCart.getX()) < 1.0E-4 || Math.abs(backCart.getZ() - frontCart.getZ()) < 1.0E-4);
    }

    public static boolean zeroDeltaMovement(AbstractCart cart) {
        return Math.abs(cart.getDeltaMovement().x) < 1.0E-3 && Math.abs(cart.getDeltaMovement().y) < 1.0E-3
                && Math.abs(cart.getDeltaMovement().z) < 1.0E-3;
    }

    public static boolean zeroDeltaMovementBigIndent(AbstractCart cart) {
        return Math.abs(cart.getDeltaMovement().x) < 5.0E-1 && Math.abs(cart.getDeltaMovement().y) < 5.0E-1
                && Math.abs(cart.getDeltaMovement().z) < 5.0E-1;
    }

    public static boolean isStopped(AbstractCart cart) {
        return cart.xOld == cart.getX() && cart.zOld == cart.getZ();
    }

    public static boolean isRotating(AbstractCart cart) {
        BlockPos blockPos = cart.getOnPos().above();
        BlockState blockState = cart.level.getBlockState(blockPos);

        if (isRail(blockState)) {
            RailShape shape = anyRailShape(blockState, blockPos, cart);
            return railIsRotating(shape);
        } else return false;
    }

    public static boolean isRail(BlockState blockState) {
        return blockState.is(Blocks.RAIL) || blockState.is(Blocks.POWERED_RAIL) || blockState.is(Blocks.ACTIVATOR_RAIL) || blockState.is(Blocks.DETECTOR_RAIL);
    }

    public static boolean railIsRotating(RailShape shape) {
        return shape.equals(RailShape.NORTH_EAST) || shape.equals(RailShape.NORTH_WEST) || shape.equals(RailShape.SOUTH_EAST) || shape.equals(RailShape.SOUTH_WEST);
    }

    public static RailShape anyRailShape(BlockState blockState, BlockPos blockPos, AbstractCart cart) {
        return ((BaseRailBlock)blockState.getBlock())
                .getRailDirection(blockState, cart.level, blockPos, cart);
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

    public static Direction vecToDirection(Vec3 vec) {
        Vec3 absVec = new Vec3(Math.abs(vec.x), Math.abs(vec.y), Math.abs(vec.z));
        String tmp;

        if (absVec.x > absVec.y) {
            if (absVec.x > absVec.z) tmp = "X";
            else if (absVec.x < absVec.z) tmp = "Z";
            else return null;
        } else {
            if (absVec.y > absVec.z) tmp = "Y";
            else if (absVec.y < absVec.z) tmp = "Z";
            else return null;
        }

        Direction direction = null;
        switch(tmp) {
            case "X":
                if (vec.x > 0) direction = Direction.EAST;
                else direction = Direction.WEST;
                break;
            case "Y":
                if (vec.y > 0) direction = Direction.UP;
                else direction = Direction.DOWN;
                break;
            case "Z":
                if (vec.z > 0) direction = Direction.SOUTH;
                else direction = Direction.NORTH;
                break;
        }

        return direction;
    }

    public static Vec3 blockPosToVec3 (BlockPos blockPos) {
        return new Vec3(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }
}