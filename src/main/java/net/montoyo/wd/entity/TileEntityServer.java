/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.data.ServerData;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Util;

import javax.annotation.Nonnull;

public class TileEntityServer extends TileEntity {

    private NameUUIDPair owner;

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        owner = Util.readOwnerFromNBT(tag);
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        return Util.writeOwnerToNBT(tag, owner);
    }

    public void setOwner(EntityPlayer ep) {
        owner = new NameUUIDPair(ep.getGameProfile());
        markDirty();
    }

    public void onPlayerRightClick(EntityPlayer ply) {
        if(world.isRemote)
            return;

        if(WebDisplays.INSTANCE.miniservPort == 0)
            Util.toast(ply, "noMiniserv");
        else if(owner != null && ply instanceof EntityPlayerMP)
            (new ServerData(pos, owner)).sendTo((EntityPlayerMP) ply);
    }

}
