/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraftforge.fml.common.Optional;
import net.montoyo.wd.core.OCArguments;

@Optional.Interface(iface = "li.cil.oc.api.network.SimpleComponent", modid = "opencomputers")
public class TileEntityOCInterface extends TileEntityInterfaceBase implements SimpleComponent {

    @Override
    public String getComponentName() {
        return "webdisplays";
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] isLinked(Context ctx, Arguments args) {
        return isLinked(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] isScreenChunkLoaded(Context ctx, Arguments args) {
        return isScreenChunkLoaded(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getScreenPos(Context ctx, Arguments args) {
        return getScreenPos(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getScreenSide(Context ctx, Arguments args) {
        return getScreenSide(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getOwner(Context ctx, Arguments args) {
        return getOwner(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] can(Context ctx, Arguments args) {
        return can(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] hasUpgrade(Context ctx, Arguments args) {
        return hasUpgrade(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getSize(Context ctx, Arguments args) {
        return getSize(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getResolution(Context ctx, Arguments args) {
        return getResolution(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getRotation(Context ctx, Arguments args) {
        return getRotation(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] getURL(Context ctx, Arguments args) {
        return getURL(new OCArguments(args));
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] click(Context ctx, Arguments args) {
        return click(new OCArguments(args));
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] type(Context ctx, Arguments args) {
        return type(new OCArguments(args));
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] typeAdvanced(Context ctx, Arguments args) {
        return typeAdvanced(new OCArguments(args));
    }

    @Callback(limit = 1)
    @Optional.Method(modid = "opencomputers")
    public Object[] setURL(Context ctx, Arguments args) {
        return setURL(new OCArguments(args));
    }

    @Callback(limit = 1)
    @Optional.Method(modid = "opencomputers")
    public Object[] setResolution(Context ctx, Arguments args) {
        return setResolution(new OCArguments(args));
    }

    @Callback(limit = 1)
    @Optional.Method(modid = "opencomputers")
    public Object[] setRotation(Context ctx, Arguments args) {
        return setRotation(new OCArguments(args));
    }

    @Callback(limit = 4)
    @Optional.Method(modid = "opencomputers")
    public Object[] runJS(Context ctx, Arguments args) {
        return runJS(new OCArguments(args));
    }

    @Callback
    @Optional.Method(modid = "opencomputers")
    public Object[] unlink(Context ctx, Arguments args) {
        return unlink(new OCArguments(args));
    }

}
