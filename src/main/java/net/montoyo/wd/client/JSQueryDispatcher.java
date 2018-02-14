/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.mcef.api.IJSQueryCallback;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IScreenQueryHandler;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.server.SMessageScreenCtrl;
import net.montoyo.wd.utilities.*;

import java.util.*;

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
    private final Minecraft mc = Minecraft.getMinecraft();

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
            if(!tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_INPUT)) {
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
                    BlockPos bpos = (new Vector3i(tes.getPos())).addMul(side.right, x).addMul(side.up, y).toBlock();
                    int level = tes.getWorld().getBlockState(bpos).getValue(BlockScreen.emitting) ? 0 : tes.getWorld().getRedstonePower(bpos, EnumFacing.VALUES[side.reverse().ordinal()]);
                    cb.success("{\"level\":" + level + "}");
                }
            } else
                cb.failure(400, "Wrong arguments");
        });

        register("GetRedstoneArray", (cb, tes, side, args) -> {
            if(tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_INPUT)) {
                final EnumFacing facing = EnumFacing.VALUES[side.reverse().ordinal()];
                final StringJoiner resp = new StringJoiner(",", "{\"levels\":[", "]}");

                tes.forEachScreenBlocks(side, bp -> {
                    if(tes.getWorld().getBlockState(bp).getValue(BlockScreen.emitting))
                        resp.add("0");
                    else
                        resp.add("" + tes.getWorld().getRedstonePower(bp, facing));
                });

                cb.success(resp.toString());
            } else
                cb.failure(403, "Missing upgrade");
        });

        register("ClearRedstone", (cb, tes, side, args) -> {
            if(tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_OUTPUT)) {
                if(tes.getScreen(side).owner.uuid.equals(mc.player.getGameProfile().getId()))
                    makeServerQuery(tes, side, cb, JSServerRequest.CLEAR_REDSTONE);
                else
                    cb.success("{\"status\":\"notOwner\"}");
            } else
                cb.failure(403, "Missing upgrade");
        });

        register("SetRedstoneAt", (cb, tes, side, args) -> {
            if(args.length != 3 || !Arrays.stream(args).allMatch((obj) -> obj instanceof Double)) {
                cb.failure(400, "Wrong arguments");
                return;
            }

            if(!tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_OUTPUT)) {
                cb.failure(403, "Missing upgrade");
                return;
            }

            if(!tes.getScreen(side).owner.uuid.equals(mc.player.getGameProfile().getId())) {
                cb.success("{\"status\":\"notOwner\"}");
                return;
            }

            int x = ((Double) args[0]).intValue();
            int y = ((Double) args[1]).intValue();
            boolean state = ((Double) args[2]) > 0.0;

            Vector2i size = tes.getScreen(side).size;
            if(x < 0 || x >= size.x || y < 0 || y >= size.y) {
                cb.failure(403, "Out of range");
                return;
            }

            makeServerQuery(tes, side, cb, JSServerRequest.SET_REDSTONE_AT, x, y, state);
        });

        register("IsEmitting", (cb, tes, side, args) -> {
            if(!tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_OUTPUT)) {
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
                    BlockPos bpos = (new Vector3i(tes.getPos())).addMul(side.right, x).addMul(side.up, y).toBlock();
                    boolean e = tes.getWorld().getBlockState(bpos).getValue(BlockScreen.emitting);
                    cb.success("{\"emitting\":" + (e ? "true" : "false") + "}");
                }
            } else
                cb.failure(400, "Wrong arguments");
        });

        register("GetEmissionArray", (cb, tes, side, args) -> {
            if(tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_OUTPUT)) {
                final StringJoiner resp = new StringJoiner(",", "{\"emission\":[", "]}");
                tes.forEachScreenBlocks(side, bp -> resp.add(tes.getWorld().getBlockState(bp).getValue(BlockScreen.emitting) ? "1" : "0"));
                cb.success(resp.toString());
            } else
                cb.failure(403, "Missing upgrade");
        });

        register("GetLocation", (cb, tes, side, args) -> {
            if(!tes.hasUpgrade(side, DefaultUpgrade.GPS)) {
                cb.failure(403, "Missing upgrade");
                return;
            }

            BlockPos bp = tes.getPos();
            cb.success("{\"x\":" + bp.getX() + ",\"y\":" + bp.getY() + ",\"z\":" + bp.getZ() + ",\"side\":\"" + side + "\"}");
        });

        register("GetUpgrades", (cb, tes, side, args) -> {
            final StringBuilder sb = new StringBuilder("{\"upgrades\":[");
            final ArrayList<ItemStack> upgrades = tes.getScreen(side).upgrades;

            for(int i = 0; i < upgrades.size(); i++) {
                if(i > 0)
                    sb.append(',');

                sb.append('\"');
                sb.append(Util.addSlashes(((IUpgrade) upgrades.get(i).getItem()).getJSName(upgrades.get(i))));
                sb.append('\"');
            }

            cb.success(sb.append("]}").toString());
        });

        register("IsOwner", (cb, tes, side, args) -> {
            boolean res = (tes.getScreen(side).owner != null && tes.getScreen(side).owner.uuid.equals(mc.player.getGameProfile().getId()));
            cb.success("{\"isOwner\":" + (res ? "true}" : "false}"));
        });

        register("GetRotation", (cb, tes, side, args) -> cb.success("{\"rotation\":" + tes.getScreen(side).rotation.ordinal() + "}"));
        register("GetSide", (cb, tes, side, args) -> cb.success("{\"side\":" + tes.getScreen(side).side.ordinal() + "}"));
    }

}
