/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.minecraft.client.Minecraft;
import net.montoyo.wd.miniserv.*;
import net.montoyo.wd.net.server.SMessageMiniservConnect;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayDeque;
import java.util.UUID;

public class Client extends AbstractClient implements Runnable {

    private static Client instance;

    public static Client getInstance() {
        if(instance == null)
            instance = new Client();

        return instance;
    }

    private final SecureRandom random = new SecureRandom();
    private KeyPair keyPair;
    private byte[] key;
    private SocketAddress address;
    private volatile boolean running;
    private volatile boolean connected;
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(8192);
    private volatile Thread thread;
    private final UUID clientUUID = Minecraft.getMinecraft().player.getGameProfile().getId();
    private final ArrayDeque<ClientTask> tasks = new ArrayDeque<>();
    private ClientTask currentTask;
    private volatile boolean authenticated;
    private long lastPingTime;

    public SMessageMiniservConnect beginConnection() {
        if(keyPair == null) {
            try {
                KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
                keygen.initialize(KeyParameters.RSA_KEY_SIZE);
                keyPair = keygen.generateKeyPair();
            } catch(NoSuchAlgorithmException ex) {
                Log.warningEx("RSA is unsupported?!?!", ex);
                return null;
            }
        }

        RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();
        return new SMessageMiniservConnect(pubKey.getModulus().toByteArray(), pubKey.getPublicExponent().toByteArray());
    }

    public boolean decryptKey(byte[] encKey) {
        try {
            Cipher cipher = Cipher.getInstance(KeyParameters.RSA_CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate(), random);
            key = cipher.doFinal(encKey);
            return true;
        } catch(NoSuchAlgorithmException | NoSuchPaddingException ex) {
            Log.warningEx("%s unsupported...", ex, KeyParameters.RSA_CIPHER);
        } catch(InvalidKeyException ex) {
            Log.warningEx("The generated key is invalid...", ex);
        } catch(IllegalBlockSizeException | BadPaddingException ex) {
            Log.warningEx("Could not decrypt key", ex);
        }

        return false;
    }

    private byte[] authenticate(byte[] challenge) {
        try {
            Mac mac = Mac.getInstance(KeyParameters.MAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, KeyParameters.MAC_ALGORITHM));
            return mac.doFinal(challenge);
        } catch(NoSuchAlgorithmException ex) {
            Log.warningEx("%s unsupported...", ex, KeyParameters.MAC_ALGORITHM);
        } catch(InvalidKeyException ex) {
            Log.warningEx("The key given by the server is invalid", ex);
        }

