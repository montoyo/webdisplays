/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.ItemBlock;

public abstract class WDBlock extends Block {

    protected ItemBlock itemBlock;

    public WDBlock(Material mat, MapColor color) {
        super(mat, color);
    }

    public WDBlock(Material material) {
        super(material);
    }

    protected void setName(String name) {
        setUnlocalizedName("webdisplays." + name);
        setRegistryName(name);
    }

    public void makeItemBlock() {
        if(itemBlock != null)
            throw new RuntimeException("WDBlock.makeItemBlock() called twice!");

        itemBlock = new ItemBlock(this);
        itemBlock.setUnlocalizedName(getUnlocalizedName());
        itemBlock.setRegistryName(getRegistryName());
    }

    public ItemBlock getItem() {
        return itemBlock;
    }

}
