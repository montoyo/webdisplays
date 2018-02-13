/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;

public abstract class WDBlockContainer extends BlockContainer {

    protected ItemBlock itemBlock;

    public WDBlockContainer(Material material) {
        super(material);
    }

    protected void setName(String name) {
        setUnlocalizedName("webdisplays." + name);
        setRegistryName(name);
    }

    protected abstract ItemBlock createItemBlock();

    public void makeItemBlock() {
        if(itemBlock != null)
            throw new RuntimeException("WDBlockContainer.makeItemBlock() called twice!");

        itemBlock = createItemBlock();
        itemBlock.setUnlocalizedName(getUnlocalizedName());
        itemBlock.setRegistryName(getRegistryName());
    }

    public ItemBlock getItem() {
        return itemBlock;
    }

}
