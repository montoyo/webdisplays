/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.client.gui.GuiServer;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Vector3i;

public class ServerData extends GuiData {

    public Vector3i pos;
    public NameUUIDPair owner;

    public ServerData() {
    }

    public ServerData(BlockPos bp, NameUUIDPair owner) {
        pos = new Vector3i(bp);
        this.owner = owner;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiScreen createGui(GuiScreen old, World world) {
        return new GuiServer(pos, owner);
    }

    @Override
    public String getName() {
        return "Server";
    }

}
