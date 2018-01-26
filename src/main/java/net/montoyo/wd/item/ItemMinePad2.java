/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;

import javax.annotation.Nullable;
import java.util.List;

public class ItemMinePad2 extends Item {

    public ItemMinePad2() {
        setUnlocalizedName("webdisplays.minepad");
        setRegistryName("minepad");
        setMaxStackSize(1);
        setFull3D();
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

    private static String getURL(ItemStack is) {
        if(is.getTagCompound() == null || !is.getTagCompound().hasKey("PadURL"))
            return WebDisplays.INSTANCE.homePage;
        else
            return is.getTagCompound().getString("PadURL");
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer ply, EnumHand hand) {
        ItemStack is = ply.getHeldItem(hand);

        if(world.isRemote) {
            if(ply.isSneaking())
                WebDisplays.PROXY.displaySetPadURLGui(getURL(is));
            else if(is.getTagCompound() != null && is.getTagCompound().hasKey("PadID"))
                WebDisplays.PROXY.openMinePadGui(is.getTagCompound().getInteger("PadID"));
        }

        return ActionResult.newResult(EnumActionResult.SUCCESS, is);
    }

    @Override
    public void addInformation(ItemStack is, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        if(is == null || is.getTagCompound() == null || !is.getTagCompound().hasKey("PadID"))
            tt.add("" + ChatFormatting.ITALIC + ChatFormatting.GRAY + I18n.format("webdisplays.minepad.turnon"));
    }

}
