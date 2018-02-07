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

import java.util.ArrayList;

@Message(messageId = 0, side = Side.CLIENT)
public class CMessageAddScreen implements IMessage, Runnable {

    private boolean clear;
    private Vector3i pos;
    private TileEntityScreen.Screen[] screens;

    public CMessageAddScreen() {
    }

    public CMessageAddScreen(TileEntityScreen tes) {
        clear = true;
        pos = new Vector3i(tes.getPos());
        screens = new TileEntityScreen.Screen[tes.screenCount()];

        for(int i = 0; i < tes.screenCount(); i++)
            screens[i] = tes.getScreen(i);
    }

    public CMessageAddScreen(TileEntityScreen tes, TileEntityScreen.Screen ... toSend) {
        clear = false;
        pos = new Vector3i(tes.getPos());
        screens = toSend;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        clear = buf.readBoolean();
        pos = new Vector3i(buf);
        int cnt = buf.readByte() & 7;

        screens = new TileEntityScreen.Screen[cnt];
        for(int i = 0; i < cnt; i++) {
            screens[i] = new TileEntityScreen.Screen();
            screens[i].side = BlockSide.values()[buf.readByte()];
            screens[i].size = new Vector2i(buf);
            screens[i].url = ByteBufUtils.readUTF8String(buf);
            screens[i].resolution = new Vector2i(buf);
            screens[i].rotation = Rotation.values()[buf.readByte() & 3];
            screens[i].owner = new NameUUIDPair(buf);
            screens[i].upgrades = new ArrayList<>();

            int numUpgrades = buf.readByte();
            for(int j = 0; j < numUpgrades; j++)
                screens[i].upgrades.add(ByteBufUtils.readItemStack(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(clear);
        pos.writeTo(buf);
        buf.writeByte(screens.length);

        for(TileEntityScreen.Screen scr: screens) {
            buf.writeByte(scr.side.ordinal());
            scr.size.writeTo(buf);
            ByteBufUtils.writeUTF8String(buf, scr.url);
            scr.resolution.writeTo(buf);
            buf.writeByte(scr.rotation.ordinal());
            scr.owner.writeTo(buf);
            buf.writeByte(scr.upgrades.size());

            for(ItemStack is: scr.upgrades)
                ByteBufUtils.writeItemStack(buf, is);
        }
    }

    @Override
    public void run() {
        TileEntity te = WebDisplays.PROXY.getWorld(SharedProxy.CURRENT_DIMENSION).getTileEntity(pos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen)) {
            if(clear)
                Log.error("CMessageAddScreen: Can't add screen to invalid tile entity at %s", pos.toString());

            return;
        }

        TileEntityScreen tes = (TileEntityScreen) te;
        if(clear)
            tes.clear();

        for(TileEntityScreen.Screen entry: screens) {
            TileEntityScreen.Screen scr = tes.addScreen(entry.side, entry.size, entry.resolution, null, false);
            scr.rotation = entry.rotation;
            scr.url = entry.url;
            scr.owner = entry.owner;
            scr.upgrades = entry.upgrades;

            if(scr.browser != null)
                scr.browser.loadURL(entry.url);
        }
    }

}
