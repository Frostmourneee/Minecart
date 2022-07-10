package com.frostmourneee.minecart.client.event;


import com.frostmourneee.minecart.client.renderer.LocomotiveEntityRenderer;
import com.frostmourneee.minecart.client.renderer.WagonEntityRenderer;
import com.frostmourneee.minecart.client.renderer.model.DebugWagonModel;
import com.frostmourneee.minecart.client.renderer.model.LocomotiveEntityModel;
import com.frostmourneee.minecart.client.renderer.model.WagonEntityModel;
import com.frostmourneee.minecart.core.init.ccEntityInit;
import com.frostmourneee.minecart.minecart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid = minecart.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class ccClientModRenderEvent {

    private ccClientModRenderEvent() {}

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LocomotiveEntityModel.LAYER_LOCATION, LocomotiveEntityModel::createBodyLayer);
        event.registerLayerDefinition(WagonEntityModel.LAYER_LOCATION, WagonEntityModel::createBodyLayer);

        //////////////////////TEST MODELS////////////////////////// //TODO remove debug

        event.registerLayerDefinition(DebugWagonModel.LAYER_LOCATION, DebugWagonModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ccEntityInit.LOCOMOTIVE_ENTITY.get(), LocomotiveEntityRenderer::new);
        event.registerEntityRenderer(ccEntityInit.WAGON_ENTITY.get(), WagonEntityRenderer::new);
    }
}
