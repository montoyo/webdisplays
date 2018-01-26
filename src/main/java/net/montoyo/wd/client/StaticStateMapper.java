/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;

public class StaticStateMapper extends StateMapperBase {

    private ModelResourceLocation resLoc;

    public StaticStateMapper(ModelResourceLocation rl) {
        resLoc = rl;
    }

    @Override
    protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
        return resLoc;
    }

}
