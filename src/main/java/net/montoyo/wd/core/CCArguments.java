/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import java.util.List;
import java.util.Map;

public class CCArguments implements IComputerArgs {

    private final Object[] args;

    public CCArguments(Object[] args) {
        this.args = args;
    }

    @Override
    public String checkString(int i) {
        checkIndex(i, "string");

        Object obj = args[i];
        if(!(obj instanceof String))
            throw typeError(i, "string", obj);

        return (String) obj;
    }

    @Override
    public int checkInteger(int i) {
        checkIndex(i, "number");

        Object obj = args[i];
        int ret;

        if(obj instanceof Integer)
            ret = (int) obj;
        else if(obj instanceof Double)
            ret = ((Double) obj).intValue();
        else if(obj instanceof Float)
            ret = ((Float) obj).intValue();
        else
            throw typeError(i, "number", obj);

        return ret;
    }

    @Override
    public Map checkTable(int i) {
        checkIndex(i, "table");

        Object obj = args[i];
        if(!(obj instanceof Map))
            throw typeError(i, "table", args[i]);

        return (Map) obj;
    }

    private void checkIndex(int idx, String want) {
        if(idx < 0 || idx >= args.length)
            typeError(idx, want, null);
    }

    private static IllegalArgumentException typeError(int idx, String want, Object got) {
        return new IllegalArgumentException("bad argument #" + (idx + 1) + " (" + want + " expected, got " + luaTypeName(got) + ")");
    }

    private static String luaTypeName(Object obj) {
        if(obj == null)
            return "nil";

        Class<?> cls = obj.getClass();
        if(cls == Boolean.class || cls == Boolean.TYPE)
            return "boolean";
        else if(cls == Integer.class || cls == Integer.TYPE || cls == Double.class || cls == Double.TYPE || cls == Float.class || cls == Float.TYPE)
            return "number";
        else if(cls == String.class)
            return "string";
        else if(Map.class.isAssignableFrom(cls))
            return "table";
        else
            return cls.getSimpleName();
    }

    @Override
    public int count() {
        return args.length;
    }

}
