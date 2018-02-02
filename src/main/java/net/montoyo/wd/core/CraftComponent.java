/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

public enum CraftComponent {

    STONE_KEY("stonekey"),
    BLANK_UPGRADE("upgrade"),
    PERIPHERAL_BASE("peripheral"),
    BATTERY_CELL("batcell"),
    BATTERY_PACK("batpack"),
    LASER_DIODE("laserdiode"),
    BACKLIGHT("backlight");

    private final String name;

    CraftComponent(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

}
