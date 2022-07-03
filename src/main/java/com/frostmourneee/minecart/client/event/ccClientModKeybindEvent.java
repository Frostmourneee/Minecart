package com.frostmourneee.minecart.client.event;


import com.frostmourneee.minecart.minecart;
import net.minecraftforge.api.distmarker.Dist;
import com.frostmourneee.minecart.client.ccKeyInit;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;


@Mod.EventBusSubscriber(modid = minecart.MOD_ID, bus = Bus.MOD, value = Dist.CLIENT)
public class ccClientModKeybindEvent {

    private ccClientModKeybindEvent() {}

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        ccKeyInit.init();
    }
}
