package com.frostmourneee.minecart;

import com.frostmourneee.minecart.core.init.ccBlockInit;
import com.frostmourneee.minecart.core.init.ccEntityInit;
import com.frostmourneee.minecart.core.init.ccItemInit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("minecart")
public class minecart
{
    // Directly reference a log4j logger.
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "minecart";

    public minecart() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ccBlockInit.BLOCKS.register(eventBus);
        ccItemInit.ITEMS.register(eventBus);
        ccEntityInit.ENTITIES.register(eventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }
}
