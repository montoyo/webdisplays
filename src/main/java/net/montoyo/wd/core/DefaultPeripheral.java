/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.util.IStringSerializable;
import net.montoyo.wd.entity.TileEntityKeyboard;
import net.montoyo.wd.entity.TileEntityPeripheralBase;
import net.montoyo.wd.entity.TileEntityRCtrl;
import net.montoyo.wd.entity.TileEntityRedCtrl;

public enum DefaultPeripheral implements IStringSerializable {

    KEYBOARD("keyboard", TileEntityKeyboard.class),                 //WITH FACING (< 3)
    CC_INTERFACE("ccinterface", null),
    OC_INTERFACE("cointerface", null),
    REMOTE_CONTROLLER("remotectrl", TileEntityRCtrl.class),         //WITHOUT FACING (>= 3)
    REDSTONE_CONTROLLER("redstonectrl", TileEntityRedCtrl.class);

    private final String name;
    private final Class<? extends TileEntityPeripheralBase> teClass;

    DefaultPeripheral(String name, Class<? extends TileEntityPeripheralBase> te) {
        this.name = name;
        teClass = te;
    }

    public static DefaultPeripheral fromMetadata(int meta) {
        if((meta & 3) == 3)
            return values()[(((meta >> 2) & 3) | 4) - 1]; //Without facing
        else
            return values()[meta & 3]; //With facing
    }

    @Override
    public String getName() {
        return name;
    }

    public Class<? extends TileEntityPeripheralBase> getTEClass() {
        return teClass;
    }

    public boolean hasFacing() {
        return ordinal() < 3;
    }

    public int toMetadata(int facing) {
        int ret = ordinal();
        if(ret < 3) //With facing
            ret |= facing << 2;
        else //Without facing
            ret = (((ret + 1) & 3) << 2) | 3;

        return ret;
    }

}
