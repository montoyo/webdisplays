/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.miniserv.Constants;
import net.montoyo.wd.miniserv.OutgoingPacket;
import net.montoyo.wd.miniserv.PacketID;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ClientTaskUploadFile extends ClientTask<ClientTaskUploadFile> implements Consumer<OutgoingPacket> {

    private static final byte[] UPLOAD_BUFFER = new byte[65535];

    private final File file;
    private final long size;
    private FileInputStream fis;
    private boolean abortFupa;
    private int uploadStatus;
    private long uploadPos;
    private BiConsumer<Long, Long> onProgress;

    public ClientTaskUploadFile(File fle) throws IOException {
        file = fle;
        size = Files.size(fle.toPath());
        fis = new FileInputStream(fle);
        runCallbackOnMcThread = true;
    }

    @Override
    public void start() {
        OutgoingPacket pkt = new OutgoingPacket();
        pkt.writeByte(PacketID.BEGIN_FILE_UPLOAD.ordinal());
        pkt.writeString(file.getName());
        pkt.writeLong(size);

        client.sendPacket(pkt);
    }

    @Override
    public void abort() {
        abortFupa = true;
        uploadStatus = Constants.FUPA_STATUS_CONNECTION_LOST;
        Util.silentClose(fis);
    }

    public void onReceivedUploadStatus(int status) {
        if(status == 0) {
            //Begin upload
            Log.info("Now uploading %s", file.getName());
            accept(null);
        } else {
            Util.silentClose(fis);
            uploadStatus = status;
            client.nextTask();
        }
    }

    public void onUploadFinishedStatus(int status) {
        abortFupa = true; //This isn't necessary, but just in case...
        uploadStatus = status;
        client.nextTask();
    }

    @Override
    public void accept(OutgoingPacket nocare) {
        if(abortFupa)
            return;

        int rd;

        do {
            try {
                rd = fis.read(UPLOAD_BUFFER);
            } catch(IOException ex) {
                Log.warningEx("Caught IOException while sending some file", ex);
                rd = 0; //This will cause a FUPA_STATUS_USER_ABORT
                break;
            }
        } while(rd == 0);

        if(rd >= 0) { //If rd < 0, end of file, we're done.
            OutgoingPacket pkt = new OutgoingPacket();
            pkt.writeByte(PacketID.FILE_PART.ordinal());
            pkt.writeShort(rd);
            pkt.writeBytes(UPLOAD_BUFFER, 0, rd);
            client.sendPacket(pkt);

            if(onProgress != null) {
                uploadPos += (long) rd;
                onProgress.accept(uploadPos, size);
            }

            if(rd > 0) {
                pkt.setOnFinishAction(this);
                return;
            }
        }

        Util.silentClose(file);
    }

    public int getUploadStatus() {
        return uploadStatus;
    }

    public void setProgressCallback(BiConsumer<Long, Long> onProgress) {
        this.onProgress = onProgress;
    }

}
