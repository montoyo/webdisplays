/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.utilities.Vector3i;

@Message(messageId = 1, side = Side.SERVER)
public class SMessageRequestTEData implements IMessage, Runnable {

    private int dim;
    private Vector3i pos;
    private EntityPlayerMP player;

    public SMessageRequestTEData() {
    }

    public SMessageRequestTEData(TileEntity te) {
        dim = te.getWorld().provider.getDimension();
        pos = new Vector3i(te.getPos());
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dim = buf.readInt();
        pos = new Vector3i(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dim);
        pos.writeTo(buf);
    }

    @Override
    public void run() {
        if(player.world.provider.getDimension() != dim)
            return;

        BlockPos bp = pos.toBlock();
        if(player.getDistanceSq(bp) > 512.0 * 512.0)
            return;

        TileEntity te = player.world.getTileEntity(bp);
        if(te == null) {
            Log.error("MesageRequestTEData: Can't request data of null tile entity at %s", pos.toString());
            return;
        }

        if(te instanceof TileEntityScreen)
            ((TileEntityScreen) te).requestData(player);
    }

    public static class Handler implements IMessageHandler<SMessageRequestTEData, IMessage> {

        @Override
        public IMessage onMessage(SMessageRequestTEData message, MessageContext ctx) {
            message.player = ctx.getServerHandler().player;
            ((WorldServer) message.player.world).addScheduledTask(message);
            return null;
        }

    }

}
