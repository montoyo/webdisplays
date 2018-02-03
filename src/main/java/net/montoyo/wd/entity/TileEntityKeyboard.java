/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumHand;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.KeyboardData;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Util;

public class TileEntityKeyboard extends TileEntityPeripheralBase {

    private static final String RANDOM_CHARS = "AZERTYUIOPQSDFGHJKLMWXCVBNazertyuiopqsdfghjklmwxcvbn0123456789"; //Yes I have an AZERTY keyboard, u care?

    @Override
    public boolean onRightClick(EntityPlayer player, EnumHand hand, BlockSide side) {
        if(world.isRemote)
            return true;

        if(!isScreenChunkLoaded()) {
            Util.toast(player, "chunkUnloaded");
            return true;
        }

        TileEntityScreen tes = getConnectedScreen();
        if(tes == null) {
            Util.toast(player, "notLinked");
            return true;
        }

        TileEntityScreen.Screen scr = tes.getScreen(screenSide);
        if((scr.rightsFor(player) & ScreenRights.CLICK) == 0) {
            Util.toast(player, "restrictions");
            return true;
        }

        (new KeyboardData(tes, screenSide, pos)).sendTo((EntityPlayerMP) player);
        return true;
    }

    public void simulateCat(Entity ent) {
        if(isScreenChunkLoaded()) {
            TileEntityScreen tes = getConnectedScreen();

            if(tes != null) {
                TileEntityScreen.Screen scr = tes.getScreen(screenSide);
                boolean ok;

                if(ent instanceof EntityPlayer)
                    ok = (scr.rightsFor((EntityPlayer) ent) & ScreenRights.CLICK) != 0;
                else
                    ok = (scr.otherRights & ScreenRights.CLICK) != 0;

                if(ok) {
                    char rnd = RANDOM_CHARS.charAt((int) (Math.random() * ((double) RANDOM_CHARS.length())));
                    tes.type(screenSide, "t" + rnd, pos);

                    EntityPlayer owner = world.getPlayerEntityByUUID(scr.owner.uuid);
                    if(owner != null && owner instanceof EntityPlayerMP && ent instanceof EntityOcelot)
                        WebDisplays.INSTANCE.criterionKeyboardCat.trigger(((EntityPlayerMP) owner).getAdvancements());
                }
            }
        }
    }

}
