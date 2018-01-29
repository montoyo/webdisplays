/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.item.Item;
import net.montoyo.wd.WebDisplays;

public class ItemLaserPointer extends Item {

    public ItemLaserPointer() {
        setUnlocalizedName("webdisplays.laserpointer");
        setRegistryName("laserpointer");
        setMaxStackSize(1);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

}
