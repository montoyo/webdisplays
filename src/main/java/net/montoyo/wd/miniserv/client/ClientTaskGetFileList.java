/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.miniserv.OutgoingPacket;
import net.montoyo.wd.miniserv.PacketID;

import java.util.UUID;

public class ClientTaskGetFileList extends ClientTask<ClientTaskGetFileList> {

    private final UUID user;
    private String[] files;

    public ClientTaskGetFileList(UUID user) {
        this.user = user;
        runCallbackOnMcThread = true;
    }

    @Override
    public void start() {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.LIST.ordinal());
        pkt.writeLong(user.getMostSignificantBits());
        pkt.writeLong(user.getLeastSignificantBits());
        client.sendPacket(pkt);
    }

    @Override
    public void abort() {
    }

    public void onFileList(String[] files) {
        this.files = files;
        client.nextTask();
    }

    public String[] getFileList() {
        return files;
    }

}
