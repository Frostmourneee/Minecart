package com.frostmourneee.minecart;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;

public class ccUtil {
    ccUtil() {}

    //Standard near zero const
    public static final double ZERO_INDENT = 1.0E-4;

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
    /**
     * Two given BlockPoses considered to be diagonally opposite. Then an AABB of blocks is built by given two BlockPos.
     * Method returns array of all BlockPoses in that AABB. Then this array can be considered as three-dimensional array.
     * First "floor" begins from the smallest X coordinate blocks to the largest X coordinate block (with same Z coordinate).
     * On the next step Z = Z + 1; When the first "floor" has been put into array then Y = Y + 1.
     */
    public static ArrayList<BlockPos> getAllBlockPosesInBox(BlockPos pos1, BlockPos pos2) {
        ArrayList<BlockPos> result = new ArrayList<>();

        if (pos1 != pos2) {
            int dx = pos1.getX() - pos2.getX(); //3
            int dy = pos1.getY() - pos2.getY(); //0
            int dz = pos1.getZ() - pos2.getZ(); //0

            int x = dx <= 0 ? pos1.getX() : pos2.getX(); //12
            int y = dy <= 0 ? pos1.getY() : pos2.getY(); //-60
            int z = dz <= 0 ? pos1.getZ() : pos2.getZ(); //12

            for (int i = 0; i < Math.abs(dy) + 1; i++) {
                for (int j = 0; j < Math.abs(dz) + 1; j++) {
                    for (int k = 0; k < Math.abs(dx) + 1; k++) {
                        result.add(new BlockPos(x + k, y + i, z + j));
                    }
                }
            }
        } else {
            result.add(pos1);
        }

        return result;
    }
    public static void renderVisibleIndicator(BlockState blockState, PoseStack poseStack, MultiBufferSource buffer, int int4) {
        poseStack.pushPose();
        poseStack.scale(0.1F, 0.1F, 0.1F);
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(blockState, poseStack, buffer, int4, OverlayTexture.NO_OVERLAY, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
        poseStack.popPose();
    }
    public static boolean railIsRotating(RailShape shape) {
        return shape.equals(RailShape.NORTH_EAST) || shape.equals(RailShape.NORTH_WEST) || shape.equals(RailShape.SOUTH_EAST) || shape.equals(RailShape.SOUTH_WEST);
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
