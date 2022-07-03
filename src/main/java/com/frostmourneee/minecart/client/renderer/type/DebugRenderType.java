package com.frostmourneee.minecart.client.renderer.type;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

public class DebugRenderType extends RenderType {

    public DebugRenderType(String name, VertexFormat format, Mode mode, int bufferSize, boolean affectsCrumbling, boolean sort, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sort, setupState, clearState);
    }

    /** Render type for the error block that is seen through everything, mostly based on {@link RenderType#LINES} */
    public static final RenderType ERROR_BLOCK = RenderType.create(
            "my_test_rendertype", DefaultVertexFormat.POSITION_COLOR_NORMAL, VertexFormat.Mode.LINES, 256, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.empty()))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    //.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    //.setDepthTestState(NO_DEPTH_TEST)
                    .createCompositeState(false));
}