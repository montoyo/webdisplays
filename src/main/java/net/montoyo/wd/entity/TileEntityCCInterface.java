package net.montoyo.wd.entity;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.montoyo.wd.utilities.Vector2i;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class TileEntityCCInterface extends TileEntityPeripheralBase implements IPeripheral
{
    public enum CCLuaMethod {
        METHOD_IS_LINKED("isLinked"),
        METHOD_GET_SCREEN_POS("getScreenPos"),
        METHOD_CLICK("click"),
        METHOD_TYPE("type"),
        METHOD_GETURL("getUrl"),
        METHOD_SETURL("setUrl"),
        METHOD_SHUTDOWN("shutdown"),
        METHOD_RUNJS("runJS");

        private final String name;

        CCLuaMethod(String n) {
            name = n;
        }

        public String toString() {
            return name;
        }

        public static CCLuaMethod getMethod(int index) {
            return values()[index];
        }

        public static String[] getMethodsList() {
            return Arrays.stream(CCLuaMethod.values()).map(CCLuaMethod::toString).toArray(String[]::new);
        }
    }

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

    public String[] getMethodNames()
    {
        return CCLuaMethod.getMethodsList();
    }

    @Override
    public Object[] callMethod(@Nonnull IComputerAccess computer,
                               @Nonnull ILuaContext context,
                               int method,
                               @Nonnull Object[] args)
            throws LuaException, InterruptedException
    {
        CCLuaMethod method_name = CCLuaMethod.getMethod(method);

        boolean requireLinked = method_name != CCLuaMethod.METHOD_IS_LINKED;
        boolean requireScreen = method_name != CCLuaMethod.METHOD_IS_LINKED &&
                                method_name != CCLuaMethod.METHOD_GET_SCREEN_POS;

        if (requireLinked && !this.isLinked()) {
            return null;
        }

        TileEntityScreen screenEntity = getConnectedScreenEx();
        if (requireScreen && screenEntity == null) {
            return null;
        }

        // lots of our methods take 1 string arg from lua as a param, so we check here
        String luaParam1String = null;
        if (args.length >= 1 && (args[0] instanceof String)) {
            luaParam1String = (String)args[0];
        }

        switch (method_name) {
            case METHOD_IS_LINKED:
                return new Object[]{this.isLinked()};
            case METHOD_GET_SCREEN_POS:
                return new Object[]{this.screenPos.x, this.screenPos.y, this.screenPos.z};
            case METHOD_CLICK:
                if ((args.length < 2) || (!(args[0] instanceof Double)) || (!(args[1] instanceof Double))) {
                    return null;
                }

                float x = ((Double)args[0]).floatValue();
                float y = ((Double)args[1]).floatValue();
                screenEntity.click(screenSide, new Vector2i((int)(x), (int)(y)));
                return null;
            case METHOD_TYPE:
                if (luaParam1String == null) { return null; }
                screenEntity.type(this.screenSide, luaParam1String, null);
                return null;
            case METHOD_GETURL:
                return new Object[]{screenEntity.getScreen(screenSide).url};
            case METHOD_SETURL:
                if (luaParam1String == null) { return null; }
                screenEntity.setScreenURL(this.screenSide, luaParam1String);
                return null;
            case METHOD_SHUTDOWN:
                screenEntity.removeScreen(this.screenSide);
                return null;
            case METHOD_RUNJS:
                if (luaParam1String == null) { return null; }
                try {
                    screenEntity.evalJS(this.screenSide, luaParam1String);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return null;
            default:
                return null;
        }
    }
}