/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.GuiScreenConfig;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.client.CMessageOpenGui;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Vector3i;

public class ScreenConfigData extends GuiData {

    public boolean onlyUpdate;
    public Vector3i pos;
    public BlockSide side;
    public NameUUIDPair[] friends;
    public int friendRights;
    public int otherRights;

    public ScreenConfigData() {
    }

    public ScreenConfigData(Vector3i pos, BlockSide side, TileEntityScreen.Screen scr) {
        this.pos = pos;
        this.side = side;
        friends = scr.friends.toArray(new NameUUIDPair[0]);
        friendRights = scr.friendRights;
        otherRights = scr.otherRights;
        onlyUpdate = false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiScreen createGui(GuiScreen old, World world) {
        if(old != null && old instanceof GuiScreenConfig) {
            GuiScreenConfig gsc = (GuiScreenConfig) old;

            if(gsc.isForBlock(pos.toBlock(), side)) {
                gsc.updateFriends(friends);
                gsc.updateFriendRights(friendRights);
                gsc.updateOtherRights(otherRights);
                gsc.updateMyRights();

                return null;
            }
        }

        if(onlyUpdate)
            return null;

        TileEntity te = world.getTileEntity(pos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen)) {
            Log.error("TileEntity at %s is not a screen; can't open gui!", pos.toString());
            return null;
        }

        return new GuiScreenConfig((TileEntityScreen) te, side, friends, friendRights, otherRights);
    }

    @Override
    public String getName() {
        return "ScreenConfig";
    }

    public ScreenConfigData updateOnly() {
        onlyUpdate = true;
        return this;
    }

    public void sendTo(NetworkRegistry.TargetPoint tp) {
        WebDisplays.NET_HANDLER.sendToAllAround(new CMessageOpenGui(this), tp);
    }

}
