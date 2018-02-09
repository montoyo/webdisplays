/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

public enum PacketID {

    INIT_CONN,          //C->S
    AUTHENTICATE,       //C->S and S->C
    PING,               //C->S and S->C
    BEGIN_FILE_UPLOAD,  //C->S
    FILE_PART,          //C->S and S->C
    FILE_STATUS,        //S->C
    GET_FILE,           //C->S
    QUOTA,              //C->S and S->C
    LIST,               //C->S and S->C
    DELETE;             //C->S and S->C

    public static PacketID fromInt(int i) {
        PacketID[] values = values();
        return (i < 0 || i >= values.length) ? null : values[i];
    }

}
