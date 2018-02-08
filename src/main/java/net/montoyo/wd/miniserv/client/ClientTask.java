/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import net.montoyo.wd.WebDisplays;

import java.util.function.Consumer;

public abstract class ClientTask<T extends ClientTask> {

    private Consumer<T> finishCallback;
    private volatile boolean canceled;
    protected boolean runCallbackOnMcThread;
    protected final Client client = Client.getInstance();

    public abstract void start();
    public abstract void abort();

    public void onFinished() {
        //Called by Client, don't call it from a ClientTask!
        if(finishCallback != null && !isCanceled()) {
            if(runCallbackOnMcThread)
                WebDisplays.PROXY.enqueue(() -> finishCallback.accept((T) this));
            else
                finishCallback.accept((T) this);
        }
    }

    public void setFinishCallback(Consumer<T> finishCallback) {
        this.finishCallback = finishCallback;
    }

    public void setRunCallbackOnMinecraftThread(boolean runCallbackOnMcThread) {
        this.runCallbackOnMcThread = runCallbackOnMcThread;
    }

    public final void cancel() {
        synchronized(this) {
            canceled = true;
        }

        Client.getInstance().wakeup();
    }

    public final boolean isCanceled() {
        boolean ret;
        synchronized(this) {
            ret = canceled;
        }

        return ret;
    }

}
