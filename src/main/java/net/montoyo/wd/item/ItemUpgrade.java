/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemUpgrade extends Item implements IUpgrade {

    public ItemUpgrade() {
        setUnlocalizedName("webdisplays.upgrade");
        setRegistryName("upgrade");
        setHasSubtypes(true);
        setMaxDamage(0);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

    @Override
    public void onInstall(@Nonnull TileEntityScreen tes, @Nonnull BlockSide screenSide, @Nullable EntityPlayer player, @Nonnull ItemStack is) {
    }

    @Override
    public boolean onRemove(@Nonnull TileEntityScreen tes, @Nonnull BlockSide screenSide, @Nullable EntityPlayer player, @Nonnull ItemStack is) {
        if(is.getMetadata() == DefaultUpgrade.LASER_MOUSE.ordinal())
            tes.clearLaserUser(screenSide);

        return false;
    }

    @Override
    public boolean isSameUpgrade(@Nonnull ItemStack myStack, @Nonnull ItemStack otherStack) {
        return otherStack.getItem() == this && otherStack.getMetadata() == myStack.getMetadata();
    }

    @Override
    @Nonnull
    public String getUnlocalizedName(ItemStack stack) {
        int meta = stack.getMetadata();
        DefaultUpgrade[] names = DefaultUpgrade.values();
        String ret = getUnlocalizedName();

        if(meta >= 0 && meta < names.length)
            return ret + '.' + names[meta].getName();
        else
            return ret;
    }

    @Override
    public void addInformation(ItemStack is, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        tt.add("" + ChatFormatting.ITALIC + I18n.format("item.webdisplays.upgrade.name"));
    }

    @Override
    public void getSubItems(@Nonnull CreativeTabs tab, @Nonnull NonNullList<ItemStack> items) {
        if(isInCreativeTab(tab)) {
            int cnt = DefaultUpgrade.values().length;

            for(int i = 0; i < cnt; i++)
                items.add(new ItemStack(this, 1, i));
        }
    }

    @Override
    public String getJSName(@Nonnull ItemStack is) {
        int meta = is.getMetadata();
        DefaultUpgrade[] upgrades = DefaultUpgrade.values();

        if(meta < 0 || meta >= upgrades.length)
            return "webdisplays:wtf";
        else
            return "webdisplays:" + upgrades[meta].getName();
    }

}
