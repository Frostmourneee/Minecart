package com.frostmourneee.minecart.core.network;

import com.frostmourneee.minecart.client.ccClientAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ClientboundCartUpdatePacket {
    public final int cartId;

    public ClientboundCartUpdatePacket(int id) {
        cartId = id;
    }

    public ClientboundCartUpdatePacket(FriendlyByteBuf buffer) {
        this(buffer.readInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(cartId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> success.set(ccClientAccess.cartDebugModeUpdated(cartId)));
        });

        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
