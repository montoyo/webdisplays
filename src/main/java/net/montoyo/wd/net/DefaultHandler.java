/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.montoyo.wd.WebDisplays;

public class DefaultHandler implements IMessageHandler<IMessage, IMessage> {

    @Override
    public IMessage onMessage(IMessage message, MessageContext ctx) {
        WebDisplays.PROXY.enqueue((Runnable) message);
        return null;
    }

}
