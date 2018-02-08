/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.client.CMessageOpenGui;

import java.util.HashMap;

public abstract class GuiData {

    private static final HashMap<String, Class<? extends GuiData>> dataTable = new HashMap<>();
    static {
        dataTable.put("SetURL", SetURLData.class);
        dataTable.put("ScreenConfig", ScreenConfigData.class);
        dataTable.put("Keyboard", KeyboardData.class);
        dataTable.put("RedstoneCtrl", RedstoneCtrlData.class);
        dataTable.put("Server", ServerData.class);
    }

    public static Class<? extends GuiData> classOf(String name) {
        return dataTable.get(name);
    }

    @SideOnly(Side.CLIENT)
    public abstract GuiScreen createGui(GuiScreen old, World world);
    public abstract String getName();

    public void sendTo(EntityPlayerMP player) {
        WebDisplays.NET_HANDLER.sendTo(new CMessageOpenGui(this), player);
    }

}
