package com.frostmourneee.minecart.common.helper;

import com.frostmourneee.minecart.common.entity.AbstractCart;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public class Clamp {

    private static EntityDataAccessor<Boolean> DATA_CLAMPED = SynchedEntityData.defineId(AbstractCart.class, EntityDataSerializers.BOOLEAN);
    public AbstractCart cart;

    public Clamp(EntityDataAccessor<Boolean> DATA, AbstractCart abstractCart) {
        DATA_CLAMPED = DATA;
        cart = abstractCart;
    }
}
