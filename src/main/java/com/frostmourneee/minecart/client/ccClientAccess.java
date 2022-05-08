package com.frostmourneee.minecart.client;

import com.frostmourneee.minecart.common.entity.AbstractCart;
import net.minecraft.client.Minecraft;

public class ccClientAccess {
    public static boolean cartDebugModeUpdated(int id) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getEntity(id) instanceof AbstractCart cart) {
            cart.debugMode = true;

            return true;
        } //LOGIC HERE

        return false;
    }
}
