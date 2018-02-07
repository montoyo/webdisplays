/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

import java.io.*;
import java.util.function.Consumer;

public final class OutgoingPacket {

    private ByteArrayOutputStream baos;
    private DataOutputStream dos;
    private Consumer<OutgoingPacket> onFinish;

    public OutgoingPacket() {
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
    }

    public final void writeLong(long l) {
        try {
            dos.writeLong(l);
        } catch(IOException ex) {}
    }

    public final void writeInt(int i) {
        try {
            dos.writeInt(i);
        } catch(IOException ex) {}
    }

    public final void writeByte(int b) {
        try {
            dos.writeByte(b);
        } catch(IOException ex) {}
    }

    public final void writeShort(int s) {
        try {
            dos.writeShort(s);
        } catch(IOException ex) {}
    }

    public final void writeBoolean(boolean b) {
        try {
            dos.writeBoolean(b);
        } catch(IOException ex) {}
    }

    public final void writeBytes(byte[] data) {
        try {
            dos.write(data);
        } catch(IOException ex) {}
    }

    public final void writeBytes(byte[] data, int offset, int size) {
        try {
            dos.write(data, offset, size);
        } catch(IOException ex) {}
    }

    public final void writeString(String str) {
        byte[] bytes;
        try {
            bytes = str.getBytes("UTF-8");
        } catch(UnsupportedEncodingException ex) {
            return; //Meh, shouldn't happen
        }

        try {
            dos.writeShort(bytes.length);
            dos.write(bytes);
        } catch(IOException ex) {}
    }

    public final int writeStream(InputStream is, int max) throws IOException {
        final int origMax = max;
        final byte[] buf = new byte[8192];

        while(max > 0) {
            int read = is.read(buf, 0, (max < buf.length) ? max : buf.length);
            if(read <= 0)
                return origMax - max;

            dos.write(buf, 0, read);
            max -= read;
        }

        return origMax;
    }

    public final byte[] finish() {
        if(onFinish != null)
            onFinish.accept(this);

        byte[] bytes = baos.toByteArray();
        baos = null;
        dos = null;

        return bytes;
    }

    public final void setOnFinishAction(Consumer<OutgoingPacket> action) {
        onFinish = action;
    }

}
