/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

import javax.annotation.Nonnull;
import java.util.BitSet;

public class ItemMulti extends Item {

    protected final Enum[] values;
    protected final BitSet creativeTabItems;

    public ItemMulti(Class<? extends Enum> cls) {
        values = cls.getEnumConstants();
        creativeTabItems = new BitSet(values.length);
        creativeTabItems.set(0, values.length);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    @Nonnull
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getMetadata();
        String ret = getUnlocalizedName();

        if(meta >= 0 && meta < values.length)
            return ret + '.' + values[meta];
        else
            return ret;
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if(isInCreativeTab(tab)) {
            for(int i = 0; i < values.length; i++) {
                if(creativeTabItems.get(i))
                    items.add(new ItemStack(this, 1, i));
            }
        }
    }

    public Enum[] getEnumValues() {
        return values;
    }

}
