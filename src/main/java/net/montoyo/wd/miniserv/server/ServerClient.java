/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.server;

import net.montoyo.wd.miniserv.OutgoingPacket;
import net.montoyo.wd.miniserv.PacketID;
import net.montoyo.wd.miniserv.PacketReader;
import net.montoyo.wd.miniserv.PacketWriter;
import net.montoyo.wd.utilities.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

public class ServerClient {

    private final SocketChannel socket;
    private final Selector selector;
    private SelectionKey writeKey;
    private boolean remove;
    private final ByteBuffer sendBuffer = ByteBuffer.allocateDirect(8192);
    private final ArrayDeque<OutgoingPacket> sendQueue = new ArrayDeque<>();
    private final PacketReader packetReader = new PacketReader();
    private final PacketWriter packetWriter = new PacketWriter();

    ServerClient(SocketChannel s, Selector ss) {
        socket = s;
        selector = ss;
        sendBuffer.limit(0); //Set empty
    }

    void readyRead(ByteBuffer bb) {
        while(bb.remaining() > 0) {
            if(packetReader.readFrom(bb)) { //End of packet
                byte[] pkt = packetReader.getPacketData();
                if(pkt != null) {
                    try {
                        handlePacket(pkt);
                    } catch(IOException ex) {
                        Log.warningEx("IOException while trying to handle packet", ex);
                    }
                }

                packetReader.reset();
            }
        }
    }

    private void handlePacket(byte[] pkt) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pkt));
        PacketID pid = PacketID.fromInt(dis.readByte());

        if(pid == null) {
            Log.warning("Caught packet with invalid ID from client");
            return;
        }

        OutgoingPacket response = null;

        switch(pid) {
            case PING:
                response = new OutgoingPacket();
                response.writeByte(PacketID.PING.ordinal());
                break;

            case BEGIN_FILE_UPLOAD:
                break;

            case FILE_PART:
                break;

            case GET_FILE:
                break;
        }

        if(response != null)
            sendPacket(response);
    }

    void readyWrite() throws Throwable {
        if(sendBuffer.remaining() > 0 || fillSendBuffer()) {
            if(socket.write(sendBuffer) < 0)
                remove = true;
        } else if(writeKey != null) {
            writeKey.cancel();
            writeKey = null;
        }
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

    private boolean fillSendBuffer() {
        sendBuffer.clear();

        do {
            if(packetWriter.writeTo(sendBuffer)) {
                OutgoingPacket pkt = sendQueue.poll();
                if(pkt == null)
                    return sendBuffer.remaining() > 0;

                packetWriter.reset(pkt.finish());
            }
        } while(sendBuffer.remaining() > 0);

        return true;
    }

    public void sendPacket(OutgoingPacket pkt) {
        sendQueue.offer(pkt);

        if(writeKey == null) {
            try {
                writeKey = socket.register(selector, SelectionKey.OP_WRITE);
            } catch(ClosedChannelException ex) {
                Log.warningEx("Couldn't send packet", ex);
            }
        }
    }

}
