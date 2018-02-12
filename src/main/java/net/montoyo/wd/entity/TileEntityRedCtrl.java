/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.RedstoneCtrlData;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Util;

import javax.annotation.Nonnull;

public class TileEntityRedCtrl extends TileEntityPeripheralBase {

    private String risingEdgeURL = "";
    private String fallingEdgeURL = "";
    private boolean state = false;

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        risingEdgeURL = tag.getString("RisingEdgeURL");
        fallingEdgeURL = tag.getString("FallingEdgeURL");
        state = tag.getBoolean("Powered");
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        tag.setString("RisingEdgeURL", risingEdgeURL);
        tag.setString("FallingEdgeURL", fallingEdgeURL);
        tag.setBoolean("Powered", state);
        return tag;
    }

    @Override
    public boolean onRightClick(EntityPlayer player, EnumHand hand, BlockSide side) {
        if(world.isRemote)
            return true;

        if(!isScreenChunkLoaded()) {
            Util.toast(player, "chunkUnloaded");
            return true;
        }

        TileEntityScreen tes = getConnectedScreen();
        if(tes == null) {
            Util.toast(player, "notLinked");
            return true;
        }

        TileEntityScreen.Screen scr = tes.getScreen(screenSide);
        if((scr.rightsFor(player) & ScreenRights.CHANGE_URL) == 0) {
            Util.toast(player, "restrictions");
            return true;
        }

        (new RedstoneCtrlData(world.provider.getDimension(), pos, risingEdgeURL, fallingEdgeURL)).sendTo((EntityPlayerMP) player);
        return true;
    }

    @Override
    public void onNeighborChange(Block neighborType, BlockPos neighborPos) {
        boolean hasPower = (world.isBlockPowered(pos) || world.isBlockPowered(pos.up())); //Same as dispenser

        if(hasPower != state) {
            state = hasPower;

            if(state) //Rising edge
                changeURL(risingEdgeURL);
            else //Falling edge
                changeURL(fallingEdgeURL);
        }
    }

    public void setURLs(String r, String f) {
        risingEdgeURL = r.trim();
        fallingEdgeURL = f.trim();
        markDirty();
    }

    private void changeURL(String url) {
        if(world.isRemote || url.isEmpty())
            return;

        if(isScreenChunkLoaded()) {
            TileEntityScreen tes = getConnectedScreen();

            if(tes != null)
                tes.setScreenURL(screenSide, url);
        }
    }

}
