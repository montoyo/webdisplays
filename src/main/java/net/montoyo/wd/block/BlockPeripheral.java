/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.DefaultPeripheral;
import net.montoyo.wd.entity.*;
import net.montoyo.wd.item.ItemLinker;
import net.montoyo.wd.item.ItemPeripheral;
import net.montoyo.wd.net.client.CMessageCloseGui;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;

import javax.annotation.Nonnull;

public class BlockPeripheral extends WDBlockContainer {

    public static final PropertyEnum<DefaultPeripheral> type = PropertyEnum.create("type", DefaultPeripheral.class);
    public static final PropertyInteger facing = PropertyInteger.create("facing", 0, 3);
    private static final IProperty[] properties = new IProperty[] { type, facing };

    public BlockPeripheral() {
        super(Material.ROCK);
        setHardness(1.5f);
        setResistance(10.f);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
        setName("peripheral");
    }

    @Override
    protected ItemBlock createItemBlock() {
        return new ItemPeripheral(this);
    }

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, properties);
    }

    @Override
    @Nonnull
    public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull EnumFacing nocare, float hitX,
                                            float hitY, float hitZ, int meta, @Nonnull EntityLivingBase placer, EnumHand hand) {
        int rot = MathHelper.floor(((double) (placer.rotationYaw * 4.0f / 360.0f)) + 2.5) & 3;
        return getDefaultState().withProperty(type, DefaultPeripheral.fromMetadata(meta)).withProperty(facing, rot);
    }

    @Override
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {
        for(DefaultPeripheral dp : DefaultPeripheral.values())
            list.add(new ItemStack(getItem(), 1, dp.toMetadata(0)));
    }

    @Override
    @Nonnull
    public IBlockState getStateFromMeta(int meta) {
        DefaultPeripheral dp = DefaultPeripheral.fromMetadata(meta);
        IBlockState state = getDefaultState().withProperty(type, dp);

        if(dp.hasFacing())
            state = state.withProperty(facing, (meta >> 2) & 3);

        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(type).toMetadata(state.getValue(facing));
    }

    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        Class<? extends TileEntity> cls = DefaultPeripheral.fromMetadata(meta).getTEClass();
        if(cls == null)
            return null;

        try {
            return cls.newInstance();
        } catch(Throwable t) {
            Log.errorEx("Couldn't instantiate peripheral TileEntity:", t);
        }

        return null;
    }

    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    public int damageDropped(IBlockState state) {
        return state.getValue(type).toMetadata(0);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if(player.isSneaking())
            return false;

        if(player.getHeldItem(hand).getItem() instanceof ItemLinker)
            return false;

        TileEntity te = world.getTileEntity(pos);

        if(te instanceof TileEntityPeripheralBase)
            return ((TileEntityPeripheralBase) te).onRightClick(player, hand, BlockSide.values()[facing.ordinal()]);
        else if(te instanceof TileEntityServer) {
            ((TileEntityServer) te).onPlayerRightClick(player);
            return true;
        } else
            return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return state.getValue(type) != DefaultPeripheral.KEYBOARD;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return state.getValue(type) != DefaultPeripheral.KEYBOARD;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return state.getValue(type) != DefaultPeripheral.KEYBOARD;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return state.getValue(type) != DefaultPeripheral.KEYBOARD;
    }

    @Override
    public boolean doesSideBlockRendering(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
        return state.getValue(type) != DefaultPeripheral.KEYBOARD;
    }

    @Override
    @Nonnull
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(type) == DefaultPeripheral.KEYBOARD ? BlockKeyboardRight.KEYBOARD_AABB : FULL_BLOCK_AABB;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        if(world.isRemote)
            return;

        if(state.getValue(type) == DefaultPeripheral.KEYBOARD) {
            //Keyboard special treatment
            int f = state.getValue(facing);
            Vec3i dir = EnumFacing.getHorizontal(f).rotateY().getDirectionVec();
            BlockPos left = pos.add(dir);
            BlockPos right = pos.subtract(dir);

            if(!world.isAirBlock(pos.down()) && BlockKeyboardRight.checkNeighborhood(world, pos, null)) {
                if(world.isAirBlock(right) && !world.isAirBlock(right.down()) && BlockKeyboardRight.checkNeighborhood(world, right, pos)) {
                    world.setBlockState(right, WebDisplays.INSTANCE.blockKbRight.getDefaultState().withProperty(BlockKeyboardRight.facing, f));
                    return;
                } else if(world.isAirBlock(left) && !world.isAirBlock(left.down()) && BlockKeyboardRight.checkNeighborhood(world, left, pos)) {
                    world.setBlockState(left, state);
                    world.setBlockState(pos, WebDisplays.INSTANCE.blockKbRight.getDefaultState().withProperty(BlockKeyboardRight.facing, f));
                    return;
                }
            }

            //Not good; remove this shit...
            world.setBlockToAir(pos);
            if(!(placer instanceof EntityPlayer) || !((EntityPlayer) placer).isCreative())
                dropBlockAsItem(world, pos, state, 0);
        } else if(placer instanceof EntityPlayer) {
            TileEntity te = world.getTileEntity(pos);

            if(te instanceof TileEntityServer)
                ((TileEntityServer) te).setOwner((EntityPlayer) placer);
            else if(te instanceof TileEntityInterfaceBase)
                ((TileEntityInterfaceBase) te).setOwner((EntityPlayer) placer);
        }
    }

    @Override
    @Nonnull
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.IGNORE;
    }

    private void removeRightPiece(World world, BlockPos pos) {
        for(EnumFacing nf: EnumFacing.HORIZONTALS) {
            BlockPos np = pos.add(nf.getDirectionVec());

            if(world.getBlockState(np).getBlock() instanceof BlockKeyboardRight) {
                world.setBlockToAir(np);
                break;
            }
        }
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighborType, BlockPos neighbor) {
        TileEntity te = world.getTileEntity(pos);
        if(te != null && te instanceof TileEntityPeripheralBase)
            ((TileEntityPeripheralBase) te).onNeighborChange(neighborType, neighbor);

        if(world.isRemote || state.getValue(type) != DefaultPeripheral.KEYBOARD)
            return;

        if(neighbor.getX() == pos.getX() && neighbor.getY() == pos.getY() - 1 && neighbor.getZ() == pos.getZ() && world.isAirBlock(neighbor)) {
            removeRightPiece(world, pos);
            world.setBlockToAir(pos);
            dropBlockAsItem(world, pos, state, 0);
            WebDisplays.NET_HANDLER.sendToAllAround(new CMessageCloseGui(pos), point(world, pos));
        }
    }

    @Override
    public void onBlockDestroyedByPlayer(World world, BlockPos pos, IBlockState state) {
        if(!world.isRemote) {
            if(state.getBlock() == this && state.getValue(type) == DefaultPeripheral.KEYBOARD)
                removeRightPiece(world, pos);

            WebDisplays.NET_HANDLER.sendToAllAround(new CMessageCloseGui(pos), point(world, pos));
        }
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        onBlockDestroyedByPlayer(world, pos, world.getBlockState(pos));
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if(!world.isRemote && world.getBlockState(pos).getValue(type) == DefaultPeripheral.KEYBOARD) {
            double rpos = (entity.posY - ((double) pos.getY())) * 16.0;

            if(rpos >= 1.0 && rpos <= 2.0 && Math.random() < 0.25) {
                TileEntity te = world.getTileEntity(pos);

                if(te != null && te instanceof TileEntityKeyboard)
                    ((TileEntityKeyboard) te).simulateCat(entity);
            }
        }
    }

    private static NetworkRegistry.TargetPoint point(World world, BlockPos bp) {
        return new NetworkRegistry.TargetPoint(world.provider.getDimension(), (double) bp.getX(), (double) bp.getY(), (double) bp.getZ(), 64.0);
    }

}
