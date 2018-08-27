/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.SharedProxy;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.utilities.*;

import javax.annotation.Nullable;
import java.util.ArrayList;

@Message(messageId = 4, side = Side.CLIENT)
public class CMessageScreenUpdate implements IMessage, Runnable {

    public static final int UPDATE_URL = 0;
    public static final int UPDATE_RESOLUTION = 1;
    public static final int UPDATE_DELETE = 2;
    public static final int UPDATE_MOUSE = 3;
    public static final int UPDATE_TYPE = 4;
    public static final int UPDATE_UPGRADES = 5;
    public static final int UPDATE_JS_REDSTONE = 6;
    public static final int UPDATE_OWNER = 7;
    public static final int UPDATE_ROTATION = 8;
    public static final int UPDATE_RUN_JS = 9;
    public static final int UPDATE_AUTO_VOL = 10;

    public static final int MOUSE_CLICK = 0;
    public static final int MOUSE_UP = 1;
    public static final int MOUSE_MOVE = 2;
    public static final int MOUSE_DOWN = 3;

    private Vector3i pos;
    private BlockSide side;
    private int action;
    private String string;
    private Vector2i vec2i;
    private int mouseEvent;
    private ItemStack[] upgrades;
    private int redstoneLevel;
    private NameUUIDPair owner;
    private Rotation rotation;
    private boolean autoVolume;

    public CMessageScreenUpdate() {
    }

    public static CMessageScreenUpdate setURL(TileEntityScreen tes, BlockSide side, String url) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_URL;
        ret.string = url;

