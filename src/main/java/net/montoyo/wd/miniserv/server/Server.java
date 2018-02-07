/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv.server;

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

public class Server extends Thread {

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
    private long maxQuota = 1024 * 1024; //1 MiB max

    public Server() {
        setDaemon(true);
    }

    @Override
    public void start() {
        try {
            server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(port));
            server.configureBlocking(false);

            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch(Throwable t) {
            t.printStackTrace();
            return;
        }

        super.start();
    }

    @Override
    public void run() {
        boolean running = true;

        while(running) {
            try {
                loopUnsafe();
            } catch(Throwable t) {
                t.printStackTrace();
                running = false;
            }
        }
    }

    private void loopUnsafe() throws Throwable {
        selector.select();

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
                    chan.register(selector, SelectionKey.OP_READ);

                    ServerClient toAdd = new ServerClient(chan, selector);
                    clientMap.put(chan, toAdd);
                    clientList.add(toAdd);
                    toAdd.onConnect();
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
                        else {
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

        for(int i = clientList.size() - 1; i >= 0; i--) {
            ServerClient cli = clientList.get(i);

            if(cli.shouldRemove())
                removeClient(cli);
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
        return maxQuota;
    }

}
