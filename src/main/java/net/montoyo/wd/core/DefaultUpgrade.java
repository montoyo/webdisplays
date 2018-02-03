/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.item.ItemStack;
import net.montoyo.wd.WebDisplays;

public enum DefaultUpgrade {

    LASER_MOUSE("lasermouse"),
    REDSTONE_INPUT("redinput"),
    REDSTONE_OUTPUT("redoutput"),
    GPS("gps");

    private final String name;

    DefaultUpgrade(String n) {
        name = n;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean matches(ItemStack is) {
        return is.getItem() == WebDisplays.INSTANCE.itemUpgrade && is.getMetadata() == ordinal();
    }

}
