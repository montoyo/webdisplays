/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.client;

import java.util.function.Consumer;

public abstract class ClientTask {

    private Consumer<ClientTask> finishCallback;
    protected final Client client = Client.getInstance();

    public abstract void start();
    public abstract void abort();

    public void onFinished() {
        //Called by Client, don't call it from a ClientTask!
        if(finishCallback != null)
            finishCallback.accept(this);
    }

    public void setFinishCallback(Consumer<ClientTask> finishCallback) {
        this.finishCallback = finishCallback;
    }

}
