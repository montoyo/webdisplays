/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.utilities.Log;

@Message(messageId = 9, side = Side.CLIENT)
public class CMessageJSResponse implements IMessage, Runnable {

    private int id;
    private JSServerRequest type;
    private boolean success;
    private byte[] data;
    private int errCode;
    private String errString;

    public CMessageJSResponse() {
    }

    public CMessageJSResponse(int id, JSServerRequest t, byte[] d) {
        this.id = id;
        type = t;
        success = true;
        data = d;
    }

    public CMessageJSResponse(int id, JSServerRequest t, int code, String err) {
        this.id = id;
        type = t;
        success = false;
        errCode = code;
        errString = err;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = buf.readInt();
        type = JSServerRequest.fromID(buf.readByte());
        success = buf.readBoolean();

        if(success) {
            data = new byte[buf.readByte()];
            buf.readBytes(data);
        } else {
            errCode = buf.readInt();
            errString = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(id);
        buf.writeByte(type.ordinal());
        buf.writeBoolean(success);

        if(success) {
            buf.writeByte(data.length);
            buf.writeBytes(data); //TODO: Eventually compress this data
        } else {
            buf.writeInt(errCode);
            ByteBufUtils.writeUTF8String(buf, errString);
        }
    }

    @Override
    public void run() {
        try {
            if(success)
                WebDisplays.PROXY.handleJSResponseSuccess(id, type, data);
            else
                WebDisplays.PROXY.handleJSResponseError(id, type, errCode, errString);
        } catch(Throwable t) {
            Log.warningEx("Could not handle JS response", t);
        }
    }

}
