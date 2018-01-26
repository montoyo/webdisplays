/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net;

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
import net.montoyo.wd.utilities.NameUUIDPair;

import java.util.ArrayList;

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

        if(!matchExact)
            beginning = beginning.toLowerCase();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, beginning);
        buf.writeBoolean(matchExact);
    }

    @Override
    public void run() {
        GameProfile[] profiles = WebDisplays.PROXY.getOnlineGameProfiles();

        if(matchExact) {
            for(GameProfile gp : profiles) {
                if(gp.getName().equals(beginning)) {
                    WebDisplays.NET_HANDLER.sendTo(new CMessageACResult(gp), player);
                    return;
                }

                WebDisplays.NET_HANDLER.sendTo(new CMessageACResult(new NameUUIDPair[0]), player);
            }
        } else {
            ArrayList<NameUUIDPair> results = new ArrayList<>();

            for(GameProfile gp : profiles) {
                if(gp.getName().toLowerCase().startsWith(beginning)) {
                    results.add(new NameUUIDPair(gp));
                    break;
                }
            }

            WebDisplays.NET_HANDLER.sendTo(new CMessageACResult(results.toArray(new NameUUIDPair[0])), player);
        }
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
