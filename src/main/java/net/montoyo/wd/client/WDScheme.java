/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.montoyo.mcef.api.IScheme;
import net.montoyo.mcef.api.ISchemeResponseData;
import net.montoyo.mcef.api.ISchemeResponseHeaders;
import net.montoyo.mcef.api.SchemePreResponse;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.miniserv.Constants;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.miniserv.client.ClientTaskGetFile;
import net.montoyo.wd.utilities.Util;

import java.util.UUID;

public class WDScheme implements IScheme {

    private ClientTaskGetFile task;

    @Override
    public SchemePreResponse processRequest(String url) {
        url = url.substring("wd://".length());

        int pos = url.indexOf('/');
        if(pos < 0)
            return SchemePreResponse.NOT_HANDLED;

        String uuidStr = url.substring(0, pos);
        String fileStr = url.substring(pos + 1);

        if(uuidStr.isEmpty() || Util.isFileNameInvalid(fileStr))
            return SchemePreResponse.NOT_HANDLED;

        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch(IllegalArgumentException ex) {
            return SchemePreResponse.NOT_HANDLED; //Invalid UUID
        }

        task = new ClientTaskGetFile(uuid, fileStr);
        return Client.getInstance().addTask(task) ? SchemePreResponse.HANDLED_CONTINUE : SchemePreResponse.NOT_HANDLED;
    }

    @Override
    public void getResponseHeaders(ISchemeResponseHeaders resp) {
        int status = task.waitForResponse();

        if(status == 0) {
            //OK
            int extPos = task.getFileName().lastIndexOf('.');
            if(extPos >= 0) {
                String mime = ((ClientProxy) WebDisplays.PROXY).getMCEF().mimeTypeFromExtension(task.getFileName().substring(extPos + 1));

                if(mime != null)
                    resp.setMimeType(mime);
            }

            resp.setStatus(200);
            resp.setStatusText("OK");
            resp.setResponseLength(-1);
        } else if(status == Constants.GETF_STATUS_NOT_FOUND) {
            resp.setStatus(404);
            resp.setStatusText("Not Found");
            resp.setResponseLength(0);
        } else {
            resp.setStatus(500);
            resp.setStatusText("Internal Server Error");
            resp.setResponseLength(0);
        }
    }

    private byte[] dataToWrite;
    private int dataOffset;
    private int amountToWrite;

    @Override
    public boolean readResponse(ISchemeResponseData data) {
        if(dataToWrite == null) {
            dataToWrite = task.waitForData();
            dataOffset = 3; //packet ID + size
            amountToWrite = task.getDataLength();

            if(amountToWrite <= 0) {
                dataToWrite = null;
                data.setAmountRead(0);
                return false;
            }
        }

        int toWrite = data.getBytesToRead();
        if(toWrite > amountToWrite)
            toWrite = amountToWrite;

        System.arraycopy(dataToWrite, dataOffset, data.getDataArray(), 0, toWrite);
        data.setAmountRead(toWrite);

        dataOffset += toWrite;
        amountToWrite -= toWrite;

        if(amountToWrite <= 0) {
            task.nextData();
            dataToWrite = null;
        }

        return true;
    }

}
