package com.frostmourneee.minecart.core.init;

import com.frostmourneee.minecart.common.entity.LocomotiveEntity;
import com.frostmourneee.minecart.common.entity.WagonEntity;
import com.frostmourneee.minecart.minecart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.RegistryObject;

public class ccEntityInit {

    private ccEntityInit() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITIES, minecart.MOD_ID);

    public static final RegistryObject<EntityType<LocomotiveEntity>> LOCOMOTIVE_ENTITY = ENTITIES.register("locomotive",
            () -> EntityType.Builder.of(LocomotiveEntity::new, MobCategory.MISC).sized(0.98F, 0.7F)
                    .build(new ResourceLocation(minecart.MOD_ID,"locomotive").toString()));

    public static final RegistryObject<EntityType<WagonEntity>> WAGON_ENTITY = ENTITIES.register("wagon",
            () -> EntityType.Builder.of(WagonEntity::new, MobCategory.MISC).sized(0.98F, 0.7F)
                    .build(new ResourceLocation(minecart.MOD_ID,"wagon").toString()));
}
