package com.frostmourneee.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ccUtil {

    ccUtil() {}

    public static boolean nearZero(double value, double epsilon) {
        return Math.abs(value) < epsilon;
    }
    public static boolean nearZero(Vec3 vec, double epsilon) {
        return nearZero(vec.x, epsilon) && nearZero(vec.y, epsilon) && nearZero(vec.z, epsilon);
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
    public static AABB getAABBBetweenBlocks(BlockPos pos1, BlockPos pos2) {
        if (pos1 != pos2) {
            int x1 = Math.abs(pos1.getX() - pos2.getX()) > Math.abs(pos1.getX() + 1 - pos2.getX()) ? pos1.getX() : pos1.getX() + 1;
            int y1 = Math.abs(pos1.getY() - pos2.getY()) > Math.abs(pos1.getY() + 1 - pos2.getY()) ? pos1.getY() : pos1.getY() + 1;
            int z1 = Math.abs(pos1.getZ() - pos2.getZ()) > Math.abs(pos1.getZ() + 1 - pos2.getZ()) ? pos1.getZ() : pos1.getZ() + 1;

            int x2 = Math.abs(pos2.getX() - x1) > Math.abs(pos2.getX() + 1 - x1) ? pos2.getX() : pos2.getX() + 1;
            int y2 = Math.abs(pos2.getY() - y1) > Math.abs(pos2.getY() + 1 - y1) ? pos2.getY() : pos2.getY() + 1;
            int z2 = Math.abs(pos2.getZ() - z1) > Math.abs(pos2.getZ() + 1 - z1) ? pos2.getZ() : pos2.getZ() + 1;

            return new AABB(x1, y1, z1, x2, y2, z2);
        } else return new AABB(pos1);
    }
}
