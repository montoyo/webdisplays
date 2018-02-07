/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

import java.nio.ByteBuffer;

public final class PacketWriter {

    private final byte[] sizeBytes = new byte[4];
    private byte[] packet;
    private int pos = 0;
    private boolean needToWriteSize = true;

    public final boolean writeTo(ByteBuffer bb) {
        if(packet == null)
            return true; //Next packet please

        if(needToWriteSize) {
            if(writeByteArray(bb, sizeBytes)) {
                needToWriteSize = false;
                pos = 0;
            } else
                return false;
        }

        if(writeByteArray(bb, packet)) {
            packet = null;
            return true;
        } else
            return false;
    }

    private boolean writeByteArray(ByteBuffer dst, byte[] src) {
        int remaining = src.length - pos;
        int written = (dst.remaining() >= remaining) ? remaining : dst.remaining();
        dst.put(src, pos, written);

        pos += written;
        return (pos >= src.length);
    }

    public final void reset(byte[] pkt) {
        final int len = pkt.length + 4;
        sizeBytes[0] = (byte) ((len >> 24) & 0xFF);
        sizeBytes[1] = (byte) ((len >> 16) & 0xFF);
        sizeBytes[2] = (byte) ((len >>  8) & 0xFF);
        sizeBytes[3] = (byte) ( len        & 0xFF);

        packet = pkt;
        pos = 0;
        needToWriteSize = true;
    }

    public final void clear() {
        packet = null;
        pos = 0;
        needToWriteSize = true;
    }

}
