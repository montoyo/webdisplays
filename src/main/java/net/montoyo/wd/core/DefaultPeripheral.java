/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IStringSerializable;
import net.montoyo.wd.entity.*;

import javax.annotation.Nonnull;

public enum DefaultPeripheral implements IStringSerializable {

    KEYBOARD("keyboard", "Keyboard", TileEntityKeyboard.class),                          //WITH FACING (< 3)
    CC_INTERFACE("ccinterface", "ComputerCraft_Interface", TileEntityCCInterface.class),
    OC_INTERFACE("cointerface", "OpenComputers_Interface", TileEntityOCInterface.class),
    REMOTE_CONTROLLER("remotectrl", "Remote_Controller", TileEntityRCtrl.class),         //WITHOUT FACING (>= 3)
    REDSTONE_CONTROLLER("redstonectrl", "Redstone_Controller", TileEntityRedCtrl.class),
    SERVER("server", "Server", TileEntityServer.class);

    private final String name;
    private final String wikiName;
    private final Class<? extends TileEntity> teClass;

    DefaultPeripheral(String name, String wname, Class<? extends TileEntity> te) {
        this.name = name;
        wikiName = wname;
        teClass = te;
    }

    public static DefaultPeripheral fromMetadata(int meta) {
        if((meta & 3) == 3)
            return values()[(((meta >> 2) & 3) | 4) - 1]; //Without facing
        else
            return values()[meta & 3]; //With facing
    }

    @Override
    @Nonnull
    public String getName() {
        return name;
    }

    public Class<? extends TileEntity> getTEClass() {
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

    public String getWikiName() {
        return wikiName;
    }

}