        return null;
    }

    public void start(SocketAddress addr) {
        if(getRunning()) {
            Log.warning("Called Client.start() twice");
            return;
        }

        address = addr;
        thread = new Thread(this);
        thread.setName("MiniServClient");
        thread.setDaemon(true);

        synchronized(this) {
            running = true;
            connected = false;
        }

        thread.start();
    }

    public void stop() {
        if(getRunning()) {
            Thread thread = this.thread;
            synchronized(this) {
                running = false;

                if(connected)
                    selector.wakeup();
            }

            while(thread.isAlive()) {
                try {
                    thread.join();
                } catch(InterruptedException ex) { }
            }

            Log.info("Miniserv client stopped");
        }
    }

    private boolean getRunning() {
        boolean ret;
        synchronized(this) {
            ret = running;
        }

        return ret;
    }

    @Override
    public void run() {
        try {
            selector = Selector.open();
            socket = SocketChannel.open();
            socket.connect(address);
            socket.configureBlocking(false);

            selKey = socket.register(selector, SelectionKey.OP_READ);
        } catch(IOException ex) {
            Log.errorEx("Couldn't start client", ex);

            synchronized(this) {
                running = false;
            }

            return;
        }

        synchronized(this) {
            connected = true;
        }

        Log.info("Miniserv client connected!");

        OutgoingPacket connPacket = new OutgoingPacket();
        connPacket.writeByte(PacketID.INIT_CONN.ordinal());
        connPacket.writeLong(clientUUID.getMostSignificantBits());
        connPacket.writeLong(clientUUID.getLeastSignificantBits());
        sendPacket(connPacket);
        lastPingTime = System.currentTimeMillis();

        while(getRunning()) {
            try {
                unsafeLoop();
            } catch(Throwable t) {
                Log.errorEx("MiniServ Client crashed", t);
                break;
            }
        }

        synchronized(this) {
            connected = false;
            running = false;
            authenticated = false;
        }

        Util.silentClose(selector);
        Util.silentClose(socket);
        selector = null;
        socket = null;

        if(currentTask != null) {
            currentTask.abort();
            currentTask.onFinished();
            currentTask = null;
        }

        synchronized(tasks) {
            ClientTask task;

            while((task = tasks.poll()) != null) {
                task.abort();
                task.onFinished();
            }
        }

        clearSendQueue();
        thread = null;
    }

    private void unsafeLoop() throws Throwable {
        long timeBeforePing = Constants.CLIENT_PING_PERIOD - (System.currentTimeMillis() - lastPingTime);
        selector.select(Math.max(0, timeBeforePing));

        if(currentTask == null || currentTask.isCanceled())
            nextTask();

        for(SelectionKey key: selector.selectedKeys()) {
            if(key.isReadable()) {
                readBuffer.clear();
                int rd = socket.read(readBuffer);

                if(rd < 0) {
                    Log.warning("Connection was closed, stopping...");
                    running = false;
                } else if(rd > 0) {
                    readBuffer.position(0);
                    readBuffer.limit(rd);
                    readyRead(readBuffer);
                }
            }

            if(key.isWritable()) {
                try {
                    readyWrite();
                } catch(Throwable t) {
                    Log.errorEx("Caught error while sending data to miniserv, stopping...", t);
                    running = false;
                }
            }
        }

        long t = System.currentTimeMillis();
        if(t - lastPingTime >= Constants.CLIENT_PING_PERIOD) {
            OutgoingPacket pkt = new OutgoingPacket();
            pkt.writeByte(PacketID.PING.ordinal());
            sendPacket(pkt);
            lastPingTime = t;
        }
    }

    @Override
    protected void onWriteError() {
        running = false;
        Log.error("Write error, stopping...");
    }

    @PacketHandler(PacketID.AUTHENTICATE)
    public void handleAuth(DataInputStream dis) throws IOException {
        int len = dis.readByte();
        byte[] challenge = new byte[len];
        dis.readFully(challenge);
        byte[] mac = authenticate(challenge);

        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.AUTHENTICATE.ordinal());
        pkt.writeByte(mac.length);
        pkt.writeBytes(mac);
        sendPacket(pkt);

        Log.info("Miniserv client authenticated");

        synchronized(this) {
            authenticated = true;
        }
    }

    @PacketHandler(PacketID.BEGIN_FILE_UPLOAD)
    public void handleBeginUpload(DataInputStream dis) throws IOException {
        if(currentTask instanceof ClientTaskUploadFile)
            ((ClientTaskUploadFile) currentTask).onReceivedUploadStatus(dis.readByte());
    }

    @PacketHandler(PacketID.FILE_STATUS)
    public void handleFileStatus(DataInputStream dis) throws IOException {
        if(currentTask instanceof ClientTaskUploadFile)
            ((ClientTaskUploadFile) currentTask).onUploadFinishedStatus(dis.readByte());
    }

    @PacketHandler(PacketID.GET_FILE)
    public void handleGetFile(DataInputStream dis) throws IOException {
        if(currentTask instanceof ClientTaskGetFile)
            ((ClientTaskGetFile) currentTask).onGetFileResponse(dis.readByte());
        else if(currentTask instanceof ClientTaskCheckFile)
            ((ClientTaskCheckFile) currentTask).onStatus(dis.readByte());
    }

    @PacketHandler(PacketID.FILE_PART)
    public void handleFilePart(DataInputStream dis) throws IOException {
        if(currentTask instanceof ClientTaskGetFile) {
            int len = dis.readShort() & 0xFFFF;
            ((ClientTaskGetFile) currentTask).onData(getCurrentPacketRawData(), len);
        }
    }

    @PacketHandler(PacketID.QUOTA)
    public void handleQuota(DataInputStream dis) throws IOException {
        long q = dis.readLong();
        long m = dis.readLong();

        if(currentTask instanceof ClientTaskGetQuota)
            ((ClientTaskGetQuota) currentTask).onQuotaData(q, m);
    }

    @PacketHandler(PacketID.LIST)
    public void handleList(DataInputStream dis) throws IOException {
        int cnt = dis.readByte() & 0xFF;
        String[] files = new String[cnt];

        for(int i = 0; i < cnt; i++)
            files[i] = readString(dis);

        if(currentTask instanceof ClientTaskGetFileList)
            ((ClientTaskGetFileList) currentTask).onFileList(files);
    }

    @PacketHandler(PacketID.DELETE)
    public void handleDelete(DataInputStream dis) throws IOException {
        if(currentTask instanceof ClientTaskDeleteFile)
            ((ClientTaskDeleteFile) currentTask).onStatusPacket(dis.readByte());
    }

    public void nextTask() {
        if(currentTask != null)
            currentTask.onFinished();

        synchronized(tasks) {
            currentTask = tasks.poll();
        }

        if(currentTask != null)
            currentTask.start();
    }

    public boolean addTask(ClientTask task) {
        boolean cancel;
        synchronized(this) {
            cancel = !running || !authenticated;
        }

        if(cancel)
            return false;

        synchronized(tasks) {
            tasks.offer(task);
        }

        selector.wakeup();
        return true;
    }

    public void wakeup() {
        boolean conn;
        synchronized(this) {
            conn = connected;
        }

        if(conn)
            selector.wakeup();
    }

    @Override
    protected void onDataSent() {
        lastPingTime = System.currentTimeMillis();
    }

}
