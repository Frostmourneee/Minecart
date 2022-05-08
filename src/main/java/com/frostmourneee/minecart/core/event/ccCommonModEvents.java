package com.frostmourneee.minecart.core.event;

import com.frostmourneee.minecart.core.init.PacketHandler;
import com.frostmourneee.minecart.minecart;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = minecart.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ccCommonModEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::init);
    }
}
