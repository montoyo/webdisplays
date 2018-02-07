/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.TileEntityRedCtrl;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector3i;

@Message(messageId = 8, side = Side.SERVER)
public class SMessageRedstoneCtrl implements IMessage, Runnable {

    private EntityPlayer player;
    private int dimension;
    private Vector3i pos;
    private String risingEdgeURL;
    private String fallingEdgeURL;

    public SMessageRedstoneCtrl() {
    }

    public SMessageRedstoneCtrl(int d, Vector3i p, String r, String f) {
        dimension = d;
        pos = p;
        risingEdgeURL = r;
        fallingEdgeURL = f;
    }

    @Override
    public void run() {
        World world = player.world;
        BlockPos blockPos = pos.toBlock();
        final double maxRange = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();

        if(world.provider.getDimension() != dimension || player.getDistanceSq(blockPos) > maxRange * maxRange)
            return;

        TileEntity te = player.world.getTileEntity(blockPos);
        if(te == null || !(te instanceof TileEntityRedCtrl))
            return;

        TileEntityRedCtrl redCtrl = (TileEntityRedCtrl) te;
        if(!redCtrl.isScreenChunkLoaded()) {
            Util.toast(player, "chunkUnloaded");
            return;
        }

        TileEntityScreen tes = redCtrl.getConnectedScreen();
        if(tes == null)
            return;

        if((tes.getScreen(redCtrl.getScreenSide()).rightsFor(player) & ScreenRights.CHANGE_URL) == 0)
            return;

        redCtrl.setURLs(risingEdgeURL, fallingEdgeURL);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        pos = new Vector3i(buf);
        risingEdgeURL = ByteBufUtils.readUTF8String(buf);
        fallingEdgeURL = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        pos.writeTo(buf);
        ByteBufUtils.writeUTF8String(buf, risingEdgeURL);
        ByteBufUtils.writeUTF8String(buf, fallingEdgeURL);
    }

    public static class Handler implements IMessageHandler<SMessageRedstoneCtrl, IMessage> {

        @Override
        public IMessage onMessage(SMessageRedstoneCtrl msg, MessageContext ctx) {
            msg.player = ctx.getServerHandler().player;
            ((WorldServer) msg.player.world).addScheduledTask(msg);
            return null;
        }

    }
}
