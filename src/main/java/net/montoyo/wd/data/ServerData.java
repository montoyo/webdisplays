/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.client.gui.GuiServer;
import net.montoyo.wd.utilities.NameUUIDPair;

public class ServerData extends GuiData {

    public NameUUIDPair owner;

    public ServerData() {
    }

    public ServerData(NameUUIDPair owner) {
        this.owner = owner;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public GuiScreen createGui(GuiScreen old, World world) {
        return new GuiServer(owner);
    }

    @Override
    public String getName() {
        return "Server";
    }

}
