/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.montoyo.wd.entity.TileEntityCCInterface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheralProvider", modid = "computercraft")
public class CCPeripheralProvider implements IPeripheralProvider {

    private CCPeripheralProvider() {
    }

    @Optional.Method(modid = "computercraft")
    @Nullable
    @Override
    public IPeripheral getPeripheral(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing f) {
        TileEntity te = world.getTileEntity(pos);
        return (te instanceof TileEntityCCInterface) ? ((TileEntityCCInterface) te) : null;
    }

    @Optional.Method(modid = "computercraft")
    public static void register() {
        ComputerCraftAPI.registerPeripheralProvider(new CCPeripheralProvider());
    }

}
