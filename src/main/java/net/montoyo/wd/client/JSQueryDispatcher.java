/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IJSQueryCallback;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IScreenQueryHandler;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.SMessageScreenCtrl;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;

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

    public static final class ServerQuery {

        private static int lastId = 0;

        private final TileEntityScreen tes;
        private final BlockSide side;
        private final IJSQueryCallback callback;
        private final int id;

        private ServerQuery(TileEntityScreen t, BlockSide s, IJSQueryCallback cb) {
            tes = t;
            side = s;
            callback = cb;
            id = lastId++;
        }

        public TileEntityScreen getTileEntity() {
            return tes;
        }

        public BlockSide getSide() {
            return side;
        }

        public TileEntityScreen.Screen getScreen() {
            return tes.getScreen(side);
        }

        public void success(String resp) {
            callback.success(resp);
        }

        public void error(int errId, String errStr) {
            callback.failure(errId, errStr);
        }

    }

    private final ClientProxy proxy;
    private final ArrayDeque<QueryData> queue = new ArrayDeque<>();
    private final ClientProxy.ScreenSidePair lookupResult = new ClientProxy.ScreenSidePair();
    private final HashMap<String, IScreenQueryHandler> handlers = new HashMap<>();
    private final ArrayList<ServerQuery> serverQueries = new ArrayList<>();

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
                        next.callback.failure(500, "Internal error");
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

    public ServerQuery fulfillQuery(int id) {
        int toRemove = -1;

        for(int i = 0; i < serverQueries.size(); i++) {
            ServerQuery sq = serverQueries.get(i);

            if(sq.id == id) {
                toRemove = i;
                break;
            }
        }

        if(toRemove < 0)
            return null;
        else
            return serverQueries.remove(toRemove);
    }

    private void makeServerQuery(TileEntityScreen tes, BlockSide side, IJSQueryCallback cb, JSServerRequest type, Object ... data) {
        ServerQuery ret = new ServerQuery(tes, side, cb);
        serverQueries.add(ret);

        WebDisplays.NET_HANDLER.sendToServer(SMessageScreenCtrl.jsRequest(tes, side, ret.id, type, data));
    }

    private void registerDefaults() {
        register("GetSize", (cb, tes, side, args) -> {
            Vector2i size = tes.getScreen(side).size;
            cb.success("{\"x\":" + size.x + ",\"y\":" + size.y + "}");
        });

        register("GetRedstoneAt", (cb, tes, side, args) -> {
            if(!tes.hasUpgrade(side, WebDisplays.INSTANCE.itemUpgrade, DefaultUpgrade.REDSTONE_INPUT.ordinal())) {
                cb.failure(403, "Missing upgrade");
                return;
            }

            if(args.length == 2 && args[0] instanceof Double && args[1] instanceof Double) {
                TileEntityScreen.Screen scr = tes.getScreen(side);
                int x = ((Double) args[0]).intValue();
                int y = ((Double) args[1]).intValue();

                if(x < 0 || x >= scr.size.x || y < 0 || y >= scr.size.y)
                    cb.failure(403, "Out of range");
                else {
                    int level = tes.getWorld().getRedstonePower((new Vector3i(tes.getPos())).addMul(side.right, x).addMul(side.up, y).toBlock(), EnumFacing.VALUES[side.reverse().ordinal()]);
                    cb.success("{\"level\":" + level + "}");
                }
            } else
                cb.failure(400, "Wrong arguments");
        });

        register("GetRedstoneArray", (cb, tes, side, args) -> {
            if(tes.hasUpgrade(side, WebDisplays.INSTANCE.itemUpgrade, DefaultUpgrade.REDSTONE_INPUT.ordinal())) {
                final Vector2i size = tes.getScreen(side).size;
                final EnumFacing facing = EnumFacing.VALUES[side.reverse().ordinal()];
                final BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();
                final Vector3i vec1 = new Vector3i(tes.getPos());
                final Vector3i vec2 = new Vector3i();
                final StringBuilder resp = new StringBuilder("{\"levels\":[");

                for(int y = 0; y < size.y; y++) {
                    vec2.set(vec1);

                    for(int x = 0; x < size.x; x++) {
                        if(x > 0 || y > 0)
                            resp.append(',');

                        vec2.toBlock(mbp);
                        resp.append(tes.getWorld().getRedstonePower(mbp, facing));
                        vec2.add(side.right.x, side.right.y, side.right.z);
                    }

                    vec1.add(side.up.x, side.up.y, side.up.z);
                }

                cb.success(resp.append("]}").toString());
            } else
                cb.failure(403, "Missing upgrade");
        });
    }

}
