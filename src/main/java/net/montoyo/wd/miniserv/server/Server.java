/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.server;

import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Util;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class Server implements Runnable {

    private static Server instance;

    public static Server getInstance() {
        if(instance == null)
            instance = new Server();

        return instance;
    }

    private ServerSocketChannel server;
    private Selector selector;
    private int port = 25566;
    private final ArrayList<ServerClient> clientList = new ArrayList<>();
    private final HashMap<SocketChannel, ServerClient> clientMap = new HashMap<>();
    private final ByteBuffer readBuffer = ByteBuffer.allocateDirect(8192);
    private final ClientManager clientMgr = new ClientManager();
    private File directory;
    private volatile boolean running;
    private volatile Thread thread;

    public void setPort(int p) {
        port = p;
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("MiniServServer");
        thread.setDaemon(true);

        try {
            server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(port));
            server.configureBlocking(false);
        } catch(Throwable t) {
            t.printStackTrace();
            Util.silentClose(server);
            server = null;
            return;
        }

        try {
            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch(Throwable t) {
            t.printStackTrace();
            Util.silentClose(selector);
            Util.silentClose(server);
            selector = null;
            server = null;
            return;
        }

        synchronized(this) {
            running = true;
        }

        thread.start();
    }

    public void stopServer() {
        if(getRunning()) {
            Thread thread = this.thread;

            synchronized(this) {
                running = false;
                selector.wakeup();
            }

            while(thread.isAlive()) {
                try {
                    thread.join();
                } catch(InterruptedException ex) { }
            }

            Log.info("Miniserv server stopped");
        }
    }

    private boolean getRunning() {
        boolean ret;
        synchronized(this) {
            ret = running;
        }

        return ret;
    }

    @Override
    public void run() {
        while(getRunning()) {
            try {
                loopUnsafe();
            } catch(Throwable t) {
                Log.errorEx("Miniserv Server crashed", t);
                break;
            }
        }

        synchronized(this) {
            running = false;
        }

        for(ServerClient cli: clientList)
            Util.silentClose(cli.getChannel());

        clientList.clear();
        clientMap.clear();
        Util.silentClose(selector);
        Util.silentClose(server);
        selector = null;
        server = null;
        thread = null;
    }

    private void loopUnsafe() throws Throwable {
        selector.select(1000); //Allow the server to kick timed-out clients

        for(SelectionKey key: selector.selectedKeys()) {
            if(key.isAcceptable()) {
                SocketChannel chan;

                try {
                    chan = server.accept();
                } catch(Throwable t) {
                    Log.warningEx("Could not accept client", t);
                    chan = null;
                }

                if(chan != null) {
                    chan.configureBlocking(false);
                    ServerClient toAdd = new ServerClient(chan, selector);

                    clientMap.put(chan, toAdd);
                    clientList.add(toAdd);
                }
            }

            if(key.isReadable()) {
                ServerClient cli = clientMap.get(key.channel());

                if(cli == null)
                    Log.warning("Received read info from unknown client");
                else {
                    try {
                        readBuffer.clear();
                        int read = cli.getChannel().read(readBuffer);

                        if(read < 0)
                            cli.setShouldRemove(); //End of stream
                        else if(read > 0) {
                            readBuffer.position(0);
                            readBuffer.limit(read);
                            cli.readyRead(readBuffer);
                        }
                    } catch(Throwable t) {
                        Log.warningEx("Could not read data from client", t);
                        cli.setShouldRemove();
                    }
                }
            }

            if(key.isWritable()) {
                ServerClient cli = clientMap.get(key.channel());

                if(cli == null)
                    Log.warning("Received write info from unknown client");
                else {
                    try {
                        cli.readyWrite();
                    } catch(Throwable t) {
                        Log.warningEx("Could not write data to client", t);
                        cli.setShouldRemove();
                    }
                }
            }
        }

        long ctime = System.currentTimeMillis();
        for(int i = clientList.size() - 1; i >= 0; i--) {
            ServerClient cli = clientList.get(i);

            if(cli.shouldRemove())
                removeClient(cli);
            else if(cli.hasTimedOut(ctime)) {
                Log.info("Client %s has timed out!", cli.getUUIDString());
                removeClient(cli);
            }
        }
    }

    private void removeClient(ServerClient cli) {
        clientMap.remove(cli.getChannel());
        clientList.remove(cli);
        Util.silentClose(cli.getChannel());
    }

    public ClientManager getClientManager() {
        return clientMgr;
    }

    public void setDirectory(File dir) {
        if(!dir.exists()) {
            if(!dir.mkdir())
                Log.warning("Could not create miniserv storage directory %s", dir.getAbsolutePath());
        }

        directory = dir;
    }

    public File getDirectory() {
        return directory;
    }

    public long getMaxQuota() {
        return WebDisplays.INSTANCE.miniservQuota;
    }

}
