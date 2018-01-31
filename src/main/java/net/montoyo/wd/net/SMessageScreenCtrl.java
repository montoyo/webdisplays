/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.*;

@Message(messageId = 2, side = Side.SERVER)
public class SMessageScreenCtrl implements IMessage, Runnable {

    public static final int CTRL_SET_URL = 0;
    public static final int CTRL_SHUT_DOWN = 1;
    public static final int CTRL_ADD_FRIEND = 2;
    public static final int CTRL_REMOVE_FRIEND = 3;
    public static final int CTRL_SET_RIGHTS = 4;
    public static final int CTRL_SET_RESOLUTION = 5;
    public static final int CTRL_TYPE = 6;
    public static final int CTRL_REMOVE_UPGRADE = 7;
    public static final int CTRL_LASER_DOWN = 8;
    public static final int CTRL_LASER_MOVE = 9;
    public static final int CTRL_LASER_UP = 10;
    public static final int CTRL_JS_REQUEST = 11;

    private int ctrl;
    private int dim;
    private Vector3i pos;
    private BlockSide side;
    private String url;
    private NameUUIDPair friend;
    private EntityPlayerMP player;
    private int friendRights;
    private int otherRights;
    private Vector2i vec2i;
    private String text;
    private BlockPos soundPos;
    private ItemStack toRemove;
    private int jsReqID;
    private JSServerRequest jsReqType;
    private Object[] jsReqData;

    public SMessageScreenCtrl() {
    }

    public SMessageScreenCtrl(TileEntityScreen tes, BlockSide side, String url) {
        ctrl = CTRL_SET_URL;
        dim = tes.getWorld().provider.getDimension();
        pos = new Vector3i(tes.getPos());
        this.side = side;
        this.url = url;
    }

    public SMessageScreenCtrl(TileEntityScreen tes, BlockSide side) {
        ctrl = CTRL_SHUT_DOWN;
        dim = tes.getWorld().provider.getDimension();
        pos = new Vector3i(tes.getPos());
        this.side = side;
        this.url = url;
    }

    public SMessageScreenCtrl(TileEntityScreen tes, BlockSide side, NameUUIDPair friend, boolean del) {
        ctrl = del ? CTRL_REMOVE_FRIEND : CTRL_ADD_FRIEND;
        dim = tes.getWorld().provider.getDimension();
        pos = new Vector3i(tes.getPos());
        this.side = side;
        this.friend = friend;
    }

    public SMessageScreenCtrl(TileEntityScreen tes, BlockSide side, int fr, int or) {
        ctrl = CTRL_SET_RIGHTS;
        dim = tes.getWorld().provider.getDimension();
        pos = new Vector3i(tes.getPos());
        this.side = side;
        friendRights = fr;
        otherRights = or;
    }

    public SMessageScreenCtrl(TileEntityScreen tes, BlockSide side, ItemStack toRem) {
        ctrl = CTRL_REMOVE_UPGRADE;
        dim = tes.getWorld().provider.getDimension();
        pos = new Vector3i(tes.getPos());
        this.side = side;
        toRemove = toRem;
    }

    public static SMessageScreenCtrl type(TileEntityScreen tes, BlockSide side, String text, BlockPos soundPos) {
        SMessageScreenCtrl ret = new SMessageScreenCtrl();
        ret.ctrl = CTRL_TYPE;
        ret.pos = new Vector3i(tes.getPos());
        ret.dim = tes.getWorld().provider.getDimension();
        ret.side = side;
        ret.text = text;
        ret.soundPos = soundPos;

        return ret;
    }

    public static SMessageScreenCtrl vec2(TileEntityScreen tes, BlockSide side, int ctrl, Vector2i vec) {
        if(!isVec2Ctrl(ctrl))
            throw new RuntimeException("Called SMessageScreenCtrl.vec2() with non-vec2 control message " + ctrl);

        SMessageScreenCtrl ret = new SMessageScreenCtrl();
        ret.ctrl = ctrl;
        ret.pos = new Vector3i(tes.getPos());
        ret.dim = tes.getWorld().provider.getDimension();
        ret.side = side;
        ret.vec2i = vec;

        return ret;
    }

