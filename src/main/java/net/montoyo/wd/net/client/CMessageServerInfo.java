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

@Message(messageId = 12, side = Side.CLIENT)
public class CMessageServerInfo implements IMessage, Runnable {

    private int miniservPort;

    public CMessageServerInfo() {
    }

    public CMessageServerInfo(int msPort) {
        miniservPort = msPort;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        miniservPort = buf.readShort() & 0xFFFF;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeShort(miniservPort);
    }

    @Override
    public void run() {
        WebDisplays.PROXY.setMiniservClientPort(miniservPort);

        if(miniservPort > 0)
            WebDisplays.NET_HANDLER.sendToServer(Client.getInstance().beginConnection());
    }

}
