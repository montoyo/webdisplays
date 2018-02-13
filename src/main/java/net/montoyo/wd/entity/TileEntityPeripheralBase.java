/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.montoyo.wd.core.IPeripheral;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class TileEntityPeripheralBase extends TileEntity implements IPeripheral {

    protected Vector3i screenPos;
    protected BlockSide screenSide;

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);

        if(tag.hasKey("WDScreen", 10)) {
            NBTTagCompound scr = tag.getCompoundTag("WDScreen");
            screenPos = new Vector3i(scr.getInteger("X"), scr.getInteger("Y"), scr.getInteger("Z"));
            screenSide = BlockSide.values()[scr.getByte("Side")];
        } else {
            screenPos = null;
            screenSide = null;
        }
    }

    @Override
    @Nonnull
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);

        if(screenPos != null && screenSide != null) {
            NBTTagCompound scr = new NBTTagCompound();
            scr.setInteger("X", screenPos.x);
            scr.setInteger("Y", screenPos.y);
            scr.setInteger("Z", screenPos.z);
            scr.setByte("Side", (byte) screenSide.ordinal());

            tag.setTag("WDScreen", scr);
        }

        return tag;
    }

    @Override
    public boolean connect(World world_, BlockPos blockPos, IBlockState blockState, Vector3i pos, BlockSide side) {
        TileEntity te = world.getTileEntity(pos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen)) {
            Log.error("TileEntityPeripheralBase.connect(): Tile entity at %s is not a screen!", pos.toString());
            return false;
        }

        if(((TileEntityScreen) te).getScreen(side) == null) {
            Log.error("TileEntityPeripheralBase.connect(): There is no screen at %s on side %s!", pos.toString(), side.toString());
            return false;
        }

        screenPos = pos;
        screenSide = side;
        markDirty();
        return true;
    }

    public boolean isLinked() {
        return screenPos != null && screenSide != null;
    }

    public boolean isScreenChunkLoaded() {
        if(screenPos == null || screenSide == null)
            return true;

        Chunk chunk = world.getChunkProvider().getLoadedChunk(screenPos.x >> 4, screenPos.z >> 4);
        return chunk != null && !chunk.isEmpty();
    }

    @Nullable
    public TileEntityScreen getConnectedScreen() {
        if(screenPos == null || screenSide == null)
            return null;

        TileEntity te = world.getTileEntity(screenPos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen) || ((TileEntityScreen) te).getScreen(screenSide) == null) {
            screenPos = null;
            screenSide = null;
            markDirty();
            return null;
        }

        return (TileEntityScreen) te;
    }

    @Nullable
    public TileEntityScreen getConnectedScreenEx() {
        if(screenPos == null || screenSide == null)
            return null;

        TileEntity te = world.getTileEntity(screenPos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen) || ((TileEntityScreen) te).getScreen(screenSide) == null)
            return null;

        return (TileEntityScreen) te;
    }

    @Nullable
    public Vector3i getScreenPos() {
        return screenPos;
    }

    @Nullable
    public BlockSide getScreenSide() {
        return screenSide;
    }

    public boolean onRightClick(EntityPlayer player, EnumHand hand, BlockSide side) {
        return false;
    }

    public void onNeighborChange(Block neighborType, BlockPos neighborPos) {
    }

}
