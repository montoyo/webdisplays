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
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.UUID;

public class WDScheme implements IScheme {

    private static final String ERROR_PAGE = "<!DOCTYPE html><html><head></head><body><h1>%d %s</h1><hr /><i>Miniserv powered by WebDisplays</i></body></html>";
    private ClientTaskGetFile task;
    private boolean isErrorPage;

    @Override
    public SchemePreResponse processRequest(String url) {
        url = url.substring("wd://".length());

        int pos = url.indexOf('/');
        if(pos < 0)
            return SchemePreResponse.NOT_HANDLED;

        String uuidStr = url.substring(0, pos);
        String fileStr = url.substring(pos + 1);

        try {
            fileStr = URLDecoder.decode(fileStr, "UTF-8");
        } catch(UnsupportedEncodingException ex) {
            Log.warningEx("UTF-8 isn't supported... yeah... and I'm a billionaire...", ex);
        }

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
        Log.info("Waiting for response...");
        int status = task.waitForResponse();
        Log.info("Got response %d", status);

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
            return;
        }

        int errCode;
        String errStr;

        if(status == Constants.GETF_STATUS_NOT_FOUND) {
            errCode = 404;
            errStr = "Not Found";
        } else {
            errCode = 500;
            errStr = "Internal Server Error";
        }

        resp.setStatus(errCode);
        resp.setStatusText(errStr);

        try {
            dataToWrite = String.format(ERROR_PAGE, errCode, errStr).getBytes("UTF-8");
            dataOffset = 0;
            amountToWrite = dataToWrite.length;
            isErrorPage = true;
            resp.setResponseLength(amountToWrite);
        } catch(UnsupportedEncodingException ex) {
            resp.setResponseLength(0);
        }
    }

    private byte[] dataToWrite;
    private int dataOffset;
    private int amountToWrite;

    @Override
    public boolean readResponse(ISchemeResponseData data) {
        if(dataToWrite == null) {
            if(isErrorPage) {
                data.setAmountRead(0);
                return false;
            }

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
            if(!isErrorPage)
                task.nextData();

            dataToWrite = null;
        }

        return true;
    }

}
