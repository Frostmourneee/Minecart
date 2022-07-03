package com.frostmourneee.minecart.core.network;

import com.frostmourneee.minecart.common.entity.AbstractCart;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ServerboundCartUpdatePacket {
    public final int cartId;

    public ServerboundCartUpdatePacket(int id) {
        cartId = id;
    }

    public ServerboundCartUpdatePacket(FriendlyByteBuf buffer) {
        this(buffer.readInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(cartId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        final var success = new AtomicBoolean(false);
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getSender().level.getEntity(cartId) instanceof AbstractCart cart) {
                cart.isClamping = true;

                success.set(true);
            } //LOGIC HERE
        });

        ctx.get().setPacketHandled(true);
        return success.get();
    }
}
