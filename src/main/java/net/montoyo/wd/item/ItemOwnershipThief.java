/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
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
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemOwnershipThief extends Item implements WDItem {

    public ItemOwnershipThief() {
        setUnlocalizedName("webdisplays.ownerthief");
        setRegistryName("ownerthief");
        setMaxStackSize(1);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
    }

    @Override
    @Nonnull
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos_, EnumHand hand, EnumFacing side_, float hitX, float hitY, float hitZ) {
        if(player.isSneaking())
            return EnumActionResult.PASS;

        if(world.isRemote)
            return EnumActionResult.SUCCESS;

        if(WebDisplays.INSTANCE.disableOwnershipThief) {
            Util.toast(player, "otDisabled");
            return EnumActionResult.SUCCESS;
        }

        ItemStack stack = player.getHeldItem(hand);
        if(stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();

            if(tag.hasKey("PosX") && tag.hasKey("PosY") && tag.hasKey("PosZ") && tag.hasKey("Side")) {
                BlockPos bp = new BlockPos(tag.getInteger("PosX"), tag.getInteger("PosY"), tag.getInteger("PosZ"));
                BlockSide side = BlockSide.values()[tag.getByte("Side")];

                if(!(world.getBlockState(bp).getBlock() instanceof BlockScreen))
                    return EnumActionResult.SUCCESS;

                TileEntity te = world.getTileEntity(bp);
                if(te == null || !(te instanceof TileEntityScreen))
                    return EnumActionResult.SUCCESS;

                TileEntityScreen tes = (TileEntityScreen) te;
                TileEntityScreen.Screen scr = tes.getScreen(side);
                if(scr == null)
                    return EnumActionResult.SUCCESS;

                Log.warning("Owner of screen at %d %d %d, side %s was changed from %s (UUID %s) to %s (UUID %s)", bp.getX(), bp.getY(), bp.getZ(), side.toString(), scr.owner.name, scr.owner.uuid.toString(), player.getName(), player.getGameProfile().getId().toString());
                player.setHeldItem(hand, ItemStack.EMPTY);
                tes.setOwner(side, player);
                Util.toast(player, TextFormatting.AQUA, "newOwner");
                return EnumActionResult.SUCCESS;
            }
        }

        if(!(world.getBlockState(pos_).getBlock() instanceof BlockScreen))
            return EnumActionResult.SUCCESS;

        Vector3i pos = new Vector3i(pos_);
        BlockSide side = BlockSide.values()[side_.ordinal()];
        Multiblock.findOrigin(world, pos, side, null);

        TileEntity te = world.getTileEntity(pos.toBlock());
        if(te == null || !(te instanceof TileEntityScreen)) {
            Util.toast(player, "turnOn");
            return EnumActionResult.SUCCESS;
        }

        if(((TileEntityScreen) te).getScreen(side) == null)
            Util.toast(player, "turnOn");
        else {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("PosX", pos.x);
            tag.setInteger("PosY", pos.y);
            tag.setInteger("PosZ", pos.z);
            tag.setByte("Side", (byte) side.ordinal());

            stack.setTagCompound(tag);
            Util.toast(player, TextFormatting.AQUA, "screenSet");
            Log.warning("Player %s (UUID %s) created an Ownership Thief item for screen at %d %d %d, side %s!", player.getName(), player.getGameProfile().getId().toString(), pos.x, pos.y, pos.z, side.toString());
        }

        return EnumActionResult.SUCCESS;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tt, ITooltipFlag ttFlags) {
        if(stack.hasTagCompound()) {
            NBTTagCompound tag = stack.getTagCompound();

            if(tag.hasKey("PosX") && tag.hasKey("PosY") && tag.hasKey("PosZ") && tag.hasKey("Side")) {
                tt.add("Screen pos: " + tag.getInteger("PosX") + ", " + tag.getInteger("PosY") + ", " + tag.getInteger("PosZ"));
                tt.add("Screen side: " + BlockSide.values()[tag.getByte("Side")].toString());
                WDItem.addInformation(tt);
                return;
            }
        }

        tt.add("" + TextFormatting.RED + "WARNING: Admin tool");
        tt.add("Right click on screen");
        tt.add("and give to new owner.");
        WDItem.addInformation(tt);
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return "Ownership_Thief";
    }

}
