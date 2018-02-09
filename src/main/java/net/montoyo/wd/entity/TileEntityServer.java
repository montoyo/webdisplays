/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
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

    public boolean onPlayerRightClick(EntityPlayer ply) {
        if(world.isRemote)
            return true;

        //TODO: Check if miniserv is disabled
        //Util.toast(ply, "noMiniserv");

        if(owner != null && ply instanceof EntityPlayerMP)
            (new ServerData(owner)).sendTo((EntityPlayerMP) ply);

        return true;
    }

}
