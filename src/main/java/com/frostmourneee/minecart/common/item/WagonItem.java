package com.frostmourneee.minecart.common.item;

import com.frostmourneee.minecart.ccUtil;
import com.frostmourneee.minecart.common.entity.AbstractCart;
import com.frostmourneee.minecart.common.entity.WagonEntity;
import com.frostmourneee.minecart.core.init.ccEntityInit;
import com.frostmourneee.minecart.core.init.ccSoundInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

import static com.frostmourneee.minecart.common.entity.AbstractCart.*;

public class WagonItem extends AbstractCartItem {

    public WagonItem(Properties properties) {
        super(properties);
    }

    @Override
    public Type getItemType() {
        return Type.WAGON;
    }
}