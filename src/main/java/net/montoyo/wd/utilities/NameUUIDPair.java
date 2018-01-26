/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.util.UUID;

public final class NameUUIDPair {

    public final String name;
    public final UUID uuid;

    public NameUUIDPair() {
        name = "";
        uuid = new UUID(0L, 0L);
    }

    public NameUUIDPair(String name, UUID uuid) {
        this.name = name;
        this.uuid = uuid;
    }

    public NameUUIDPair(GameProfile profile) {
        name = profile.getName();
        uuid = profile.getId();
    }

    public NameUUIDPair(ByteBuf bb) {
        name = ByteBufUtils.readUTF8String(bb);

        long msb = bb.readLong();
        long lsb = bb.readLong();
        uuid = new UUID(msb, lsb);
    }

    public String getName() {
        return name;
    }

    public UUID getUUID() {
        return uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null || !(obj instanceof NameUUIDPair))
            return false;

        return ((NameUUIDPair) obj).uuid.equals(uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    public boolean isBlank() {
        return name.isEmpty() && uuid.getMostSignificantBits() == 0L && uuid.getLeastSignificantBits() == 0L;
    }

    public void writeTo(ByteBuf bb) {
        ByteBufUtils.writeUTF8String(bb, name);
        bb.writeLong(uuid.getMostSignificantBits());
        bb.writeLong(uuid.getLeastSignificantBits());
    }

}
