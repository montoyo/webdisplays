/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.CraftComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemCraftComponent extends Item {

    public ItemCraftComponent() {
        setUnlocalizedName("webdisplays.craftcomp");
        setRegistryName("craftcomp");
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

    @Override
    @Nonnull
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getMetadata();
        CraftComponent[] names = CraftComponent.values();
        String ret = getUnlocalizedName();

        if(meta >= 0 && meta < names.length)
            return ret + '.' + names[meta].getName();
        else
            return ret;
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if(isInCreativeTab(tab)) {
            int cnt = CraftComponent.values().length;

            for(int i = 0; i < cnt; i++)
                items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public void addInformation(ItemStack is, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        tt.add("" + ChatFormatting.ITALIC + I18n.format("item.webdisplays.craftcomp.name"));
    }

}
