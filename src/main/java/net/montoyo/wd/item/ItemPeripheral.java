/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.block.Block;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemMultiTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.BlockKeyboardRight;
import net.montoyo.wd.core.DefaultPeripheral;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemPeripheral extends ItemMultiTexture implements WDItem {

    public ItemPeripheral(Block block) {
        super(block, block, (is) -> DefaultPeripheral.fromMetadata(is.getMetadata()).getName());
    }

    @Override
    public boolean canPlaceBlockOnSide(World world, @Nonnull BlockPos pos_, @Nonnull EnumFacing side, EntityPlayer player, ItemStack stack) {
        if(stack.getMetadata() != 0) //Keyboard
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

    @Override
    public void addInformation(@Nullable ItemStack is, @Nullable World world, @Nullable List<String> tt, @Nullable ITooltipFlag ttFlags) {
        super.addInformation(is, world, tt, ttFlags);

        if(is != null && tt != null) {
            if(is.getMetadata() == 1 && !WebDisplays.isComputerCraftAvailable()) //CC Interface
                tt.add("" + ChatFormatting.RED + I18n.format("webdisplays.message.missingCC"));
            else if(is.getMetadata() == 2 && !WebDisplays.isOpenComputersAvailable()) //OC Interface
                tt.add("" + ChatFormatting.RED + I18n.format("webdisplays.message.missingOC"));
            else if(is.getMetadata() == 11 && WebDisplays.PROXY.isMiniservDisabled()) //Server
                tt.add("" + ChatFormatting.RED + I18n.format("webdisplays.message.noMiniserv"));
        }

        WDItem.addInformation(tt);
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return DefaultPeripheral.fromMetadata(is.getMetadata()).getWikiName();
    }

}
