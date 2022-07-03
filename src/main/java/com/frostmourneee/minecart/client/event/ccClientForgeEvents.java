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

    private static final String NBT_KEY = minecart.MOD_ID + ".first_joined";
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getPlayer();
        if (player instanceof ServerPlayer) {

            CompoundTag data = player.getPersistentData();
            CompoundTag persistent;
            if (!data.contains(Player.PERSISTED_NBT_TAG)) {
                data.put(Player.PERSISTED_NBT_TAG, (persistent = new CompoundTag()));
            } else {
                persistent = data.getCompound(Player.PERSISTED_NBT_TAG);
            }

            if (!persistent.contains(NBT_KEY)) {
                persistent.putBoolean(NBT_KEY, true);

                ItemStack stack1 = new ItemStack(ccItemInit.LOCOMOTIVE_ITEM.get());
                ItemStack stack2 = new ItemStack(Items.APPLE);
                ItemStack stack3 = new ItemStack(Blocks.RAIL);
                ItemStack stack4 = new ItemStack(ccItemInit.WAGON_ITEM.get());
                ItemStack stack5 = new ItemStack(ccItemInit.CLAMP.get());
                ItemStack stack6 = new ItemStack(ccItemInit.DEBUG_ITEM.get());
                player.addItem(stack1);
                player.addItem(stack2);
                player.addItem(stack3);
                player.addItem(stack4);
                player.addItem(stack5);
                player.addItem(stack6);

                // message, fired when the player joins for the first time
                player.sendMessage(new TextComponent(player.getDisplayName().getString() +  " joined the for the first time!"), player.getUUID());
            } else {
                // another message, fired when the player doesn't join for the first time
                player.sendMessage(new TextComponent("Welcome back, " + player.getDisplayName().getString() + "!"), player.getUUID());
            }
        }
    }
}
