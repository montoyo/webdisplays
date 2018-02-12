/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.montoyo.wd.client.renderers.IModelBaker;

public class ResourceModelPair {

    private final ModelResourceLocation resLoc;
    private final IModelBaker model;

    public ResourceModelPair(ModelResourceLocation rl, IModelBaker m) {
        resLoc = rl;
        model = m;
    }

    public ModelResourceLocation getResourceLocation() {
        return resLoc;
    }

    public IModelBaker getModel() {
        return model;
    }

}