        return ret;
    }

    public static CMessageScreenUpdate setResolution(TileEntityScreen tes, BlockSide side, Vector2i res) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_RESOLUTION;
        ret.vec2i = res;

        return ret;
    }

    public static CMessageScreenUpdate click(TileEntityScreen tes, BlockSide side, int mouseEvent, @Nullable Vector2i pos) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_MOUSE;
        ret.mouseEvent = mouseEvent;
        ret.vec2i = pos;

        return ret;
    }

    public CMessageScreenUpdate(TileEntityScreen tes, BlockSide side) {
        pos = new Vector3i(tes.getPos());
        this.side = side;
        action = UPDATE_DELETE;
    }

    public static CMessageScreenUpdate type(TileEntityScreen tes, BlockSide side, String text) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.string = text;
        ret.action = UPDATE_TYPE;

        return ret;
    }

    public static CMessageScreenUpdate js(TileEntityScreen tes, BlockSide side, String code) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.string = code;
        ret.action = UPDATE_RUN_JS;

        return ret;
    }

    public static CMessageScreenUpdate upgrade(TileEntityScreen tes, BlockSide side) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_UPGRADES;

        ArrayList<ItemStack> upgrades = tes.getScreen(side).upgrades;
        ret.upgrades = new ItemStack[upgrades.size()];

        for(int i = 0; i < upgrades.size(); i++)
            ret.upgrades[i] = upgrades.get(i).copy();

        return ret;
    }

    public static CMessageScreenUpdate jsRedstone(TileEntityScreen tes, BlockSide side, Vector2i vec, int level) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_JS_REDSTONE;
        ret.vec2i = vec;
        ret.redstoneLevel = level;

        return ret;
    }

    public static CMessageScreenUpdate owner(TileEntityScreen tes, BlockSide side, NameUUIDPair owner) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_OWNER;
        ret.owner = owner;

        return ret;
    }

    public static CMessageScreenUpdate rotation(TileEntityScreen tes, BlockSide side, Rotation rot) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_ROTATION;
        ret.rotation = rot;

        return ret;
    }

    public static CMessageScreenUpdate autoVolume(TileEntityScreen tes, BlockSide side, boolean av) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_AUTO_VOL;
        ret.autoVolume = av;

        return ret;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new Vector3i(buf);
        side = BlockSide.values()[buf.readByte()];
        action = buf.readByte();

        if(action == UPDATE_URL || action == UPDATE_TYPE || action == UPDATE_RUN_JS)
            string = ByteBufUtils.readUTF8String(buf);
        else if(action == UPDATE_MOUSE) {
            mouseEvent = buf.readByte();

            if(mouseEvent != MOUSE_UP)
                vec2i = new Vector2i(buf);
        } else if(action == UPDATE_RESOLUTION)
            vec2i = new Vector2i(buf);
        else if(action == UPDATE_UPGRADES) {
            upgrades = new ItemStack[buf.readByte()];

            for(int i = 0; i < upgrades.length; i++)
                upgrades[i] = ByteBufUtils.readItemStack(buf);
        } else if(action == UPDATE_JS_REDSTONE) {
            vec2i = new Vector2i(buf);
            redstoneLevel = buf.readByte();
        } else if(action == UPDATE_OWNER)
            owner = new NameUUIDPair(buf);
        else if(action == UPDATE_ROTATION)
            rotation = Rotation.values()[buf.readByte() & 3];
        else if(action == UPDATE_AUTO_VOL)
            autoVolume = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        pos.writeTo(buf);
        buf.writeByte(side.ordinal());
        buf.writeByte(action);

        if(action == UPDATE_URL || action == UPDATE_TYPE || action == UPDATE_RUN_JS)
            ByteBufUtils.writeUTF8String(buf, string);
        else if(action == UPDATE_MOUSE) {
            buf.writeByte(mouseEvent);

            if(mouseEvent != MOUSE_UP)
                vec2i.writeTo(buf);
        } else if(action == UPDATE_RESOLUTION)
            vec2i.writeTo(buf);
        else if(action == UPDATE_UPGRADES) {
            buf.writeByte(upgrades.length);

            for(ItemStack is: upgrades)
                ByteBufUtils.writeItemStack(buf, is);
        } else if(action == UPDATE_JS_REDSTONE) {
            vec2i.writeTo(buf);
            buf.writeByte(redstoneLevel);
        } else if(action == UPDATE_OWNER)
            owner.writeTo(buf);
        else if(action == UPDATE_ROTATION)
            buf.writeByte(rotation.ordinal());
        else if(action == UPDATE_AUTO_VOL)
            buf.writeBoolean(autoVolume);
    }

    @Override
    public void run() {
        TileEntity te = WebDisplays.PROXY.getWorld(SharedProxy.CURRENT_DIMENSION).getTileEntity(pos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen)) {
            Log.error("CMessageScreenUpdate: TileEntity at %s is not a screen!", pos.toString());
            return;
        }

        TileEntityScreen tes = (TileEntityScreen) te;
        /*TileEntityScreen.Screen scr = tes.getScreen(side);
        if(scr == null) {
            Log.error("CMessageScreenUpdate: No screen on side %s at %s", side.toString(), pos.toString());
            return;
        }*/

        if(action == UPDATE_URL)
            tes.setScreenURL(side, string);
        else if(action == UPDATE_MOUSE)
            tes.handleMouseEvent(side, mouseEvent, vec2i);
        else if(action == UPDATE_DELETE)
            tes.removeScreen(side);
        else if(action == UPDATE_RESOLUTION)
            tes.setResolution(side, vec2i);
        else if(action == UPDATE_TYPE)
            tes.type(side, string, null);
        else if(action == UPDATE_RUN_JS)
            tes.evalJS(side, string);
        else if(action == UPDATE_UPGRADES)
            tes.updateUpgrades(side, upgrades);
        else if(action == UPDATE_JS_REDSTONE)
            tes.updateJSRedstone(side, vec2i, redstoneLevel);
        else if(action == UPDATE_OWNER) {
            TileEntityScreen.Screen scr = tes.getScreen(side);

            if(scr != null)
                scr.owner = owner;
        } else if(action == UPDATE_ROTATION)
            tes.setRotation(side, rotation);
        else if(action == UPDATE_AUTO_VOL)
            tes.setAutoVolume(side, autoVolume);
        else
            Log.warning("Caught invalid CMessageScreenUpdate with action ID %d", action);
    }

}
