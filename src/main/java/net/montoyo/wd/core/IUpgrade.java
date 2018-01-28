/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IUpgrade {

    void onInstall(@Nonnull TileEntityScreen tes, @Nonnull BlockSide screenSide, @Nullable EntityPlayer player, @Nonnull ItemStack is);
    boolean onRemove(@Nonnull TileEntityScreen tes, @Nonnull BlockSide screenSide, @Nullable EntityPlayer player, @Nonnull ItemStack is); //Return true to prevent dropping
    void updateUpgrade(@Nonnull TileEntityScreen tes, @Nonnull BlockSide screenSide, @Nonnull ItemStack is);

}
