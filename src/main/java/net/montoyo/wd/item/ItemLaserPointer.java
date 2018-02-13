/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemLaserPointer extends Item implements WDItem {

    public ItemLaserPointer() {
        setUnlocalizedName("webdisplays.laserpointer");
        setRegistryName("laserpointer");
        setMaxStackSize(1);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

    @Override
    public void addInformation(ItemStack is, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        WDItem.addInformation(tt);
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return "Laser_Pointer";
    }

}
