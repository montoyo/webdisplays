/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

public enum AdvancementIcon {

    WEB_DISPLAYS("wd"),
    BROKEN_PAD("brokenpad"),
    PIGEON("pigeon");

    private final String name;

    AdvancementIcon(String n) {
        name = n;
    }

    @Override
    public String toString() {
        return name;
    }

}
