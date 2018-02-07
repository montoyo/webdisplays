/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

import net.montoyo.wd.utilities.Log;

import java.nio.ByteBuffer;

public final class PacketReader {

    private final byte[] sizeArray = new byte[4];
    private byte[] packetData;
    private int pos = 0;
    private boolean needSize = true;

    public final boolean readFrom(ByteBuffer buf) {
        if(needSize) {
            //Read packet size
            if(readByteArray(sizeArray, buf)) {
                int packetSize = ((sizeArray[0] & 0xFF) << 24) | ((sizeArray[1] & 0xFF) << 16) | ((sizeArray[2] & 0xFF) << 8) | (sizeArray[3] & 0xFF);
                needSize = false;
                pos = 0;

                if(packetSize < 5 || packetSize > 70000) {
                    Log.warning("Got invalid packet of size %d, things won't go well...", packetSize);
                    return true; //Abort packet reading
                }

                packetSize -= 4;
                packetData = new byte[packetSize];
            } else
                return false;
        }

        return readByteArray(packetData, buf);
    }

    private boolean readByteArray(byte[] dst, ByteBuffer src) {
        int remaining = dst.length - pos;
        int read = (src.remaining() >= remaining) ? remaining : src.remaining();

        src.get(dst, pos, read);
        pos += read;

        return (pos >= dst.length);
    }

    public final byte[] getPacketData() {
        return packetData;
    }

    public final void reset() {
        packetData = null;
        pos = 0;
        needSize = true;
    }

}
