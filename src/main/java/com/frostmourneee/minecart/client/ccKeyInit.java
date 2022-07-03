package com.frostmourneee.minecart.client;

import com.frostmourneee.minecart.minecart;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.ClientRegistry;

public final class ccKeyInit {
    private ccKeyInit() {}

    public static KeyMapping exampleKeyMapping;

    public static void init() {
        exampleKeyMapping = registerKey("example_key", KeyMapping.CATEGORY_GAMEPLAY , InputConstants.KEY_K);
    }

    private static KeyMapping registerKey(String name, String category, int keycode) {
        final var key = new KeyMapping("key."+ minecart.MOD_ID + "." + name, keycode, category);
        ClientRegistry.registerKeyBinding(key);
        return key;
    }
}
