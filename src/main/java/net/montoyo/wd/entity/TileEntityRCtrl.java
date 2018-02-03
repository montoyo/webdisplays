/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.SetURLData;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Util;

public class TileEntityRCtrl extends TileEntityPeripheralBase {

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

        (new SetURLData(screenPos, screenSide, scr.url, pos)).sendTo((EntityPlayerMP) player);
        return true;
    }

}
