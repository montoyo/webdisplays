/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

import net.montoyo.wd.utilities.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;

public abstract class AbstractClient {

    private final ByteBuffer sendBuffer = ByteBuffer.allocateDirect(8192);
    private final ArrayDeque<OutgoingPacket> sendQueue = new ArrayDeque<>();
    private final PacketReader packetReader = new PacketReader();
    private final PacketWriter packetWriter = new PacketWriter();
    private final Method[] packetHandlers = new Method[PacketID.values().length];
    protected SocketChannel socket;
    protected Selector selector;
    private SelectionKey writeKey;

    public AbstractClient() {
        sendBuffer.limit(0);

        Method[] methods = getClass().getMethods();
        for(Method m: methods) {
            PacketHandler ph = m.getAnnotation(PacketHandler.class);

            if(ph != null) {
                if(packetHandlers[ph.value().ordinal()] != null)
                    Log.warning("AbstractClient: several packet handlers for %s, ignoring %s", ph.value().toString(), m.getName());
                else if(m.getParameterCount() != 1 || m.getParameterTypes()[0] != DataInputStream.class)
                    Log.warning("AbstractClient: found invalid packet handler %s", m.getName());
                else
                    packetHandlers[ph.value().ordinal()] = m;
            }
        }

        for(int i = 0; i < packetHandlers.length; i++) {
            if(packetHandlers[i] == null)
                Log.warning("AbstractClient: no packet handler for %s", PacketID.fromInt(i).toString());
        }
    }

    protected abstract void onWriteError();

    public void readyRead(ByteBuffer bb) {
        while(bb.remaining() > 0) {
            if(packetReader.readFrom(bb)) { //End of packet
                byte[] pkt = packetReader.getPacketData();

                if(pkt != null) {
                    try {
                        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pkt));
                        int pid = dis.readByte() & 0xFF;

                        if(pid >= packetHandlers.length)
                            Log.error("Caught invalid packet ID %d", pid);
                        else if(packetHandlers[pid] != null) {
                            try {
                                packetHandlers[pid].invoke(this, dis); //This is slow, I know... sorry
                            } catch(IllegalAccessException ex) {
                                Log.errorEx("This shouldn't have happened", ex);
                            } catch(InvocationTargetException ex) {
                                Log.warningEx("Caught exception while handling packet %d", ex.getTargetException(), pid);
                            }
                        }
                    } catch(IOException ex) {
                        Log.warningEx("IOException while trying to handle packet", ex);
                    }
                }

                packetReader.reset();
            }
        }
    }

    public void readyWrite() throws Throwable {
        if(sendBuffer.remaining() > 0 || fillSendBuffer()) {
            if(socket.write(sendBuffer) < 0)
                onWriteError();
        }
    }

    private boolean fillSendBuffer() {
        sendBuffer.clear();

        do {
            if(packetWriter.writeTo(sendBuffer)) {
                OutgoingPacket pkt;
                synchronized(sendQueue) {
                    pkt = sendQueue.poll();

                    if(pkt == null) {
                        if(writeKey != null) {
                            writeKey.cancel();
                            writeKey = null;
                        }

                        return sendBuffer.remaining() > 0;
                    }
                }

                packetWriter.reset(pkt.finish());
            }
        } while(sendBuffer.remaining() > 0);

        return true;
    }

    public void sendPacket(OutgoingPacket pkt) {
        synchronized(sendQueue) {
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

}
