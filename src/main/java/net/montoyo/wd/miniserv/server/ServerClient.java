/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.server;

import net.montoyo.wd.miniserv.*;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.*;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Consumer;

public class ServerClient extends AbstractClient {

    private static final byte[] FILE_UPLOAD_BUFFER = new byte[65535];

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
    private long lastDataTime;

    ServerClient(SocketChannel s, Selector ss) {
        socket = s;
        selector = ss;

        try {
            selKey = socket.register(selector, SelectionKey.OP_READ);
        } catch(ClosedChannelException ex) {}

        lastDataTime = System.currentTimeMillis();
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

    public boolean hasTimedOut(long now) {
        return now - lastDataTime >= Constants.CLIENT_TIMEOUT;
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
        dis.readFully(mac);

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

            if(Util.isFileNameInvalid(fname)) {
                Log.warning("Client %s tried to upload a file with a bad name", uuid.toString());
                rep.writeByte(Constants.FUPA_STATUS_BAD_NAME);
            } else if(size <= 0) {
                Log.warning("Client %s tried to upload a file an invalid size", uuid.toString());
                rep.writeByte(Constants.FUPA_STATUS_INVALID_SIZE);
            } else if(quota + size > Server.getInstance().getMaxQuota())
                rep.writeByte(Constants.FUPA_STATUS_EXCEEDS_QUOTA);
            else if(currentFile != null || sendingFile)
                rep.writeByte(Constants.FUPA_STATUS_OCCUPIED);
            else {
                File fle = new File(userDir, fname);

                if(fle.exists())
                    rep.writeByte(Constants.FUPA_STATUS_FILE_EXISTS);
                else {
                    try {
                        currentFile = new FileOutputStream(fle);
                        currentFileSize = 0;
                        currentFileExpectedSize = size;

                        rep.writeByte(0); //OK
                    } catch(IOException ex) {
                        Log.warningEx("IOException while uploading file %s from user %s", ex, fname, uuid.toString());
                        rep.writeByte(Constants.FUPA_STATUS_INTERNAL_ERROR);
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
                finishUpload(Constants.FUPA_STATUS_USER_ABORT);
                return;
            }

            currentFileSize += (long) len;
            if(currentFileSize > currentFileExpectedSize) {
                //Exceeded expected size
                finishUpload(Constants.FUPA_STATUS_LIER);
                return;
            }

            try {
                currentFile.write(getCurrentPacketRawData(), 3, len);
            } catch(IOException ex) {
                Log.warningEx("Client %s encountered an IOException while uploading some file", ex, uuid.toString());
                finishUpload(Constants.FUPA_STATUS_INTERNAL_ERROR);
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
            boolean doQuery = dis.readBoolean();

            OutgoingPacket rep = new OutgoingPacket();
            rep.writeByte(PacketID.GET_FILE.ordinal());

            if(Util.isFileNameInvalid(fname))
                rep.writeByte(Constants.GETF_STATUS_BAD_NAME);
            else {
                UUID user = new UUID(msb, lsb);
                File fle = new File(Server.getInstance().getDirectory(), user.toString() + File.separatorChar + fname);

                if(doQuery) {
                    try {
                        rep.setOnFinishAction(new SendFileCallback(fle));
                        rep.writeByte(0);
                        sendingFile = true;
                    } catch(FileNotFoundException ex) {
                        rep.writeByte(Constants.GETF_STATUS_NOT_FOUND);
                    }
                } else
                    rep.writeByte((fle.exists() && fle.isFile()) ? 0 : Constants.GETF_STATUS_NOT_FOUND);
            }

            sendPacket(rep);
        }
    }

    @PacketHandler(PacketID.QUOTA)
    public void handleQuota(DataInputStream dis) {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.QUOTA.ordinal());
        pkt.writeLong(quota);
        pkt.writeLong(Server.getInstance().getMaxQuota());

        sendPacket(pkt);
    }

    @PacketHandler(PacketID.LIST)
    public void handleList(DataInputStream dis) throws IOException {
        long msb = dis.readLong();
        long lsb = dis.readLong();
        File dir = new File(Server.getInstance().getDirectory(), (new UUID(msb, lsb)).toString());
        String[] list = null;

        if(dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();

            if(files != null)
                list = Arrays.stream(files).filter(f -> f.isFile() && !Util.isFileNameInvalid(f.getName())).map(File::getName).toArray(String[]::new);
        }

        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.LIST.ordinal());

        if(list == null)
            pkt.writeByte(0);
        else {
            pkt.writeByte(list.length);
            Arrays.stream(list).forEach(pkt::writeString);
        }

        sendPacket(pkt);
    }

    @PacketHandler(PacketID.DELETE)
    public void handleDelete(DataInputStream dis) throws IOException {
        String fname = readString(dis);
        int status = 2;

        if(!Util.isFileNameInvalid(fname)) {
            File file = new File(userDir, fname);

            if(file.exists() && file.isFile()) {
                try {
                    long sz = Files.size(file.toPath());

                    if(file.delete()) {
                        quota -= sz;
                        if(quota < 0)
                            quota = 0;

                        saveQuota();
                        status = 0;
                    }
                } catch(IOException ex) {
                    Log.errorEx("Couldn't get size of file %s of user %s for removal", ex, file.getAbsolutePath(), uuid.toString());
                }
            } else
                status = 1;
        }

        OutgoingPacket ret = new OutgoingPacket();
        ret.writeByte(PacketID.DELETE.ordinal());
        ret.writeByte(status);
        sendPacket(ret);
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
            saveQuota();
        }
    }

    private void saveQuota() {
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(new File(userDir, ".quota")));
            dos.writeLong(quota);
            Util.silentClose(dos);
        } catch(IOException ex) {
            Log.errorEx("Could not save quota data for user %s", ex, uuid.toString());
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
                    pkt.writeByte(Constants.GETF_STATUS_INTERNAL_ERROR); //Read error
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
            pkt.writeBytes(FILE_UPLOAD_BUFFER, 0, rd);
            sendPacket(pkt);
        }

    }

    @Override
    protected void onDataReceived() {
        lastDataTime = System.currentTimeMillis();
    }

    public String getUUIDString() {
        return (uuid == null) ? "[NOT IDENTIFIED YET]" : uuid.toString();
    }

}
