/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.montoyo.mcef.api.IJSQueryCallback;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;

import javax.annotation.Nonnull;

public interface IScreenQueryHandler {

    //args is an array of Doubles or Strings
    //The screen DOES exist, so scr.getScreen(side) is never null
    void handleQuery(@Nonnull IJSQueryCallback cb, @Nonnull TileEntityScreen scr, @Nonnull BlockSide side, @Nonnull Object[] args);

}
