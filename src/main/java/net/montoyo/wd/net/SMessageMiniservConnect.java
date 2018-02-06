/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

@Message(messageId = 10, side = Side.SERVER)
public class SMessageMiniservConnect implements IMessage {

    private byte[] modulus;
    private byte[] exponent;

    public SMessageMiniservConnect() {
    }

    public SMessageMiniservConnect(byte[] mod, byte[] exp) {
        modulus = mod;
        exponent = exp;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int sz = buf.readByte() & 0xFF;
        modulus = new byte[sz];
        buf.readBytes(modulus);

        sz = buf.readByte() & 0xFF;
        exponent = new byte[sz];
        buf.readBytes(exponent);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(modulus.length);
        buf.writeBytes(modulus);
        buf.writeByte(exponent.length);
        buf.writeBytes(exponent);
    }

    public static class Handler implements IMessageHandler<SMessageMiniservConnect, IMessage> {

        @Override
        public IMessage onMessage(SMessageMiniservConnect message, MessageContext ctx) {
            //TODO: Generate key
            return null;
        }

    }

}
