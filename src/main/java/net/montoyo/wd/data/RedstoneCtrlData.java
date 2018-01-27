/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.client.gui.GuiRedstoneCtrl;
import net.montoyo.wd.utilities.Vector3i;

public class RedstoneCtrlData extends GuiData {

    public int dimension;
    public Vector3i pos;
    public String risingEdgeURL;
    public String fallingEdgeURL;

    public RedstoneCtrlData() {
    }

    public RedstoneCtrlData(int d, BlockPos p, String r, String f) {
        dimension = d;
        pos = new Vector3i(p);
        risingEdgeURL = r;
        fallingEdgeURL = f;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiScreen createGui(GuiScreen old, World world) {
        return new GuiRedstoneCtrl(dimension, pos, risingEdgeURL, fallingEdgeURL);
    }

    @Override
    public String getName() {
        return "RedstoneCtrl";
    }

}
