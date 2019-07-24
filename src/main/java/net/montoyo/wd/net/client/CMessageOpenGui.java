/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.data.GuiData;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.utilities.Util;

@Message(messageId = 3, side = Side.CLIENT)
public class CMessageOpenGui implements IMessage, Runnable {

    private GuiData data;

    public CMessageOpenGui() {
    }

    public CMessageOpenGui(GuiData data) {
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        String name = ByteBufUtils.readUTF8String(buf);
        Class<? extends GuiData> cls = GuiData.classOf(name);

        if(cls == null) {
            Log.error("Could not create GuiData of type %s because it doesn't exist!", name);
            return;
        }

        data = (GuiData) Util.unserialize(buf, cls);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, data.getName());
        Util.serialize(buf, data);
    }

    @Override
    public void run() {
        WebDisplays.PROXY.displayGui(data);
    }

}
