package com.frostmourneee.minecart.core.init;

import com.frostmourneee.minecart.common.item.ClampItem;
import com.frostmourneee.minecart.common.item.LocomotiveItem;
import com.frostmourneee.minecart.common.item.WagonItem;
import com.frostmourneee.minecart.minecart;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public final class ccItemInit {
    private ccItemInit() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, minecart.MOD_ID);

    public static final RegistryObject<BlockItem> NEW_OBSIDIAN_ITEM = ITEMS.register("obsidian_new",
            () -> new BlockItem(ccBlockInit.OBSIDIANNEW.get(), new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS).fireResistant()));

    public static final RegistryObject<LocomotiveItem> LOCOMOTIVE_ITEM = ITEMS.register("locomotive",
            () -> new LocomotiveItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION).stacksTo(1)));

    public static final RegistryObject<WagonItem> WAGON_ITEM = ITEMS.register("wagon",
            () -> new WagonItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION).stacksTo(1)));

    public  static final RegistryObject<ClampItem> CLAMP = ITEMS.register("clamp",
            () -> new ClampItem(new Item.Properties().tab(CreativeModeTab.TAB_TRANSPORTATION).stacksTo(1)));
}