    public static SMessageScreenCtrl laserUp(TileEntityScreen tes, BlockSide side) {
        SMessageScreenCtrl ret = new SMessageScreenCtrl();
        ret.ctrl = CTRL_LASER_UP;
        ret.pos = new Vector3i(tes.getPos());
        ret.dim = tes.getWorld().provider.getDimension();
        ret.side = side;

        return ret;
    }

    public static SMessageScreenCtrl jsRequest(TileEntityScreen tes, BlockSide side, int reqId, JSServerRequest reqType, Object ... data) {
        SMessageScreenCtrl ret = new SMessageScreenCtrl();
        ret.ctrl = CTRL_JS_REQUEST;
        ret.pos = new Vector3i(tes.getPos());
        ret.dim = tes.getWorld().provider.getDimension();
        ret.side = side;
        ret.jsReqID = reqId;
        ret.jsReqType = reqType;
        ret.jsReqData = data;

        return ret;
    }

    private static boolean isVec2Ctrl(int msg) {
        return msg == CTRL_SET_RESOLUTION || msg == CTRL_LASER_DOWN || msg == CTRL_LASER_MOVE;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        ctrl = buf.readByte();
        dim = buf.readInt();
        pos = new Vector3i(buf);
        side = BlockSide.values()[buf.readByte()];

        if(ctrl == CTRL_SET_URL)
            url = ByteBufUtils.readUTF8String(buf);
        else if(ctrl == CTRL_ADD_FRIEND || ctrl == CTRL_REMOVE_FRIEND)
            friend = new NameUUIDPair(buf);
        else if(ctrl == CTRL_SET_RIGHTS) {
            friendRights = buf.readByte();
            otherRights = buf.readByte();
        } else if(isVec2Ctrl(ctrl))
            vec2i = new Vector2i(buf);
        else if(ctrl == CTRL_TYPE) {
            text = ByteBufUtils.readUTF8String(buf);

            int sx = buf.readInt();
            int sy = buf.readInt();
            int sz = buf.readInt();
            soundPos = new BlockPos(sx, sy, sz);
        } else if(ctrl == CTRL_REMOVE_UPGRADE)
            toRemove = ByteBufUtils.readItemStack(buf);
        else if(ctrl == CTRL_JS_REQUEST) {
            jsReqID = buf.readInt();
            jsReqType = JSServerRequest.fromID(buf.readByte());

            if(jsReqType != null)
                jsReqData = jsReqType.deserialize(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(ctrl);
        buf.writeInt(dim);
        pos.writeTo(buf);
        buf.writeByte(side.ordinal());

        if(ctrl == CTRL_SET_URL)
            ByteBufUtils.writeUTF8String(buf, url);
        else if(ctrl == CTRL_ADD_FRIEND || ctrl == CTRL_REMOVE_FRIEND)
            friend.writeTo(buf);
        else if(ctrl == CTRL_SET_RIGHTS) {
            buf.writeByte(friendRights);
            buf.writeByte(otherRights);
        } else if(isVec2Ctrl(ctrl))
            vec2i.writeTo(buf);
        else if(ctrl == CTRL_TYPE) {
            ByteBufUtils.writeUTF8String(buf, text);
            buf.writeInt(soundPos.getX());
            buf.writeInt(soundPos.getY());
            buf.writeInt(soundPos.getZ());
        } else if(ctrl == CTRL_REMOVE_UPGRADE)
            ByteBufUtils.writeItemStack(buf, toRemove);
        else if(ctrl == CTRL_JS_REQUEST) {
            buf.writeInt(jsReqID);
            buf.writeByte(jsReqType.ordinal());

            if(!jsReqType.serialize(buf, jsReqData))
                throw new RuntimeException("Could not serialize CTRL_JS_REQUEST " + jsReqType);
        }
    }

    @Override
    public void run() {
        try {
            runUnsafe();
        } catch(MissingPermissionException e) {
            Log.errorEx("I have reasons to believe %s (UUID %s) is a hacker, but don't take my word for it...", e, e.getPlayer().getName(), e.getPlayer().getGameProfile().getId().toString());
        }
    }

    private void checkPermission(TileEntityScreen scr, int right) throws MissingPermissionException {
        int prights = scr.getScreen(side).rightsFor(player);
        if((prights & right) == 0)
            throw new MissingPermissionException(right, player);
    }

    private void runUnsafe() throws MissingPermissionException {
        World world = player.world;
        BlockPos bp = pos.toBlock();

        if(world.provider.getDimension() != dim || player.getDistanceSq(bp) > 512.0 * 512.0)
            return; //Out of range

        TileEntity te = world.getTileEntity(bp);
        if(te == null || !(te instanceof TileEntityScreen)) {
            Log.error("TileEntity at %s is not a screen; can't change url!", pos.toString());
            return;
        }

        TileEntityScreen tes = (TileEntityScreen) te;

        if(ctrl == CTRL_SET_URL) {
            checkPermission(tes, ScreenRights.CHANGE_URL);
            tes.setScreenURL(side, url);
        } else if(ctrl == CTRL_SHUT_DOWN) {
            checkPermission(tes, ScreenRights.CHANGE_URL);
            tes.removeScreen(side);
        } else if(ctrl == CTRL_ADD_FRIEND) {
            checkPermission(tes, ScreenRights.MANAGE_FRIEND_LIST);
            tes.addFriend(player, side, friend);
        } else if(ctrl == CTRL_REMOVE_FRIEND) {
            checkPermission(tes, ScreenRights.MANAGE_FRIEND_LIST);
            tes.removeFriend(player, side, friend);
        } else if(ctrl == CTRL_SET_RIGHTS) {
            TileEntityScreen.Screen scr = tes.getScreen(side);
            int fr = scr.owner.uuid.equals(player.getGameProfile().getId()) ? friendRights : scr.friendRights;
            int or = (scr.rightsFor(player) & ScreenRights.MANAGE_OTHER_RIGHTS) == 0 ? scr.otherRights : otherRights;

            if(scr.friendRights != fr || scr.otherRights != or)
                tes.setRights(player, side, fr, or);
        } else if(ctrl == CTRL_SET_RESOLUTION) {
            checkPermission(tes, ScreenRights.CHANGE_RESOLUTION);
            tes.setResolution(side, vec2i);
        } else if(ctrl == CTRL_TYPE) {
            checkPermission(tes, ScreenRights.CLICK);
            tes.type(side, text, soundPos);
        } else if(ctrl == CTRL_REMOVE_UPGRADE) {
            checkPermission(tes, ScreenRights.MANAGE_UPGRADES);
            tes.removeUpgrade(side, toRemove, player);
        } else if(ctrl == CTRL_LASER_DOWN || ctrl == CTRL_LASER_MOVE)
            tes.laserDownMove(side, player, vec2i, ctrl == CTRL_LASER_DOWN);
        else if(ctrl == CTRL_LASER_UP)
            tes.laserUp(side, player);
        else if(ctrl == CTRL_JS_REQUEST) {
            if(jsReqType == null || jsReqData == null)
                Log.warning("Caught invalid JS request from player %s (UUID %s)", player.getName(), player.getGameProfile().getId().toString());
            else
                tes.handleJSRequest(player, side, jsReqID, jsReqType, jsReqData);
        } else
            Log.info("SMessageScreenCtrl: TODO"); //TODO: other ctrl messages
    }

    public static class Handler implements IMessageHandler<SMessageScreenCtrl, IMessage> {

        @Override
        public IMessage onMessage(SMessageScreenCtrl message, MessageContext ctx) {
            message.player = ctx.getServerHandler().player;
            ((WorldServer) message.player.world).addScheduledTask(message);
            return null;
        }

    }

}
