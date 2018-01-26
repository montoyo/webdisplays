/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Vector3i;

public interface IPeripheral {

    boolean connect(World world, BlockPos blockPos, IBlockState blockState, Vector3i screenPos, BlockSide screenSide);

}
