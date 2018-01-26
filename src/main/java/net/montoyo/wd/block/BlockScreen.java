/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.SetURLData;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.*;

import javax.annotation.Nullable;

public class BlockScreen extends WDBlockContainer {

    public static final PropertyBool hasTE = PropertyBool.create("haste");
    private static final IProperty[] properties = new IProperty[] { hasTE };
    public static final IUnlistedProperty<Integer>[] sideFlags = new IUnlistedProperty[6];
    static {
        for(int i = 0; i < sideFlags.length; i++)
            sideFlags[i] = Properties.toUnlisted(PropertyInteger.create("neighbor" + i, 0, 15));
    }

    public static final int BAR_BOT = 1;
    public static final int BAR_RIGHT = 2;
    public static final int BAR_TOP = 4;
    public static final int BAR_LEFT = 8;

    public BlockScreen() {
        super(Material.ROCK);
        setHardness(1.5f);
        setResistance(10.f);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
        setName("screen");
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, properties, sideFlags);
    }

    public static boolean isScreenBlock(IBlockAccess world, Vector3i pos) {
        return world.getBlockState(pos.toBlock()).getBlock() instanceof BlockScreen;
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos bpos) {
        IExtendedBlockState ret = (IExtendedBlockState) blockState.getBaseState();
        Vector3i pos = new Vector3i(bpos);

        for(BlockSide side : BlockSide.values()) {
            int icon = 0;
            if(!isScreenBlock(world, side.up.clone().add(pos)))    icon |= BAR_TOP;
            if(!isScreenBlock(world, side.down.clone().add(pos)))  icon |= BAR_BOT;
            if(!isScreenBlock(world, side.left.clone().add(pos)))  icon |= BAR_LEFT;
            if(!isScreenBlock(world, side.right.clone().add(pos))) icon |= BAR_RIGHT;

            ret = ret.withProperty(sideFlags[side.ordinal()], icon);
        }

        return ret;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(hasTE, meta != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(hasTE) ? 1 : 0;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos bpos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if(!player.getHeldItem(hand).isEmpty())
            return false;

        if(world.isRemote)
            return true;

        boolean sneaking = player.isSneaking();
        Vector3i pos = new Vector3i(bpos);
        BlockSide side = BlockSide.values()[facing.ordinal()];

        Multiblock.findOrigin(world, pos, side, null);
        TileEntityScreen te = (TileEntityScreen) world.getTileEntity(pos.toBlock());

        if(te != null && te.getScreen(side) != null) {
            TileEntityScreen.Screen scr = te.getScreen(side);

            if(sneaking) { //Set URL
                if((scr.rightsFor(player) & ScreenRights.CHANGE_URL) == 0)
                    Util.toast(player, "restrictions");
                else
                    (new SetURLData(pos, scr.side, scr.url)).sendTo((EntityPlayerMP) player);

                return true;
            } else {
                if((scr.rightsFor(player) & ScreenRights.CLICK) == 0) {
                    Util.toast(player, "restrictions");
                    return true;
                }

                if(side.right.x < 0)
                    hitX -= 1.f;

                if(side.right.z < 0 || side == BlockSide.TOP || side == BlockSide.BOTTOM)
                    hitZ -= 1.f;

                Vector3f rel = new Vector3f(bpos.getX(), bpos.getY(), bpos.getZ());
                rel.sub((float) pos.x, (float) pos.y, (float) pos.z);
                rel.add(hitX, hitY, hitZ);

                float cx = rel.dot(side.right.toFloat()) - 2.f / 16.f;
                float cy = rel.dot(side.up.toFloat()) - 2.f / 16.f;
                float sw = ((float) scr.size.x) - 4.f / 16.f;
                float sh = ((float) scr.size.y) - 4.f / 16.f;

                cx /= sw;
                cy /= sh;

                if(cx >= 0.f && cx <= 1.0 && cy >= 0.f && cy <= 1.f) {
                    if(side != BlockSide.BOTTOM)
                        cy = 1.f - cy;

                    cx *= (float) scr.resolution.x;
                    cy *= (float) scr.resolution.y;
                    te.click(side, new Vector2i((int) cx, (int) cy));
                }

                return true;
            }
        } else if(sneaking) {
            Util.toast(player, "turnOn");
            return true;
        }

        Vector2i size = Multiblock.measure(world, pos, side);
        if(size.x < 2 || size.y < 2) {
            Util.toast(player, "tooSmall");
            return true;
        }

        Vector3i err = Multiblock.check(world, pos, size, side);
        if(err != null) {
            Util.toast(player, "invalid", err.toString());
            return true;
        }

        boolean created = false;
        Log.info("Structure at %s of size %dx%d", pos.toString(), size.x, size.y);

        if(te == null) {
            BlockPos bp = pos.toBlock();
            world.setBlockState(bp, getDefaultState().withProperty(hasTE, true));
            te = (TileEntityScreen) world.getTileEntity(bp);
            created = true;
        }

        te.addScreen(side, size, null, !created).setOwner(player);
        return true;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return meta == 0 ? null : new TileEntityScreen();
    }

    @Override
    public void onBlockDestroyedByPlayer(World world, BlockPos pos, IBlockState dontCare) {
        if(!world.isRemote) {
            Vector3i bp = new Vector3i(pos);
            Multiblock.BlockOverride override = new Multiblock.BlockOverride(bp, Multiblock.OverrideAction.SIMULATE);

            for(BlockSide bs: BlockSide.values())
                destroySide(world, bp.clone(), bs, override);
        }
    }

    private void destroySide(World world, Vector3i pos, BlockSide side, Multiblock.BlockOverride override) {
        Multiblock.findOrigin(world, pos, side, override);
        BlockPos bp = pos.toBlock();
        TileEntity te = world.getTileEntity(bp);

        if(te != null && te instanceof TileEntityScreen)
            world.setBlockState(bp, getDefaultState().withProperty(hasTE, false)); //Destroy tile entity
    }

    @Override
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.IGNORE;
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        onBlockDestroyedByPlayer(world, pos, null);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase whoDidThisShit, ItemStack stack) {
        if(world.isRemote)
            return;

        Multiblock.BlockOverride override = new Multiblock.BlockOverride(new Vector3i(pos), Multiblock.OverrideAction.IGNORE);
        Vector3i[] neighbors = new Vector3i[6];

        neighbors[0] = new Vector3i(pos.getX() + 1, pos.getY(), pos.getZ());
        neighbors[1] = new Vector3i(pos.getX() - 1, pos.getY(), pos.getZ());
        neighbors[2] = new Vector3i(pos.getX(), pos.getY() + 1, pos.getZ());
        neighbors[3] = new Vector3i(pos.getX(), pos.getY() - 1, pos.getZ());
        neighbors[4] = new Vector3i(pos.getX(), pos.getY(), pos.getZ() + 1);
        neighbors[5] = new Vector3i(pos.getX(), pos.getY(), pos.getZ() - 1);

        for(Vector3i neighbor: neighbors) {
            if(world.getBlockState(neighbor.toBlock()).getBlock() instanceof BlockScreen) {
                for(BlockSide bs: BlockSide.values())
                    destroySide(world, neighbor.clone(), bs, override);
            }
        }
    }

}
