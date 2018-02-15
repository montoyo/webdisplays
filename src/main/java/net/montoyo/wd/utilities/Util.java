/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.UUID;

public abstract class Util {

    public static void serialize(ByteBuf bb, Object f) {
        Class<?> cls = f.getClass();

        if(cls == Integer.class || cls == Integer.TYPE)
            bb.writeInt((Integer) f);
        else if(cls == Float.class || cls == Float.TYPE)
            bb.writeFloat((Float) f);
        else if(cls == Double.class || cls == Double.TYPE)
            bb.writeDouble((Double) f);
        else if(cls == Boolean.class || cls == Boolean.TYPE)
            bb.writeBoolean((Boolean) f);
        else if(cls == String.class)
            ByteBufUtils.writeUTF8String(bb, (String) f);
        else if(cls == NameUUIDPair.class)
            ((NameUUIDPair) f).writeTo(bb);
        else if(cls.isEnum())
            bb.writeByte(((Enum<?>) f).ordinal());
        else if(cls.isArray()) {
            Object[] ray = (Object[]) f;

            bb.writeInt(ray.length);
            for(Object o : ray)
                serialize(bb, o);
        } else if(!cls.isPrimitive()) {
            Field[] fields = cls.getFields();

            for(Field ff : fields) {
                try {
                    if(ff.getAnnotation(DontSerialize.class) == null && !Modifier.isStatic(ff.getModifiers()))
                        serialize(bb, ff.get(f));
                } catch(IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException(String.format("Caught IllegalAccessException for %s.%s", cls.getName(), ff.getName()));
                }
            }
        } else
            throw new RuntimeException(String.format("Cannot transmit class %s over network!", cls.getName()));
    }

    public static Object unserialize(ByteBuf bb, Class cls) {
        if(cls == Integer.class || cls == Integer.TYPE)
            return bb.readInt();
        else if(cls == Float.class || cls == Float.TYPE)
            return bb.readFloat();
        else if(cls == Double.class || cls == Double.TYPE)
            return bb.readDouble();
        else if(cls == Boolean.class || cls == Boolean.TYPE)
            return bb.readBoolean();
        else if(cls == String.class)
            return ByteBufUtils.readUTF8String(bb);
        else if(cls == NameUUIDPair.class)
            return new NameUUIDPair(bb);
        else if(cls.isEnum())
            return cls.getEnumConstants()[bb.readByte()];
        else if(cls.isArray()) {
            Object[] ray = new Object[bb.readInt()];

            for(int i = 0; i < ray.length; i++)
                ray[i] = unserialize(bb, cls.getComponentType());

            return Arrays.copyOf(ray, ray.length, cls);
        } else if(!cls.isPrimitive()) {
            Object ret;
            Field[] fields = cls.getFields();

            try {
                ret = cls.newInstance();
            } catch(InstantiationException e) {
                e.printStackTrace();
                throw new RuntimeException(String.format("Caught InstantiationException for class %s", cls.getName()));
            } catch(IllegalAccessException e) {
                e.printStackTrace();
                throw new RuntimeException(String.format("Caught IllegalAccessException for class %s", cls.getName()));
            }

            for(Field ff : fields) {
                try {
                    if(ff.getAnnotation(DontSerialize.class) == null && !Modifier.isStatic(ff.getModifiers()))
                        ff.set(ret, unserialize(bb, ff.getType()));
                } catch(IllegalAccessException e) {
                    throw new RuntimeException(String.format("Caught IllegalAccessException for %s.%s", cls.getName(), ff.getName()));
                }
            }

            return ret;
        } else
            throw new RuntimeException(String.format("Cannot unserialize class %s!", cls.getName()));
    }

    public static String addSlashes(String str) {
        String out = "";
        for(int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if(c == '\\')
                out += "\\\\";
            else if(c == '\"')
                out += "\\\"";
            else
                out += c;
        }

        return out;
    }

    public static int scrambleKey(int idx) {
        idx = idx * 0x9E3779B9;
        return idx ^ (idx >> 16);
    }

    public static void toast(EntityPlayer player, String key, Object... data) {
        toast(player, TextFormatting.RED, key, data);
    }

    public static void toast(EntityPlayer player, TextFormatting color, String key, Object... data) {
        ITextComponent root = new TextComponentString("[WebDisplays] ");
        root.setStyle((new Style()).setColor(color));
        root.appendSibling(new TextComponentTranslation("webdisplays.message." + key, data));

        player.sendMessage(root);
    }

    public static void silentClose(Object obj) {
        try {
            obj.getClass().getMethod("close").invoke(obj);
        } catch(Throwable t) {}
    }

    public static String addProtocol(String str) {
        return (str.isEmpty() || str.contains("://")) ? str : ("http://" + str);
    }

    public static boolean isFileNameInvalid(String fname) {
        return fname.isEmpty() || fname.length() > 64 || fname.charAt(0) == '.' || fname.indexOf('/') >= 0 || fname.indexOf('\\') >= 0;
    }

    public static final String[] SIZES = { "bytes", "KiB", "MiB", "GiB", "TiB" };

    public static String sizeString(long l) {
        double d = (double) l;
        int size = 0;

        while(l >= 1024L && size + 1 < SIZES.length) {
            d /= 1024.0;
            l /= 1024L;
            size++;
        }

        return String.format("%.2f %s", d, SIZES[size]);
    }

    public static String join(String[] array, String sep) {
        StringJoiner j = new StringJoiner(sep);
        Arrays.stream(array).forEach(j::add);
        return j.toString();
    }

    public static NBTTagCompound writeOwnerToNBT(NBTTagCompound tag, NameUUIDPair owner) {
        if(owner != null) {
            tag.setLong("OwnerMSB", owner.uuid.getMostSignificantBits());
            tag.setLong("OwnerLSB", owner.uuid.getLeastSignificantBits());
            tag.setString("OwnerName", owner.name);
        }

        return tag;
    }

    public static NameUUIDPair readOwnerFromNBT(NBTTagCompound tag) {
        long msb = tag.getLong("OwnerMSB");
        long lsb = tag.getLong("OwnerLSB");
        String str = tag.getString("OwnerName");

        return new NameUUIDPair(str, new UUID(msb, lsb));
    }

}
