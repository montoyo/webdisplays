/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraftforge.fml.common.Optional;
import net.montoyo.wd.core.CCArguments;
import net.montoyo.wd.core.IComputerArgs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "computercraft")
public class TileEntityCCInterface extends TileEntityInterfaceBase implements IPeripheral {

    private static final String[] METHOD_NAMES;
    private static final Method[] METHODS;

    static {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Method> methods = new ArrayList<>();
        Method[] src = TileEntityInterfaceBase.class.getMethods();

        for(Method m: src) {
            if(m.getAnnotation(TileEntityInterfaceBase.ComputerFunc.class) != null) {
                if(m.getParameterCount() != 1 || m.getParameterTypes()[0] != IComputerArgs.class)
                    throw new RuntimeException("Found @ComputerFunc method with invalid arguments");

                if(m.getReturnType() != Object[].class)
                    throw new RuntimeException("Found @ComputerFunc method with invalid return type");

                names.add(m.getName());
                methods.add(m);
            }
        }

        METHOD_NAMES = names.toArray(new String[0]);
        METHODS = methods.toArray(new Method[0]);
    }

    @Nonnull
    @Override
    public String getType() {
        return "webdisplays";
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return METHOD_NAMES;
    }

    @Optional.Method(modid = "computercraft")
    @Nullable
    @Override
    public Object[] callMethod(@Nonnull IComputerAccess ca, @Nonnull ILuaContext ctx, int mid, @Nonnull Object[] args) throws LuaException, InterruptedException {
        try {
            return (Object[]) METHODS[mid].invoke(this, new CCArguments(args));
        } catch(IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch(InvocationTargetException e) {
            if(e.getCause() instanceof IllegalArgumentException)
                throw new LuaException(e.getCause().getMessage());
            else
                throw new RuntimeException(e.getCause());
        }
    }

    @Optional.Method(modid = "computercraft")
    @Override
    public boolean equals(@Nullable IPeripheral periph) {
        return periph == this;
    }

}
