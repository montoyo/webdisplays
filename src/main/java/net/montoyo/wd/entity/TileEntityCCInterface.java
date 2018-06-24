package net.montoyo.wd.entity;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

public class TileEntityCCInterface extends TileEntityPeripheralBase implements IPeripheral
{
    @Override
    public boolean equals(IPeripheral other)
    {
        return other == this;
    }

    @Override
    public String getType()
    {
        return "WebScreen";
    }

    @Override
    public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] args) throws LuaException, InterruptedException
    {
        if (method == 0)
            return new Object[] { Boolean.valueOf(this.isLinked()) };
        if (method == 1) {
            if (this.isLinked()) {
                return new Object[] { Double.valueOf(this.screenPos.x), Double.valueOf(this.screenPos.y), Double.valueOf(this.screenPos.z) };
            }
            return null; }
        if (method == 2) {
            if ((args.length < 2) || (!(args[0] instanceof Double)) || (!(args[1] instanceof Double))) {
                return null;
            }
            TileEntityScreen ent = getConnectedScreenEx();
            if (ent == null) {
                return null;
            }
            // NOT YET IMPLEMENTED, TODO
            // ent.click(((Double)args[0]).floatValue(), ((Double)args[1]).floatValue());
            return null; }
        if (method == 3) {
            if ((args.length < 1) || (!(args[0] instanceof String))) {
                return null;
            }
            TileEntityScreen ent = getConnectedScreenEx();
            if (ent == null) {
                return null;
            }
            // TODO: NOT YET IMPLEMENTED
            // ent.type((String)args[0]);
            return null; }
        if (method == 4) {
            TileEntityScreen ent = getConnectedScreenEx();
            if (ent == null) {
                return null;
            }
            return new Object[] { ent.getScreen(screenSide).url }; }
        if (method == 5) {
            if ((args.length < 1) || (!(args[0] instanceof String))) {
                return null;
            }
            TileEntityScreen ent = getConnectedScreenEx();
            if (ent == null) {
                return null;
            }
            ent.setScreenURL(this.screenSide, (String)args[0]);
            return null;
        }
        if (method == 6) {
            TileEntityScreen ent = getConnectedScreenEx();
            if (ent == null) {
                return null;
            }
            // TODO: NOT YET IMPLEMENTED // ent.getScreen(screenSide).setDead() or something
            return null; }
        if (method == 7) {
            if ((args.length < 1) || (!(args[0] instanceof String))) {
                return null;
            }
            TileEntityScreen ent = getConnectedScreenEx();
            if (ent == null) {
                return null;
            }
            try {
                // TODO: not yet implemented.  call runJS() probably.
                //ent.getScreen(screenSide).browser.runJS();
                //ent.sendJS((String)args[0]);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            return null;
        }
        return null;
    }

    public static final String[] methods = { "isLinked", "getScreenPos", "click", "type", "getUrl", "setUrl", "shutdown", "runJS" };

    public String[] getMethodNames()
    {
        return methods;
    }
}