/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;

import javax.annotation.Nonnull;

public class StaticStateMapper extends StateMapperBase {

    private final ModelResourceLocation resLoc;

    public StaticStateMapper(ModelResourceLocation rl) {
        resLoc = rl;
    }

    @Override
    @Nonnull
    protected ModelResourceLocation getModelResourceLocation(@Nonnull IBlockState state) {
        return resLoc;
    }

}
