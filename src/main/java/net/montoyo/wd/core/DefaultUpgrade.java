/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.item.ItemStack;
import net.montoyo.wd.WebDisplays;

public enum DefaultUpgrade {

    LASER_MOUSE("lasermouse", "Laser_Sensor"),
    REDSTONE_INPUT("redinput", "Redstone_Input_Port"),
    REDSTONE_OUTPUT("redoutput", "Redstone_Output_Port"),
    GPS("gps", "GPS_Module");

    private final String name;
    private final String wikiName;

    DefaultUpgrade(String n, String wn) {
        name = n;
        wikiName = wn;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean matches(ItemStack is) {
        return is.getItem() == WebDisplays.INSTANCE.itemUpgrade && is.getMetadata() == ordinal();
    }

    public static String getWikiName(int meta) {
        DefaultUpgrade[] values = values();
        return (meta >= 0 && meta < values.length) ? values[meta].wikiName : null;
    }

}
