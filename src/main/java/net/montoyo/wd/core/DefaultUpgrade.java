/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

public enum DefaultUpgrade {

    LASER_MOUSE("lasermouse"),
    REDSTONE_INPUT("redinput"),
    REDSTONE_OUTPUT("redoutput");

    private final String name;

    DefaultUpgrade(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

}
