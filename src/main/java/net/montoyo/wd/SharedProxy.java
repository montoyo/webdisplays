/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.server.FMLServerHandler;
import net.montoyo.mcef.utilities.Log;
import net.montoyo.wd.data.GuiData;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3i;

public class SharedProxy {

    public static final int CURRENT_DIMENSION = Integer.MAX_VALUE;

    public void preInit() {
    }

    public void init() {
    }

    public void postInit() {
    }

    public World getWorld(int dim) {
        if(dim == CURRENT_DIMENSION)
            throw new RuntimeException("Current dimension not available server side...");

        return DimensionManager.getWorld(dim);
    }

    public void enqueue(Runnable r) {
        FMLServerHandler.instance().getServer().addScheduledTask(r);
    }

    public void displayGui(GuiData data) {
        Log.error("Called SharedProxy.displayGui() on server side...");
    }

    public void trackScreen(TileEntityScreen tes, boolean track) {
    }

    public void onAutocompleteResult(NameUUIDPair pairs[]) {
    }

    public GameProfile[] getOnlineGameProfiles() {
        return FMLServerHandler.instance().getServer().getOnlinePlayerProfiles();
    }

    public void screenUpdateResolutionInGui(Vector3i pos, BlockSide side, Vector2i res) {
    }

    public void displaySetPadURLGui(String padURL) {
        Log.error("Called SharedProxy.displaySetPadURLGui() on server side...");
    }

    public void openMinePadGui(int padId) {
        Log.error("Called SharedProxy.openMinePadGui() on server side...");
    }

}
