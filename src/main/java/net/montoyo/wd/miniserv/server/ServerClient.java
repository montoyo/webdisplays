/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.server;

import net.montoyo.wd.miniserv.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

public class ServerClient extends AbstractClient {

    private boolean remove;
    private boolean isAuthenticated;

    ServerClient(SocketChannel s, Selector ss) {
        socket = s;
        selector = ss;
    }

    @Override
    protected void onWriteError() {
        remove = true;
    }

    void setShouldRemove() {
        remove = true;
    }

    public boolean shouldRemove() {
        return remove;
    }

    SocketChannel getChannel() {
        return socket;
    }

    @PacketHandler(PacketID.AUTHENTICATE)
    public void handleAuthPacket(DataInputStream dis) throws IOException {
        //TODO: Do some stuff
    }

    @PacketHandler(PacketID.PING)
    public void handlePing(DataInputStream dis) {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.PING.ordinal());
        sendPacket(pkt);
    }

}
