/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.Message;
import net.montoyo.wd.net.client.CMessageACResult;
import net.montoyo.wd.utilities.NameUUIDPair;

import java.util.Arrays;

@Message(messageId = 5, side = Side.SERVER)
public class SMessageACQuery implements IMessage, Runnable {

    private EntityPlayerMP player;
    private String beginning;
    private boolean matchExact;

    public SMessageACQuery() {
    }

    public SMessageACQuery(String beg, boolean exact) {
        beginning = beg;
        matchExact = exact;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        beginning = ByteBufUtils.readUTF8String(buf);
        matchExact = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, beginning);
        buf.writeBoolean(matchExact);
    }

    @Override
    public void run() {
        GameProfile[] profiles = WebDisplays.PROXY.getOnlineGameProfiles();
        NameUUIDPair[] result;

        if(matchExact)
            result = Arrays.stream(profiles).filter(gp -> gp.getName().equalsIgnoreCase(beginning)).map(NameUUIDPair::new).toArray(NameUUIDPair[]::new);
        else {
            final String lBeg = beginning.toLowerCase();
            result = Arrays.stream(profiles).filter(gp -> gp.getName().toLowerCase().startsWith(lBeg)).map(NameUUIDPair::new).toArray(NameUUIDPair[]::new);
        }

        WebDisplays.NET_HANDLER.sendTo(new CMessageACResult(result), player);
    }

    public static class Handler implements IMessageHandler<SMessageACQuery, IMessage> {

        @Override
        public IMessage onMessage(SMessageACQuery msg, MessageContext ctx) {
            msg.player = ctx.getServerHandler().player;
            ((WorldServer) msg.player.world).addScheduledTask(msg);
            return null;
        }

    }

}
