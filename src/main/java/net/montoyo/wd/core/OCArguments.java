/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import li.cil.oc.api.machine.Arguments;
import net.minecraftforge.fml.common.Optional;

import java.util.Map;

@Optional.Interface(iface = "net.montoyo.wd.core.IComputerArgs", modid = "opencomputers")
public class OCArguments implements IComputerArgs {

    //Keep this as an "Object" so that it doesn't crash if OC is absent
    private final Object args;

    public OCArguments(Object a) {
        args = a;
    }

    @Optional.Method(modid = "opencomputers")
    @Override
    public String checkString(int i) {
        return ((Arguments) args).checkString(i);
    }

    @Optional.Method(modid = "opencomputers")
    @Override
    public int checkInteger(int i) {
        return ((Arguments) args).checkInteger(i);
    }

    @Optional.Method(modid = "opencomputers")
    @Override
    public Map checkTable(int i) {
        return ((Arguments) args).checkTable(i);
    }

    @Optional.Method(modid = "opencomputers")
    @Override
    public int count() {
        return ((Arguments) args).count();
    }

}
