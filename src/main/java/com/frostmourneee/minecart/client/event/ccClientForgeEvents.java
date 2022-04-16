package com.frostmourneee.minecart.client.event;

import com.frostmourneee.minecart.client.ccKeyInit;
import com.frostmourneee.minecart.core.init.ccItemInit;
import com.frostmourneee.minecart.minecart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@Mod.EventBusSubscriber(modid = minecart.MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
public final class ccClientForgeEvents {
    private ccClientForgeEvents() {}

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (ccKeyInit.exampleKeyMapping.isDown()) {

        }
    }
}
