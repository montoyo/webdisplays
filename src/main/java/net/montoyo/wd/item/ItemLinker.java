/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.BlockScreen;
import net.montoyo.wd.core.IPeripheral;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemLinker extends Item implements WDItem {

    public ItemLinker() {
        setUnlocalizedName("webdisplays.linker");
        setRegistryName("linker");
        setMaxStackSize(1);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos_, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if(world.isRemote)
            return EnumActionResult.SUCCESS;

        ItemStack stack = player.getHeldItem(hand);
        NBTTagCompound tag = stack.getTagCompound();

        if(tag != null) {
            if(tag.hasKey("ScreenX") && tag.hasKey("ScreenY") && tag.hasKey("ScreenZ") && tag.hasKey("ScreenSide")) {
                IBlockState state = world.getBlockState(pos_);
                IPeripheral target;

                if(state.getBlock() instanceof IPeripheral)
                    target = (IPeripheral) state.getBlock();
                else {
                    TileEntity te = world.getTileEntity(pos_);
                    if(te == null || !(te instanceof IPeripheral)) {
                        if(player.isSneaking()) {
                            Util.toast(player, TextFormatting.GOLD, "linkAbort");
                            stack.setTagCompound(null);
                        } else
                            Util.toast(player, "peripheral");

                        return EnumActionResult.SUCCESS;
                    }

                    target = (IPeripheral) te;
                }

                Vector3i tePos = new Vector3i(tag.getInteger("ScreenX"), tag.getInteger("ScreenY"), tag.getInteger("ScreenZ"));
                BlockSide scrSide = BlockSide.values()[tag.getByte("ScreenSide")];

                if(target.connect(world, pos_, state, tePos, scrSide)) {
                    Util.toast(player, TextFormatting.AQUA, "linked");

                    if(player instanceof EntityPlayerMP)
                        WebDisplays.INSTANCE.criterionLinkPeripheral.trigger(((EntityPlayerMP) player).getAdvancements());
                } else
                    Util.toast(player, "linkError");

                stack.setTagCompound(null);
                return EnumActionResult.SUCCESS;
            }
        }

        if(!(world.getBlockState(pos_).getBlock() instanceof BlockScreen)) {
            Util.toast(player, "notAScreen");
            return EnumActionResult.SUCCESS;
        }

        Vector3i pos = new Vector3i(pos_);
        BlockSide side = BlockSide.values()[facing.ordinal()];
        Multiblock.findOrigin(world, pos, side, null);

        TileEntity te = world.getTileEntity(pos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen)) {
            Util.toast(player, "turnOn");
            return EnumActionResult.SUCCESS;
        }

        TileEntityScreen.Screen scr = ((TileEntityScreen) te).getScreen(side);
        if(scr == null)
            Util.toast(player, "turnOn");
        else if((scr.rightsFor(player) & ScreenRights.MANAGE_UPGRADES) == 0)
            Util.toast(player, "restrictions");
        else {
            tag = new NBTTagCompound();
            tag.setInteger("ScreenX", pos.x);
            tag.setInteger("ScreenY", pos.y);
            tag.setInteger("ScreenZ", pos.z);
            tag.setByte("ScreenSide", (byte) side.ordinal());

            stack.setTagCompound(tag);
            Util.toast(player, TextFormatting.AQUA, "screenSet2");
        }

        return EnumActionResult.SUCCESS;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tt, ITooltipFlag ttFlag) {
        NBTTagCompound tag = stack.getTagCompound();

        if(tag != null) {
            if(tag.hasKey("ScreenX") && tag.hasKey("ScreenY") && tag.hasKey("ScreenZ") && tag.hasKey("ScreenSide")) {
                BlockSide side = BlockSide.fromInt(tag.getByte("ScreenSide"));
                if(side == null)
                    side = BlockSide.BOTTOM;

                tt.add(I18n.format("webdisplays.linker.selectPeripheral"));
                tt.add(I18n.format("webdisplays.linker.posInfo", tag.getInteger("ScreenX"), tag.getInteger("ScreenY"), tag.getInteger("ScreenZ")));
                tt.add(I18n.format("webdisplays.linker.sideInfo", I18n.format("webdisplays.side." + side.toString().toLowerCase())));
                WDItem.addInformation(tt);
                return;
            }
        }

        tt.add(I18n.format("webdisplays.linker.selectScreen"));
        WDItem.addInformation(tt);
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return "Linking_Tool";
    }

}
