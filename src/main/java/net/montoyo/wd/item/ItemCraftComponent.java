/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.CraftComponent;
import net.montoyo.wd.core.HasAdvancement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemCraftComponent extends ItemMulti implements WDItem {

    public ItemCraftComponent() {
        super(CraftComponent.class);
        setUnlocalizedName("webdisplays.craftcomp");
        setRegistryName("craftcomp");
        setCreativeTab(WebDisplays.CREATIVE_TAB);

        //Hide the bad extension card from the creative tab
        creativeTabItems.clear(CraftComponent.BAD_EXTENSION_CARD.ordinal());
    }

    @Override
    public void addInformation(ItemStack is, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        if(WebDisplays.INSTANCE.doHardRecipe && is.getMetadata() == CraftComponent.EXTENSION_CARD.ordinal() && WebDisplays.PROXY.hasClientPlayerAdvancement(WebDisplays.ADV_PAD_BREAK) != HasAdvancement.YES) {
            tt.add("" + ChatFormatting.RED + I18n.format("webdisplays.extcard.cantcraft1"));
            tt.add("" + ChatFormatting.RED + I18n.format("webdisplays.extcard.cantcraft2"));
        } else if(is.getMetadata() == CraftComponent.BAD_EXTENSION_CARD.ordinal())
            tt.add("" + ChatFormatting.RED + I18n.format("webdisplays.extcard.bad"));
        else
            tt.add("" + ChatFormatting.ITALIC + I18n.format("item.webdisplays.craftcomp.name"));

        WDItem.addInformation(tt);
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return CraftComponent.getWikiName(is.getMetadata());
    }

}
