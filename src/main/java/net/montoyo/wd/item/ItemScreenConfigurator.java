/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemScreenConfigurator extends Item implements WDItem {

    public ItemScreenConfigurator() {
        setUnlocalizedName("webdisplays.screencfg");
        setRegistryName("screencfg");
        setMaxStackSize(1);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side_, float hitX, float hitY, float hitZ) {
        if(player.isSneaking() || !(world.getBlockState(pos).getBlock() instanceof BlockScreen))
            return EnumActionResult.PASS;

        if(world.isRemote)
            return EnumActionResult.SUCCESS;

        Vector3i origin = new Vector3i(pos);
        BlockSide side = BlockSide.values()[side_.ordinal()];

        Multiblock.findOrigin(world, origin, side, null);
        TileEntity te = world.getTileEntity(origin.toBlock());

        if(te == null || !(te instanceof TileEntityScreen)) {
            Util.toast(player, "turnOn");
            return EnumActionResult.SUCCESS;
        }

        TileEntityScreen.Screen scr = ((TileEntityScreen) te).getScreen(side);
        if(scr == null)
            Util.toast(player, "turnOn");
        else
            (new ScreenConfigData(origin, side, scr)).sendTo((EntityPlayerMP) player);

        return EnumActionResult.SUCCESS;
    }

    @Override
    public void addInformation(ItemStack is, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        WDItem.addInformation(tt);
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return "Screen_Configurator";
    }

}
