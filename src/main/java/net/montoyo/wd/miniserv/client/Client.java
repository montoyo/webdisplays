/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.miniserv.*;
import net.montoyo.wd.net.SMessageMiniservConnect;
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
    private boolean running = true;
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(8192);
    private final Thread thread = new Thread(this);
    private boolean authenticated = false;

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

    public byte[] authenticate(byte[] challenge) {
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
        address = addr;
        thread.start();
    }

    @Override
    public void run() {
        try {
            socket = SocketChannel.open();
            socket.connect(address);
            socket.configureBlocking(false);

            selector = Selector.open();
            socket.register(selector, SelectionKey.OP_READ);
        } catch(IOException ex) {
            Log.errorEx("Couldn't start client", ex);
            return;
        }

        while(running) {
            try {
                unsafeLoop();
            } catch(Throwable t) {
                Log.errorEx("MiniServ Client crashed", t);
                break;
            }
        }

        Util.silentClose(socket);
        Util.silentClose(selector);
    }

    private void unsafeLoop() throws Throwable {
        selector.select();

        for(SelectionKey key: selector.selectedKeys()) {
            if(key.isReadable()) {
                readBuffer.clear();
                int rd = socket.read(readBuffer);

                if(rd <= 0) {
                    Log.warning("Connection was closed, stopping...");
                    running = false;
                } else {
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
    }

    @Override
    protected void onWriteError() {
        running = false;
        Log.error("Write error, stopping...");
    }

    @PacketHandler(PacketID.AUTHENTICATE)
    public void handleAuth(DataInputStream dis) {
        //TODO: Do some stuff
    }

}
