/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.miniserv.server.ClientManager;
import net.montoyo.wd.miniserv.server.Server;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.net.client.CMessageMiniservKey;

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
        int sz = buf.readShort() & 0xFFFF;
        modulus = new byte[sz];
        buf.readBytes(modulus);

        sz = buf.readShort() & 0xFFFF;
        exponent = new byte[sz];
        buf.readBytes(exponent);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(modulus.length);
        buf.writeBytes(modulus);
        buf.writeShort(exponent.length);
        buf.writeBytes(exponent);
    }

    public static class Handler implements IMessageHandler<SMessageMiniservConnect, IMessage> {

        @Override
        public IMessage onMessage(SMessageMiniservConnect msg, MessageContext ctx) {
            ClientManager cliMgr = Server.getInstance().getClientManager();
            byte[] encKey = cliMgr.encryptClientKey(ctx.getServerHandler().player.getGameProfile().getId(), msg.modulus, msg.exponent);

            return encKey == null ? null : new CMessageMiniservKey(encKey);
        }

    }

}
