/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.Callable;

public class WDDCapability implements IWDDCapability {

    @CapabilityInject(IWDDCapability.class)
    public static final Capability<IWDDCapability> INSTANCE = null;

    public static class Storage implements Capability.IStorage<IWDDCapability> {

        @Nullable
        @Override
        public NBTBase writeNBT(Capability<IWDDCapability> cap, IWDDCapability inst, EnumFacing side) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("FirstRun", inst.isFirstRun());

            return tag;
        }

        @Override
        public void readNBT(Capability<IWDDCapability> cap, IWDDCapability inst, EnumFacing side, NBTBase nbt) {
            if(nbt instanceof NBTTagCompound) {
                NBTTagCompound tag = (NBTTagCompound) nbt;

                if(tag.hasKey("FirstRun") && tag.getTag("FirstRun") instanceof NBTTagByte && !tag.getBoolean("FirstRun"))
                    inst.clearFirstRun();
            }
        }

    }

    public static class Factory implements Callable<IWDDCapability> {

        @Override
        public IWDDCapability call() throws Exception {
            return new WDDCapability();
        }

    }

    public static class Provider implements ICapabilitySerializable<NBTBase> {

        private IWDDCapability cap = INSTANCE.getDefaultInstance();

        @Override
        public boolean hasCapability(@Nonnull Capability<?> cap, @Nullable EnumFacing f) {
            return cap == INSTANCE;
        }

        @Nullable
        @Override
        public <T> T getCapability(@Nonnull Capability<T> cap, @Nullable EnumFacing f) {
            return cap == INSTANCE ? INSTANCE.cast(this.cap) : null;
        }

        @Override
        public NBTBase serializeNBT() {
            return INSTANCE.getStorage().writeNBT(INSTANCE, cap, null);
        }

        @Override
        public void deserializeNBT(NBTBase nbt) {
            INSTANCE.getStorage().readNBT(INSTANCE, cap, null, nbt);
        }

    }

    private boolean firstRun = true;

    private WDDCapability() {
    }

    @Override
    public boolean isFirstRun() {
        return firstRun;
    }

    @Override
    public void clearFirstRun() {
        firstRun = false;
    }

    @Override
    public void cloneTo(IWDDCapability dst) {
        if(!isFirstRun())
            dst.clearFirstRun();
    }

}
