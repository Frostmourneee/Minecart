package com.frostmourneee.minecart.core.init;

import com.frostmourneee.minecart.core.network.ClientboundCartUpdatePacket;
import com.frostmourneee.minecart.core.network.ServerboundCartUpdatePacket;
import com.frostmourneee.minecart.minecart;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(new ResourceLocation(minecart.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    public static void init() {
        int index = 0;
        INSTANCE.messageBuilder(ServerboundCartUpdatePacket.class, index++, NetworkDirection.PLAY_TO_SERVER).encoder(ServerboundCartUpdatePacket::encode)
                .decoder(ServerboundCartUpdatePacket::new).consumer(ServerboundCartUpdatePacket::handle)
                .add();
        INSTANCE.messageBuilder(ClientboundCartUpdatePacket.class, index++, NetworkDirection.PLAY_TO_CLIENT).encoder(ClientboundCartUpdatePacket::encode)
                .decoder(ClientboundCartUpdatePacket::new).consumer(ClientboundCartUpdatePacket::handle)
                .add();
    }
}
