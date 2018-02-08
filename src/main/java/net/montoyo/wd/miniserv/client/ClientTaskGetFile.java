/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.miniserv.Constants;
import net.montoyo.wd.miniserv.OutgoingPacket;
import net.montoyo.wd.miniserv.PacketID;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ClientTaskGetFile extends ClientTask<ClientTaskGetFile> {

    private final UUID uuid;
    private final String fname;

    private int response;
    private boolean hasResponse;
    private final ReentrantLock responseLock = new ReentrantLock();
    private final Condition gotResponse = responseLock.newCondition();

    private final ReentrantLock dataLock = new ReentrantLock();
    private final Condition dataChanged = dataLock.newCondition();
    private byte[] data;
    private int dataLen;

    public ClientTaskGetFile(UUID id, String name) {
        uuid = id;
        fname = name;
    }

    @Override
    public void start() {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.GET_FILE.ordinal());
        pkt.writeLong(uuid.getMostSignificantBits());
        pkt.writeLong(uuid.getLeastSignificantBits());
        pkt.writeString(fname);
        pkt.writeBoolean(true);

        client.sendPacket(pkt);
    }

    @Override
    public void abort() {
        responseLock.lock();
        if(!hasResponse) {
            response = Constants.GETF_STATUS_CONNECTION_LOST;
            hasResponse = true;
            gotResponse.signal();
        }

        responseLock.unlock();
        onData(new byte[0], -1); //This will trigger an error
    }

    public void onGetFileResponse(int status) {
        boolean triggerError = false;
        responseLock.lock();

        if(hasResponse) {
            if(status != 0)
                triggerError = true;
        } else {
            response = status;
            hasResponse = true;
            gotResponse.signal();

            if(response != 0)
                client.nextTask();
        }

        responseLock.unlock();

        if(triggerError)
            onData(new byte[0], -1);
    }

    public int waitForResponse() {
        responseLock.lock();
        long t = System.currentTimeMillis();

        while(!hasResponse) {
            if(System.currentTimeMillis() - t > 10000) {
                responseLock.unlock();
                cancel();
                return Constants.GETF_STATUS_TIMED_OUT;
            }

            try {
                gotResponse.await(100, TimeUnit.MILLISECONDS);
            } catch(InterruptedException ex) {}
        }

        responseLock.unlock();
        return response;
    }

    public void onData(byte[] data, int len) {
        dataLock.lock();
        while(this.data != null) {
            try {
                dataChanged.await();
            } catch(InterruptedException ex) {}
        }

        this.data = data;
        dataLen = len;
        dataChanged.signal();
        dataLock.unlock();

        if(len <= 0)
            client.nextTask();
    }

    public byte[] waitForData() {
        dataLock.lock();
        long t = System.currentTimeMillis();

        while(data == null) {
            if(System.currentTimeMillis() - t > 10000) {
                data = new byte[0];
                dataLen = -1;
                dataLock.unlock();
                cancel();
                return data;
            }

            try {
                dataChanged.await(100, TimeUnit.MILLISECONDS);
            } catch(InterruptedException ex) {}
        }

        dataLock.unlock(); //This won't change until data is null again
        return data;
    }

    public int getDataLength() {
        return dataLen;
    }

    public void nextData() {
        dataLock.lock();
        data = null;
        dataChanged.signal();
        dataLock.unlock();
    }

    public String getFileName() {
        return fname;
    }

}
