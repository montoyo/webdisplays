/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.Message;

@Message(messageId = 7, side = Side.SERVER)
public class SMessagePadCtrl implements IMessage, Runnable {

    private int id;
    private String url;
    private EntityPlayer player;

    public SMessagePadCtrl() {
    }

    public SMessagePadCtrl(String url) {
        id = -1;
        this.url = url;
    }

    public SMessagePadCtrl(int id, String url) {
        this.id = id;
        this.url = url;
    }

    private boolean matchesMinePadID(ItemStack is) {
        return is.getItem() == WebDisplays.INSTANCE.itemMinePad && is.getTagCompound() != null && is.getTagCompound().hasKey("PadID") && is.getTagCompound().getInteger("PadID") == id;
    }

    @Override
    public void run() {
        if(id < 0) {
            ItemStack is = player.getHeldItem(EnumHand.MAIN_HAND);

            if(is.getItem() == WebDisplays.INSTANCE.itemMinePad) {
                if(url.isEmpty())
                    is.setTagCompound(null); //Shutdown
                else {
                    if(is.getTagCompound() == null)
                        is.setTagCompound(new NBTTagCompound());

                    if(!is.getTagCompound().hasKey("PadID"))
                        is.getTagCompound().setInteger("PadID", WebDisplays.getNextAvailablePadID());

                    is.getTagCompound().setString("PadURL", WebDisplays.applyBlacklist(url));
                }
            }
        } else {
            NonNullList<ItemStack> inv = player.inventory.mainInventory;
            ItemStack target = null;

            for(int i = 0; i < 9; i++) {
                if(matchesMinePadID(inv.get(i))) {
                    target = inv.get(i);
                    break;
                }
            }

            if(target == null && matchesMinePadID(player.inventory.offHandInventory.get(0)))
                target = player.inventory.offHandInventory.get(0);

            if(target != null)
                target.getTagCompound().setString("PadURL", WebDisplays.applyBlacklist(url));
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        id = buf.readInt();
        url = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(id);
        ByteBufUtils.writeUTF8String(buf, url);
    }

    public static class Handler implements IMessageHandler<SMessagePadCtrl, IMessage> {

        @Override
        public IMessage onMessage(SMessagePadCtrl msg, MessageContext ctx) {
            msg.player = ctx.getServerHandler().player;
            ((WorldServer) msg.player.world).addScheduledTask(msg);
            return null;
        }

    }

}
