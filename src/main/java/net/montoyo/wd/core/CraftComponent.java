/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.item.ItemStack;
import net.montoyo.wd.WebDisplays;

public enum CraftComponent {

    STONE_KEY("stonekey"),
    BLANK_UPGRADE("upgrade"),
    PERIPHERAL_BASE("peripheral"),
    BATTERY_CELL("batcell"),
    BATTERY_PACK("batpack"),
    LASER_DIODE("laserdiode"),
    BACKLIGHT("backlight"),
    EXTENSION_CARD("extcard"),
    BAD_EXTENSION_CARD("badextcard");

    private final String name;

    CraftComponent(String n) {
        name = n;
    }

    @Override
    public String toString() {
        return name;
    }

    public ItemStack makeItemStack() {
        return new ItemStack(WebDisplays.INSTANCE.itemCraftComp, 1, ordinal());
    }

}
