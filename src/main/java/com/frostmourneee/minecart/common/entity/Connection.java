package com.frostmourneee.minecart.common.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;

public class Connection {
    private static EntityDataAccessor<Boolean> DATA_CART_EXISTS;

    private boolean isConnected;
    private AbstractCart cart;

    private final SynchedEntityData entityData; // Data of connection origin

    public Connection(EntityDataAccessor<Boolean> dataCartExists, SynchedEntityData savedEntityData) {
        isConnected = false;
        cart = null;
        DATA_CART_EXISTS = dataCartExists;
        entityData = savedEntityData;
    }

    public void connect(AbstractCart connectingCart) {
        cart = connectingCart;
        isConnected = true;
        entityData.set(DATA_CART_EXISTS, true);
    }

    public void release() {
        cart = null;
        isConnected = false;
        entityData.set(DATA_CART_EXISTS, false);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public AbstractCart getCart() {
        return cart;
    }

}
