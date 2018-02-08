/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.utilities.Log;

@Message(messageId = 11, side = Side.CLIENT)
public class CMessageMiniservKey implements IMessage, Runnable {

    private byte[] encryptedKey;

    public CMessageMiniservKey() {
    }

    public CMessageMiniservKey(byte[] key) {
        encryptedKey = key;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        encryptedKey = new byte[buf.readShort() & 0xFFFF];
        buf.readBytes(encryptedKey);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(encryptedKey.length);
        buf.writeBytes(encryptedKey);
    }

    @Override
    public void run() {
        if(Client.getInstance().decryptKey(encryptedKey)) {
            Log.info("Successfully received and decrypted key, starting miniserv client...");
            WebDisplays.PROXY.startMiniservClient();
        }
    }

}
