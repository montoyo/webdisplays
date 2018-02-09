/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.miniserv.OutgoingPacket;
import net.montoyo.wd.miniserv.PacketID;

public class ClientTaskDeleteFile extends ClientTask<ClientTaskDeleteFile> {

    private final String fname;
    private int status;

    public ClientTaskDeleteFile(String fname) {
        this.fname = fname;
    }

    @Override
    public void start() {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.DELETE.ordinal());
        pkt.writeString(fname);
        client.sendPacket(pkt);
    }

    @Override
    public void abort() {
    }

    public void onStatusPacket(int s) {
        status = s;
        client.nextTask();
    }

    public int getStatus() {
        return status;
    }

}
