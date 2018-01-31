/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IJSQueryCallback;
import net.montoyo.wd.core.IScreenQueryHandler;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Vector2i;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

@SideOnly(Side.CLIENT)
public final class JSQueryDispatcher {

    private static final class QueryData {

        private final IBrowser browser;
        private final String query;
        private final String args;
        private final IJSQueryCallback callback;

        private QueryData(IBrowser b, String q, String a, IJSQueryCallback cb) {
            browser = b;
            query = q;
            args = a;
            callback = cb;
        }

    }

    private final ClientProxy proxy;
    private final ArrayDeque<QueryData> queue = new ArrayDeque<>();
    private final ClientProxy.ScreenSidePair lookupResult = new ClientProxy.ScreenSidePair();
    private final HashMap<String, IScreenQueryHandler> handlers = new HashMap<>();

    public JSQueryDispatcher(ClientProxy proxy) {
        this.proxy = proxy;
        registerDefaults();
    }

    public void enqueueQuery(IBrowser b, String q, String a, IJSQueryCallback cb) {
        synchronized(queue) {
            queue.offer(new QueryData(b, q, a, cb));
        }
    }

    public void handleQueries() {
        while(true) {
            QueryData next;
            synchronized(queue) {
                next = queue.poll();
            }

            if(next == null)
                break;

            if(proxy.findScreenFromBrowser(next.browser, lookupResult)) {
                Object[] args = (next.args == null) ? new Object[0] : parseArgs(next.args);

                if(args == null)
                    next.callback.failure(400, "Malformed request parameters");
                else {
                    try {
                        handlers.get(next.query).handleQuery(next.callback, lookupResult.tes, lookupResult.side, args);
                    } catch(Throwable t) {
                        Log.warningEx("Could not execute JS query %s(%s)", t, next.query, (next.args == null) ? "" : next.args);
                    }
                }
            } else
                next.callback.failure(403, "A screen is required");
        }
    }

    public boolean canHandleQuery(String q) {
        return handlers.containsKey(q);
    }

    private static Object[] parseArgs(String args) {
        ArrayList<String> array = new ArrayList<>();
        int lastIdx = 0;
        boolean inString = false;
        boolean escape = false;
        boolean hadString = false;

        for(int i = 0; i < args.length(); i++) {
            char chr = args.charAt(i);

            if(inString) {
                if(escape)
                    escape = false;
                else {
                    if(chr == '\"')
                        inString = false;
                    else if(chr == '\\')
                        escape = true;
                }
            } else if(chr == '\"') {
                if(hadString)
                    return null;

                inString = true;
                hadString = true;
            } else if(chr == ',') {
                array.add(args.substring(lastIdx, i).trim());
                lastIdx = i + 1;
                hadString = false;
            }
        }

        if(inString)
            return null; //Non terminated string

        array.add(args.substring(lastIdx).trim());
        Object[] ret = new Object[array.size()];

        for(int i = 0; i < ret.length; i++) {
            String str = array.get(i);
            if(str.isEmpty())
                return null; //Nah...

            if(str.charAt(0) == '\"') //String
                ret[i] = str.substring(1, str.length() - 1);
            else {
                try {
                    ret[i] = Double.parseDouble(str);
                } catch(NumberFormatException ex) {
                    return null;
                }
            }
        }

        return ret;
    }

    public void register(String query, IScreenQueryHandler handler) {
        handlers.put(query.toLowerCase(), handler);
    }

    private void registerDefaults() {
        register("getSize", (cb, tes, side, args) -> {
            Vector2i size = tes.getScreen(side).size;
            cb.success("{\"x\":" + size.x + ",\"y\":" + size.y + "}");
        });
    }

}
