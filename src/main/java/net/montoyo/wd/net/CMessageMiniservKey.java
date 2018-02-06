/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@Message(messageId = 11, side = Side.CLIENT)
public class CMessageMiniservKey implements IMessage {

    private byte[] encryptedKey;

    public CMessageMiniservKey() {
    }

    public CMessageMiniservKey(byte[] key) {
        encryptedKey = key;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        encryptedKey = new byte[buf.readByte() & 0xFF];
        buf.readBytes(encryptedKey);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(encryptedKey.length);
        buf.writeBytes(encryptedKey);
    }

    public static class Handler implements IMessageHandler<CMessageMiniservKey, IMessage> {

        @Override
        public IMessage onMessage(CMessageMiniservKey message, MessageContext ctx) {
            //TODO: Start client thread
            return null;
        }

    }

}
