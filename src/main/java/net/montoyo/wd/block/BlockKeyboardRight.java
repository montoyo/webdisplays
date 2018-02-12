/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.DefaultPeripheral;
import net.montoyo.wd.core.IPeripheral;
import net.montoyo.wd.entity.TileEntityKeyboard;
import net.montoyo.wd.item.ItemLinker;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Vector3i;

import javax.annotation.Nonnull;
import java.util.Random;

public class BlockKeyboardRight extends Block implements IPeripheral {

    public static final PropertyInteger facing = PropertyInteger.create("facing", 0, 3);
    private static final IProperty[] properties = new IProperty[] { facing };
    public static final AxisAlignedBB KEYBOARD_AABB = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0 / 16.0, 1.0);

    public BlockKeyboardRight() {
        super(Material.ROCK);
        setHardness(1.5f);
        setResistance(10.f);
        setUnlocalizedName("webdisplays.peripheral.keyboard");
        setRegistryName("keyboard");
        fullBlock = false;
    }

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, properties);
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        return false;
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return KEYBOARD_AABB;
    }

    @Override
    @Nonnull
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(facing, meta);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(facing);
    }

    @Override
    @Nonnull
    public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target, @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
        return new ItemStack(WebDisplays.INSTANCE.blockPeripheral, 1, 0);
    }

    private TileEntityKeyboard getTileEntity(World world, BlockPos pos) {
        for(EnumFacing nf: EnumFacing.HORIZONTALS) {
            BlockPos np = pos.add(nf.getDirectionVec());
            IBlockState ns = world.getBlockState(np);

            if(ns.getBlock() instanceof BlockPeripheral && ns.getValue(BlockPeripheral.type) == DefaultPeripheral.KEYBOARD) {
                TileEntity te = world.getTileEntity(np);
                if(te != null && te instanceof TileEntityKeyboard)
                    return (TileEntityKeyboard) te;

                break;
            }
        }

        return null;
    }

    @Override
    public boolean connect(World world, BlockPos pos, IBlockState state, Vector3i scrPos, BlockSide scrSide) {
        TileEntityKeyboard keyboard = getTileEntity(world, pos);
        return keyboard != null && keyboard.connect(world, pos, state, scrPos, scrSide);
    }

    @Override
    @Nonnull
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.IGNORE;
    }

    public static boolean checkNeighborhood(IBlockAccess world, BlockPos bp, BlockPos ignore) {
        for(EnumFacing neighbor: EnumFacing.HORIZONTALS) {
            BlockPos np = bp.add(neighbor.getDirectionVec());

            if(ignore == null || !np.equals(ignore)) {
                IBlockState state = world.getBlockState(np);

                if(state.getBlock() instanceof BlockPeripheral) {
                    if(state.getValue(BlockPeripheral.type) == DefaultPeripheral.KEYBOARD)
                        return false;
                } else if(state.getBlock() instanceof BlockKeyboardRight)
                    return false;
            }
        }

        return true;
    }

    public void removeLeftPiece(World world, BlockPos pos, boolean dropItem) {
        for(EnumFacing nf: EnumFacing.HORIZONTALS) {
            BlockPos np = pos.add(nf.getDirectionVec());
            IBlockState ns = world.getBlockState(np);

            if(ns.getBlock() instanceof BlockPeripheral && ns.getValue(BlockPeripheral.type) == DefaultPeripheral.KEYBOARD) {
                if(dropItem)
                    ns.getBlock().dropBlockAsItem(world, np, ns, 0);

                world.setBlockToAir(np);
                break;
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborType, BlockPos neighbor) {
        if(world.isRemote)
            return;

        if(neighbor.getX() == pos.getX() && neighbor.getY() == pos.getY() - 1 && neighbor.getZ() == pos.getZ() && world.isAirBlock(neighbor)) {
            removeLeftPiece(world, pos, true);
            world.setBlockToAir(pos);
        }
    }

    @Override
    public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer ply, boolean willHarvest) {
        if(!world.isRemote)
            removeLeftPiece(world, pos, !ply.isCreative());

        return super.removedByPlayer(state, world, pos, ply, willHarvest);
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, BlockPos pos, Explosion explosionIn) {
        if(!world.isRemote)
            removeLeftPiece(world, pos, true);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        double rpos = (entity.posY - ((double) pos.getY())) * 16.0;
        if(!world.isRemote && rpos >= 1.0 && rpos <= 2.0 && Math.random() < 0.25) {
            TileEntityKeyboard tek = getTileEntity(world, pos);

            if(tek != null)
                tek.simulateCat(entity);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if(player.isSneaking())
            return false;

        if(player.getHeldItem(hand).getItem() instanceof ItemLinker)
            return false;

        TileEntityKeyboard tek = getTileEntity(world, pos);
        if(tek != null)
            return tek.onRightClick(player, hand, BlockSide.values()[facing.ordinal()]);

        return false;
    }

}
