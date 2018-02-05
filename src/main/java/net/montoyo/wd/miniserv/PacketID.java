/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

public enum PacketID {

    PING,               //C->S and S->C
    BEGIN_FILE_UPLOAD,  //C->S
    FILE_PART,          //C->S and S->C
    GET_FILE;           //C->S

    public static PacketID fromInt(int i) {
        PacketID[] values = values();
        return (i < 0 || i >= values.length) ? null : values[i];
    }

}
