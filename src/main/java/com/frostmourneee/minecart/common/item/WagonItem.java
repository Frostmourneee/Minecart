package com.frostmourneee.minecart.common.item;

public class WagonItem extends AbstractCartItem {

    public WagonItem(Properties properties) {
        super(properties);
    }

    @Override
    public Type getItemType() {
        return Type.WAGON;
    }
}