/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import net.minecraft.item.ItemStack;

public interface IItemRenderer {

    void render(ItemStack is, float handSideSign, float swingProgress, float equipProgress);

}
