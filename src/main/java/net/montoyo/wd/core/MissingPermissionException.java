/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.entity.player.EntityPlayerMP;

public class MissingPermissionException extends Exception {

    private final int permission;
    private final EntityPlayerMP player;

    public MissingPermissionException(int p, EntityPlayerMP ply) {
        super("Player " + ply.getName() + " is missing permission " + p);
        permission = p;
        player = ply;
    }

    public int getPermission() {
        return permission;
    }

    public EntityPlayerMP getPlayer() {
        return player;
    }

}
