package com.frostmourneee.minecart.core.init;

import com.frostmourneee.minecart.minecart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ccSoundInit {

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, minecart.MOD_ID);

    public static final RegistryObject<SoundEvent> CART_CLAMP = SOUNDS.register("entity.cart.clamp",
            () -> new SoundEvent(new ResourceLocation(minecart.MOD_ID, "entity.cart.clamp")));

    public static final RegistryObject<SoundEvent> CART_UNCLAMP = SOUNDS.register("entity.cart.unclamp",
            () -> new SoundEvent(new ResourceLocation(minecart.MOD_ID, "entity.cart.unclamp")));

    public static final RegistryObject<SoundEvent> CART_CLAMP_FAIL = SOUNDS.register("entity.cart.clamp_fail",
            () -> new SoundEvent(new ResourceLocation(minecart.MOD_ID, "entity.cart.clamp_fail")));

    public static final RegistryObject<SoundEvent> LOCOMOTIVE_START = SOUNDS.register("entity.locomotive.start",
            () -> new SoundEvent(new ResourceLocation(minecart.MOD_ID, "entity.locomotive.start")));

    private ccSoundInit() {

    }
}
