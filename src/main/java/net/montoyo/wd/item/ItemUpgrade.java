/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemUpgrade extends ItemMulti implements IUpgrade, WDItem {

    public ItemUpgrade() {
        super(DefaultUpgrade.class);
        setUnlocalizedName("webdisplays.upgrade");
        setRegistryName("upgrade");
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
    public void addInformation(ItemStack is, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        tt.add("" + ChatFormatting.ITALIC + I18n.format("item.webdisplays.upgrade.name"));
        WDItem.addInformation(tt);
    }

    @Override
    public String getJSName(@Nonnull ItemStack is) {
        int meta = is.getMetadata();
        DefaultUpgrade[] upgrades = DefaultUpgrade.values();

        if(meta < 0 || meta >= upgrades.length)
            return "webdisplays:wtf";
        else
            return "webdisplays:" + upgrades[meta];
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return DefaultUpgrade.getWikiName(is.getMetadata());
    }

}
