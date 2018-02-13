/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.montoyo.wd.WebDisplays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface WDItem {

    @Nullable
    String getWikiName(@Nonnull ItemStack is);

    static void addInformation(@Nullable List<String> tt) {
        if(tt != null && WebDisplays.PROXY.isShiftDown())
            tt.add("" + ChatFormatting.GRAY + I18n.format("item.webdisplays.wiki"));
    }

}
