/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import io.netty.buffer.ByteBuf;
import net.montoyo.wd.utilities.Util;

public enum JSServerRequest {

    CLEAR_REDSTONE,
    SET_REDSTONE_AT(Integer.class, Integer.class, Boolean.class);

    private final Class[] requestTypes;

    JSServerRequest(Class<?> ... requestTypes) {
        this.requestTypes = requestTypes;
    }

    public static JSServerRequest fromID(int id) {
        JSServerRequest[] values = values();
        return (id >= 0 && id < values.length) ? values[id] : null;
    }

    public boolean serialize(ByteBuf buf, Object[] data) {
        if(data.length != requestTypes.length)
            return false;

        for(int i = 0; i < data.length; i++) {
            if(data[i].getClass() != requestTypes[i])
                return false;

            Util.serialize(buf, data[i]);
        }

        return true;
    }

    public Object[] deserialize(ByteBuf buf) {
        Object[] ret = new Object[requestTypes.length];

        try {
            for(int i = 0; i < requestTypes.length; i++)
                ret[i] = Util.unserialize(buf, requestTypes[i]);
        } catch(Throwable t) {
            t.printStackTrace();
            return null;
        }

        return ret;
    }

}
