/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.montoyo.wd.WebDisplays;

import javax.annotation.Nonnull;

public class WDCreativeTab extends CreativeTabs {

    public WDCreativeTab() {
        super("webdisplays");
    }

    @Override
    @Nonnull
    public ItemStack getTabIconItem() {
        return new ItemStack(WebDisplays.INSTANCE.blockScreen);
    }

}
