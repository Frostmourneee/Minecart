package com.frostmourneee.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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

    public static double maxOf3(double a, double b, double c) {
        return a >= b ? (Math.max(a, c)) : (Math.max(b, c));
    }
    public static Direction vecToDirection(Vec3 vec) {
        Vec3 absVec = new Vec3(Math.abs(vec.x), Math.abs(vec.y), Math.abs(vec.z));

        double max = maxOf3(absVec.x, absVec.y, absVec.z);
        if (nearZero(max - absVec.x, 1.0E-4)) {
            return vec.x > 1.0E-4 ? Direction.EAST : Direction.WEST;
        }
        else if (nearZero(max - absVec.z, 1.0E-4)) {
            return vec.z > 1.0E-4 ? Direction.SOUTH : Direction.NORTH;
        }
        else {
            return vec.y > 1.0E-4 ? Direction.UP : Direction.DOWN;
        }
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

    public static void customPrint(Object... str) {
        StringBuilder stringBuilder = new StringBuilder();

        for (Object s : str) {
            stringBuilder.append(s);
            stringBuilder.append(" ");
        }
        System.out.println(stringBuilder);
    }
}
