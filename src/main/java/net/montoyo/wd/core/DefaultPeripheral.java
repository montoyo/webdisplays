/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.util.IStringSerializable;
import net.montoyo.wd.entity.TileEntityKeyboard;
import net.montoyo.wd.entity.TileEntityPeripheralBase;
import net.montoyo.wd.entity.TileEntityRCtrl;

public enum DefaultPeripheral implements IStringSerializable {

    KEYBOARD("keyboard", TileEntityKeyboard.class),
    REMOTE_CONTROLLER("remotectrl", TileEntityRCtrl.class),
    CC_INTERFACE("ccinterface", null),
    OC_INTERFACE("cointerface", null);

    private final String name;
    private final Class<? extends TileEntityPeripheralBase> teClass;

    DefaultPeripheral(String name, Class<? extends TileEntityPeripheralBase> te) {
        this.name = name;
        teClass = te;
    }

    @Override
    public String getName() {
        return name;
    }

    public Class<? extends TileEntityPeripheralBase> getTEClass() {
        return teClass;
    }

}
