/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.*;
import net.minecraft.util.text.*;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;

public class Util {

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
            for (int i = 0; i < ray.length; i++)
                serialize(bb, ray[i]);
        } else if(!cls.isPrimitive()) {
            Field[] fields = cls.getFields();

            for(int i = 0; i < fields.length; i++) {
                try {
                    if(fields[i].getAnnotation(DontSerialize.class) == null && !Modifier.isStatic(fields[i].getModifiers()))
                        serialize(bb, fields[i].get(f));
                } catch(IllegalAccessException e) {
                    e.printStackTrace();
                    throw new RuntimeException(String.format("Caught IllegalAccessException for %s.%s", cls.getName(), fields[i].getName()));
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

            for(int i = 0; i < fields.length; i++) {
                try {
                    if(fields[i].getAnnotation(DontSerialize.class) == null && !Modifier.isStatic(fields[i].getModifiers()))
                        fields[i].set(ret, unserialize(bb, fields[i].getType()));
                } catch(IllegalAccessException e) {
                    throw new RuntimeException(String.format("Caught IllegalAccessException for %s.%s", cls.getName(), fields[i].getName()));
                }
            }

            return ret;
        } else
            throw new RuntimeException(String.format("Cannot unserialize class %s!", cls.getName()));
    }

    public static NBTBase serialize(Object f) {
        Class<?> cls = f.getClass();

        if(cls == Integer.class)
            return new NBTTagInt((Integer) f);
        else if(cls == Float.class)
            return new NBTTagFloat((Float) f);
        else if(cls == Double.class)
            return new NBTTagDouble((Double) f);
        else if(cls == String.class)
            return new NBTTagString((String) f);
        else if(cls.isEnum())
            return new NBTTagInt(((Enum<?>) f).ordinal());
        else if(cls.isArray()) {
            Object[] ray = (Object[]) f;
            NBTTagList ret = new NBTTagList();

            for(int i = 0; i < ray.length; i++)
                ret.appendTag(serialize(ray[i]));

            return ret;
        } else
            throw new RuntimeException(String.format("Cannot save class %s as NBT!", cls.getName()));
    }

    public static Object unserialize(NBTBase nbt, Class cls) {
        if(cls == Integer.class || cls == Integer.TYPE)
            return ((NBTTagInt) nbt).getInt();
        else if(cls == Float.class || cls == Float.TYPE)
            return ((NBTTagFloat) nbt).getFloat();
        else if(cls == Double.class || cls == Double.TYPE)
            return ((NBTTagDouble) nbt).getDouble();
        else if(cls == String.class)
            return ((NBTTagString) nbt).getString();
        else if(cls.isEnum())
            return cls.getEnumConstants()[((NBTTagInt) nbt).getInt()];
        else if(cls.isArray()) {
            NBTTagList lst = (NBTTagList) nbt;
            Object[] ray = new Object[lst.tagCount()];

            for(int i = 0; lst.tagCount() > 0; i++)
                ray[i] = unserialize(lst.removeTag(0), cls.getComponentType());

            return Arrays.copyOf(ray, ray.length, cls);
        } else
            throw new RuntimeException(String.format("Cannot load class %s from NBT!", cls.getName()));
    }

    public static String[] commaSplit(String str) {
        ArrayList<String> lst = new ArrayList<String>();
        String out = "";
        boolean escape = false;

        for(int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if(c == '\\' && !escape) {
                escape = true;
                continue; //Otherwise it'll set escape back to false
            } else if(c == ',' && !escape) {
                lst.add(out);
                out = "";
            } else
                out += c;

            if(escape)
                escape = false;
        }

        lst.add(out);

        String[] ret = new String[lst.size()];
        return lst.toArray(ret);
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

    public static int scrambleKey(int idx)
    {
        idx = idx * 0x9E3779B9;
        return idx ^ (idx >> 16);
    }

    public static void toast(EntityPlayer player, String key, Object ... data) {
        toast(player, TextFormatting.RED, key, data);
    }

    public static void toast(EntityPlayer player, TextFormatting color, String key, Object ... data) {
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

}
