/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.SharedProxy;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;

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

    public static final int MOUSE_CLICK = 0;
    public static final int MOUSE_UP = 1;
    public static final int MOUSE_MOVE = 2;
    public static final int MOUSE_DOWN = 3;

    private Vector3i pos;
    private BlockSide side;
    private int action;
    private String url;
    private Vector2i resolution;
    private int mouseEvent;
    private Vector2i mouse;
    private String text;
    private ItemStack[] upgrades;

    public CMessageScreenUpdate() {
    }

    public static CMessageScreenUpdate setURL(TileEntityScreen tes, BlockSide side, String url) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_URL;
        ret.url = url;

        return ret;
    }

    public static CMessageScreenUpdate setResolution(TileEntityScreen tes, BlockSide side, Vector2i res) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_RESOLUTION;
        ret.resolution = res;

        return ret;
    }

    public static CMessageScreenUpdate click(TileEntityScreen tes, BlockSide side, int mouseEvent, @Nullable Vector2i pos) {
        CMessageScreenUpdate ret = new CMessageScreenUpdate();
        ret.pos = new Vector3i(tes.getPos());
        ret.side = side;
        ret.action = UPDATE_MOUSE;
        ret.mouseEvent = mouseEvent;
        ret.mouse = pos;

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
        ret.text = text;
        ret.action = UPDATE_TYPE;

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

    @Override
    public void fromBytes(ByteBuf buf) {
        pos = new Vector3i(buf);
        side = BlockSide.values()[buf.readByte()];
        action = buf.readByte();

        if(action == UPDATE_URL)
            url = ByteBufUtils.readUTF8String(buf);
        else if(action == UPDATE_MOUSE) {
            mouseEvent = buf.readByte();

            if(mouseEvent != MOUSE_UP)
                mouse = new Vector2i(buf);
        } else if(action == UPDATE_RESOLUTION)
            resolution = new Vector2i(buf);
        else if(action == UPDATE_TYPE)
            text = ByteBufUtils.readUTF8String(buf);
        else if(action == UPDATE_UPGRADES) {
            upgrades = new ItemStack[buf.readByte()];

            for(int i = 0; i < upgrades.length; i++)
                upgrades[i] = ByteBufUtils.readItemStack(buf);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        pos.writeTo(buf);
        buf.writeByte(side.ordinal());
        buf.writeByte(action);

        if(action == UPDATE_URL)
            ByteBufUtils.writeUTF8String(buf, url);
        else if(action == UPDATE_MOUSE) {
            buf.writeByte(mouseEvent);

            if(mouseEvent != MOUSE_UP)
                mouse.writeTo(buf);
        } else if(action == UPDATE_RESOLUTION)
            resolution.writeTo(buf);
        else if(action == UPDATE_TYPE)
            ByteBufUtils.writeUTF8String(buf, text);
        else if(action == UPDATE_UPGRADES) {
            buf.writeByte(upgrades.length);

            for(ItemStack is: upgrades)
                ByteBufUtils.writeItemStack(buf, is);
        }
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
            tes.setScreenURL(side, url);
        else if(action == UPDATE_MOUSE)
            tes.handleMouseEvent(side, mouseEvent, mouse);
        else if(action == UPDATE_DELETE)
            tes.removeScreen(side);
        else if(action == UPDATE_RESOLUTION)
            tes.setResolution(side, resolution);
        else if(action == UPDATE_TYPE)
            tes.type(side, text, null);
        else if(action == UPDATE_UPGRADES)
            tes.updateUpgrades(side, upgrades);
        else
            Log.warning("===> TODO"); //TODO
    }

}
