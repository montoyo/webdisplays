/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.miniserv.Constants;
import net.montoyo.wd.miniserv.OutgoingPacket;
import net.montoyo.wd.miniserv.PacketID;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

public class ClientTaskCheckFile extends ClientTask<ClientTaskCheckFile> {

    private final UUID uuid;
    private final String fname;
    private int status = Constants.GETF_STATUS_INTERNAL_ERROR;

    public ClientTaskCheckFile(UUID id, String name) {
        uuid = id;
        fname = name;
        runCallbackOnMcThread = true;
    }

    @Override
    public void start() {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.GET_FILE.ordinal());
        pkt.writeLong(uuid.getMostSignificantBits());
        pkt.writeLong(uuid.getLeastSignificantBits());
        pkt.writeString(fname);
        pkt.writeBoolean(false);

        client.sendPacket(pkt);
    }

    @Override
    public void abort() {
    }

    public void onStatus(int s) {
        status = s;
        client.nextTask();
    }

    public int getStatus() {
        return status;
    }

    public String getURL() {
        try {
            return ((new StringBuilder("wd://"))).append(uuid.toString()).append('/').append(URLEncoder.encode(fname, "UTF-8")).toString();
        } catch(UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return "hi";
        }
    }

}
