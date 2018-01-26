/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.client.gui.GuiKeyboard;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Vector3i;

public class KeyboardData extends GuiData {

    public Vector3i pos;
    public BlockSide side;
    public int kbX;
    public int kbY;
    public int kbZ;

    public KeyboardData() {
    }

    public KeyboardData(TileEntityScreen tes, BlockSide side, BlockPos kbPos) {
        pos = new Vector3i(tes.getPos());
        this.side = side;
        kbX = kbPos.getX();
        kbY = kbPos.getY();
        kbZ = kbPos.getZ();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiScreen createGui(GuiScreen old, World world) {
        TileEntity te = world.getTileEntity(pos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen)) {
            Log.error("TileEntity at %s is not a screen; can't open keyboard!", pos.toString());
            return null;
        }

        return new GuiKeyboard((TileEntityScreen) te, side, new BlockPos(kbX, kbY, kbZ));
    }

    @Override
    public String getName() {
        return "Keyboard";
    }

}
