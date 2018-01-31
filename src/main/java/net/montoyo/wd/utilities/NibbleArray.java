/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

public final class NibbleArray {

    private final byte[] data;

    public NibbleArray(int count) {
        if((count & 1) != 0)
            count++;

        data = new byte[count >> 1];
    }

    public NibbleArray(byte[] d) {
        data = d;
    }

    public final int get(int idx) {
        if((idx & 1) == 0)
            return (data[idx >> 1] >> 4) & 0x0F; //MSB
        else
            return data[idx >> 1] & 0x0F; //LSB
    }

    public final void set(int idx, int val) {
        val &= 0x0F;

        if((idx & 1) == 0) {
            idx >>= 1;
            data[idx] = (byte) ((data[idx] & 0x0F) | (val << 4)); //MSB
        } else {
            idx >>= 1;
            data[idx] = (byte) ((data[idx] & 0xF0) | val); //LSB
        }
    }

    public byte[] copyBytes() {
        byte[] ret = new byte[data.length];
        System.arraycopy(data, 0, ret, 0, data.length);
        return ret;
    }

}
