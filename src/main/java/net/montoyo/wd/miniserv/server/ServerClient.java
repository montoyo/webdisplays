/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.server;

import net.montoyo.wd.miniserv.*;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.*;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.UUID;
import java.util.function.Consumer;

public class ServerClient extends AbstractClient {

    private static final byte[] FILE_UPLOAD_BUFFER = new byte[65536];

    private boolean remove;
    private boolean isAuthenticated;
    private UUID uuid;
    private byte[] challenge;
    private File userDir;
    private long quota;
    private FileOutputStream currentFile;
    private long currentFileSize;
    private long currentFileExpectedSize;
    private boolean sendingFile; //!= receiving, which is handled by currentFile

    ServerClient(SocketChannel s, Selector ss) {
        socket = s;
        selector = ss;
    }

    @Override
    protected void onWriteError() {
        remove = true;
    }

    public void onConnect() {
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

    @PacketHandler(PacketID.INIT_CONN)
    public void handleInitConnPacket(DataInputStream dis) throws IOException {
        if(uuid != null) {
            Log.warning("A client tried to change his UUID?");
            return;
        }

        long msb = dis.readLong();
        long lsb = dis.readLong();
        UUID uuid = new UUID(msb, lsb);
        byte[] key = Server.getInstance().getClientManager().getClientKey(uuid);

        if(key == null) {
            Log.warning("Unkown client with UUID %s wanted to connect", uuid.toString());
            remove = true;
        } else {
            this.uuid = uuid;
            challenge = Server.getInstance().getClientManager().generateChallenge();

            OutgoingPacket pkt = new OutgoingPacket();
            pkt.writeByte(PacketID.AUTHENTICATE.ordinal());
            pkt.writeByte(challenge.length);
            pkt.writeBytes(challenge);

            sendPacket(pkt);
        }
    }

    @PacketHandler(PacketID.AUTHENTICATE)
    public void handleAuthPacket(DataInputStream dis) throws IOException {
        if(uuid == null) {
            Log.warning("A client tried to authenticate, but he didn't send the connection packet");
            remove = true;
            return;
        }

        int len = dis.readByte() & 0xFF;
        byte[] mac = new byte[len];

        if(Server.getInstance().getClientManager().verifyClient(uuid, challenge, mac)) {
            Log.info("Client with UUID %s authenticated successfully", uuid.toString());

            userDir = new File(Server.getInstance().getDirectory(), uuid.toString());
            if(!userDir.exists() && !userDir.mkdir())
                Log.warning("Could not create storage directory for user %s, things may go wrong...", uuid.toString());

            try {
                DataInputStream quotaDis = new DataInputStream(new FileInputStream(new File(userDir, ".quota")));
                quota = quotaDis.readLong();
                Util.silentClose(quotaDis);
            } catch(FileNotFoundException ex) {
                quota = 0;
            } catch(IOException ex) {
                Log.warningEx("Couldn't read quota for user %s, things may go wrong...", ex, uuid.toString());
                quota = Server.getInstance().getMaxQuota();
            }

            isAuthenticated = true;
        } else {
            Log.warning("Client with UUID %s failed to authenticate", uuid.toString());
            remove = true;
        }
    }

    @PacketHandler(PacketID.PING)
    public void handlePing(DataInputStream dis) {
        if(isAuthenticated) {
            OutgoingPacket pkt = new OutgoingPacket();
            pkt.writeByte(PacketID.PING.ordinal());
            sendPacket(pkt);
        }
    }

    @PacketHandler(PacketID.BEGIN_FILE_UPLOAD)
    public void handleBeginUpload(DataInputStream dis) throws IOException {
        if(isAuthenticated) {
            String fname = readString(dis);
            long size = dis.readLong();

            OutgoingPacket rep = new OutgoingPacket();
            rep.writeByte(PacketID.BEGIN_FILE_UPLOAD.ordinal());

            if(isFileNameInvalid(fname)) {
                Log.warning("Client %s tried to upload a file with a bad name", uuid.toString());
                rep.writeByte(1);
            } else if(size <= 0) {
                Log.warning("Client %s tried to upload a file an invalid size", uuid.toString());
                rep.writeByte(2);
            } else if(quota + size > Server.getInstance().getMaxQuota())
                rep.writeByte(3);
            else if(currentFile != null || sendingFile)
                rep.writeByte(4);
            else {
                File fle = new File(userDir, fname);

                if(fle.exists())
                    rep.writeByte(5);
                else {
                    try {
                        currentFile = new FileOutputStream(fle);
                        currentFileSize = 0;
                        currentFileExpectedSize = size;

                        rep.writeByte(0); //OK
                    } catch(IOException ex) {
                        Log.warningEx("IOException while uploading file %s from user %s", ex, fname, uuid.toString());
                        rep.writeByte(6);
                    }
                }
            }

            sendPacket(rep);
        }
    }

    @PacketHandler(PacketID.FILE_PART)
    public void handleFilePart(DataInputStream dis) throws IOException {
        if(isAuthenticated && currentFile != null) {
            int len = dis.readShort() & 0xFFFF;
            if(len <= 0) {
                //Aborted by user
                finishUpload(1);
                return;
            }

            currentFileSize += (long) len;
            if(currentFileSize > currentFileExpectedSize) {
                //Exceeded expected size
                finishUpload(2);
                return;
            }

            try {
                currentFile.write(getCurrentPacketRawData(), 3, len);
            } catch(IOException ex) {
                Log.warningEx("Client %s encountered an IOException while uploading some file", ex, uuid.toString());
                finishUpload(3);
                currentFileSize -= (long) len;
                return;
            }

            if(currentFileSize >= currentFileExpectedSize)
                finishUpload(0); //No error
        }
    }

    @PacketHandler(PacketID.GET_FILE)
    public void handleGetFile(DataInputStream dis) throws IOException {
        if(isAuthenticated && currentFile == null) {
            long msb = dis.readLong();
            long lsb = dis.readLong();
            String fname = readString(dis);

            OutgoingPacket rep = new OutgoingPacket();
            rep.writeByte(PacketID.GET_FILE.ordinal());

            if(isFileNameInvalid(fname))
                rep.writeByte(1);
            else {
                UUID user = new UUID(msb, lsb);
                File fle = new File(Server.getInstance().getDirectory(), user.toString() + File.separatorChar + fname);

                try {
                    rep.setOnFinishAction(new SendFileCallback(fle));
                    sendingFile = true;
                } catch(FileNotFoundException ex) {
                    rep.writeByte(2);
                }
            }

            sendPacket(rep);
        }
    }

    private static boolean isFileNameInvalid(String fname) {
        return fname.isEmpty() || fname.length() > 64 || fname.charAt(0) == '.' || fname.indexOf('/') >= 0 || fname.indexOf('\\') >= 0;
    }

    private void finishUpload(int status) {
        if(currentFile != null) {
            OutgoingPacket pkt = new OutgoingPacket();
            pkt.writeByte(PacketID.FILE_STATUS.ordinal());
            pkt.writeByte(status);
            sendPacket(pkt);

            Util.silentClose(currentFile);
            currentFile = null;

            quota += currentFileSize;

            try {
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(userDir, ".quota")));
                dos.writeLong(quota);
                Util.silentClose(dos);
            } catch(IOException ex) {
                Log.errorEx("Could not save quota data for user %s", ex, uuid.toString());
            }
        }
    }

    private class SendFileCallback implements Consumer<OutgoingPacket> {

        private final FileInputStream fis;

        private SendFileCallback(File fle) throws FileNotFoundException {
            fis = new FileInputStream(fle);
        }

        @Override
        public void accept(OutgoingPacket nocare) {
            int rd;

            do {
                try {
                    rd = fis.read(FILE_UPLOAD_BUFFER);
                } catch(IOException ex) {
                    Log.warningEx("Caught IOException while sending some file", ex);

                    OutgoingPacket pkt = new OutgoingPacket();
                    pkt.writeByte(PacketID.GET_FILE.ordinal());
                    pkt.writeByte(3); //Read error
                    sendPacket(pkt);

                    Util.silentClose(fis);
                    sendingFile = false;
                    return;
                }
            } while(rd == 0);

            OutgoingPacket pkt = new OutgoingPacket();
            if(rd < 0) {
                rd = 0; //EOF
                sendingFile = false;
            } else
                pkt.setOnFinishAction(this);

            pkt.writeByte(PacketID.FILE_PART.ordinal());
            pkt.writeShort(rd);
            pkt.writeBytes(FILE_UPLOAD_BUFFER);
            sendPacket(pkt);
        }

    }

}
