/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.utilities.BlockSide;

import java.util.Arrays;

@Message(messageId = 13, side = Side.CLIENT)
public class CMessageCloseGui implements IMessage, Runnable {

    private BlockPos blockPos;
    private BlockSide blockSide;

    public CMessageCloseGui() {
    }

    public CMessageCloseGui(BlockPos bp) {
        blockPos = bp;
        blockSide = null;
    }

    public CMessageCloseGui(BlockPos bp, BlockSide side) {
        blockPos = bp;
        blockSide = side;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int x, y, z, side;
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        side = buf.readByte();

        blockPos = new BlockPos(x, y, z);
        if(side <= 0)
            blockSide = null;
        else
            blockSide = BlockSide.values()[side - 1];
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(blockPos.getX());
        buf.writeInt(blockPos.getY());
        buf.writeInt(blockPos.getZ());

        if(blockSide == null)
            buf.writeByte(0);
        else
            buf.writeByte(blockSide.ordinal() + 1);
    }

    @Override
    public void run() {
        if(blockSide == null)
            Arrays.stream(BlockSide.values()).forEach(s -> WebDisplays.PROXY.closeGui(blockPos, s));
        else
            WebDisplays.PROXY.closeGui(blockPos, blockSide);
    }

}
