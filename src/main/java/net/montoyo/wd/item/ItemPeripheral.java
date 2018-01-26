/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMultiTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.montoyo.wd.block.BlockKeyboardRight;
import net.montoyo.wd.core.DefaultPeripheral;

public class ItemPeripheral extends ItemMultiTexture {

    public ItemPeripheral(Block block) {
        super(block, block, new ItemMultiTexture.Mapper() {
            @Override
            public String apply(ItemStack is) {
                return DefaultPeripheral.values()[is.getMetadata()].getName();
            }
        });
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, BlockPos pos_, EnumFacing side, EntityPlayer player, ItemStack stack) {
        if(stack.getMetadata() != 0)
            return true;

        //Special checks for the keyboard
        BlockPos pos = pos_.add(side.getDirectionVec());
        if(world.isAirBlock(pos.down()) || !BlockKeyboardRight.checkNeighborhood(world, pos, null))
            return false;

        int f = MathHelper.floor(((double) (player.rotationYaw * 4.0f / 360.0f)) + 2.5) & 3;
        Vec3i dir = EnumFacing.getHorizontal(f).rotateY().getDirectionVec();
        BlockPos left = pos.add(dir);
        BlockPos right = pos.subtract(dir);

        if(world.isAirBlock(right) && !world.isAirBlock(right.down()) && BlockKeyboardRight.checkNeighborhood(world, right, null))
            return true;
        else
            return world.isAirBlock(left) && !world.isAirBlock(left.down()) && BlockKeyboardRight.checkNeighborhood(world, left, null);
    }

}
