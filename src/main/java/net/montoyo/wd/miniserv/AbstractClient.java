/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

import net.montoyo.wd.utilities.Log;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
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
    protected SelectionKey selKey;

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
    }

    protected abstract void onWriteError();

    public final void readyRead(ByteBuffer bb) {
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

        onDataReceived();
    }

    public final void readyWrite() throws Throwable {
        if(sendBuffer.remaining() > 0 || fillSendBuffer()) {
            int sent = socket.write(sendBuffer);

            if(sent < 0)
                onWriteError();
            else if(sent > 0)
                onDataSent();
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
                        selKey.interestOps(SelectionKey.OP_READ); //Remove write op
                        break;
                    }
                }

                packetWriter.reset(pkt.finish());
            }
        } while(sendBuffer.remaining() > 0);

        int pos = sendBuffer.position();
        sendBuffer.position(0);
        sendBuffer.limit(pos);
        return pos > 0;
    }

    public final void sendPacket(OutgoingPacket pkt) {
        synchronized(sendQueue) {
            sendQueue.offer(pkt);

            if(selKey.isValid() && (selKey.interestOps() & SelectionKey.OP_WRITE) == 0) {
                selKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                selector.wakeup(); //Is this needed?
            }
        }
    }

    protected static String readString(DataInputStream dis) throws IOException {
        int len = dis.readShort() & 0xFFFF;
        byte[] str = new byte[len];
        dis.readFully(str);

        try {
            return new String(str, "UTF-8");
        } catch(UnsupportedEncodingException ex) {
            throw new RuntimeException(ex); //Shouldn't happen
        }
    }

    protected final byte[] getCurrentPacketRawData() {
        return packetReader.getPacketData();
    }

    protected final void clearSendQueue() {
        synchronized(sendQueue) {
            packetWriter.clear();
            sendQueue.clear();
        }
    }

    protected void onDataReceived() {
    }

    protected void onDataSent() {
    }

}
