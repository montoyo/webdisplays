/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.miniserv.OutgoingPacket;
import net.montoyo.wd.miniserv.PacketID;

public class ClientTaskGetQuota extends ClientTask<ClientTaskGetQuota> {

    private long quota;
    private long maxQuota;

    public ClientTaskGetQuota() {
        runCallbackOnMcThread = true;
    }

    @Override
    public void start() {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.QUOTA.ordinal());
        client.sendPacket(pkt);
    }

    @Override
    public void abort() {
    }

    public void onQuotaData(long q, long m) {
        quota = q;
        maxQuota = m;
        client.nextTask();
    }

    public long getMaxQuota() {
        return maxQuota;
    }

    public long getQuota() {
        return quota;
    }

}
