/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.montoyo.wd.net.client.*;
import net.montoyo.wd.net.server.*;

import java.lang.reflect.Modifier;
import java.util.ArrayList;

public abstract class Messages {

    private static final DefaultHandler DEFAULT_HANDLER = new DefaultHandler();
    private static final Class<? extends IMessage>[] MESSAGES;
    static {
        ArrayList<Class<? extends IMessage>> l = new ArrayList<>();
        l.add(CMessageAddScreen.class);
        l.add(SMessageRequestTEData.class);
        l.add(SMessageScreenCtrl.class);
        l.add(CMessageOpenGui.class);
        l.add(CMessageScreenUpdate.class);
        l.add(SMessageACQuery.class);
        l.add(CMessageACResult.class);
        l.add(SMessagePadCtrl.class);
        l.add(SMessageRedstoneCtrl.class);
        l.add(CMessageJSResponse.class);
        l.add(SMessageMiniservConnect.class);
        l.add(CMessageMiniservKey.class);
        l.add(CMessageServerInfo.class);
        l.add(CMessageCloseGui.class);

        MESSAGES = l.toArray(new Class[0]);
    }

    public static void registerAll(SimpleNetworkWrapper wrapper) {
        for(Class<? extends IMessage> md: MESSAGES) {
            Message data = md.getAnnotation(Message.class);
            if(data == null)
                throw new RuntimeException("Missing @Message annotation for message class " + md.getSimpleName());

            Class<?>[] classes = md.getClasses();
            Class<? extends IMessageHandler> handler = null;

            for(Class<?> cls: classes) {
                if(cls.getSimpleName().equals("Handler") && Modifier.isStatic(cls.getModifiers()) && IMessageHandler.class.isAssignableFrom(cls)) {
                    handler = (Class<? extends IMessageHandler>) cls;
                    break;
                }
            }

            IMessageHandler handlerInst;
            if(handler == null) {
                if(Runnable.class.isAssignableFrom(md))
                    handlerInst = DEFAULT_HANDLER;
                else
                    throw new RuntimeException("Could not find message handler for message " + md.getSimpleName());
            } else {
                try {
                    handlerInst = handler.newInstance();
                } catch(Throwable t) {
                    throw new RuntimeException("Could not instantiate message handler for message " + md.getSimpleName());
                }
            }

            wrapper.registerMessage(handlerInst, md, data.messageId(), data.side());
        }
    }

}
