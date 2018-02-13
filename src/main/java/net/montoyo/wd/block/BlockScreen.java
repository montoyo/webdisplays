/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.data.SetURLData;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.item.WDItem;
import net.montoyo.wd.utilities.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BlockScreen extends WDBlockContainer {

    public static final PropertyBool hasTE = PropertyBool.create("haste");
    public static final PropertyBool emitting = PropertyBool.create("emitting");
    private static final IProperty[] properties = new IProperty[] { hasTE, emitting };
    public static final IUnlistedProperty<Integer>[] sideFlags = new IUnlistedProperty[6];
    static {
        for(int i = 0; i < sideFlags.length; i++)
            sideFlags[i] = Properties.toUnlisted(PropertyInteger.create("neighbor" + i, 0, 15));
    }

    private static final int BAR_BOT = 1;
    private static final int BAR_RIGHT = 2;
    private static final int BAR_TOP = 4;
    private static final int BAR_LEFT = 8;

    public BlockScreen() {
        super(Material.ROCK);
        setHardness(1.5f);
        setResistance(10.f);
        setCreativeTab(WebDisplays.CREATIVE_TAB);
        setName("screen");
        setDefaultState(blockState.getBaseState().withProperty(hasTE, false).withProperty(emitting, false));
    }

    @Override
    @Nonnull
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @Nonnull
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, properties, sideFlags);
    }

    public static boolean isntScreenBlock(IBlockAccess world, Vector3i pos) {
        return world.getBlockState(pos.toBlock()).getBlock() != WebDisplays.INSTANCE.blockScreen;
    }

    @Override
    @Nonnull
    public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world, BlockPos bpos) {
        IExtendedBlockState ret = (IExtendedBlockState) blockState.getBaseState();
        Vector3i pos = new Vector3i(bpos);

        for(BlockSide side : BlockSide.values()) {
            int icon = 0;
            if(isntScreenBlock(world, side.up.clone().add(pos)))    icon |= BAR_TOP;
            if(isntScreenBlock(world, side.down.clone().add(pos)))  icon |= BAR_BOT;
            if(isntScreenBlock(world, side.left.clone().add(pos)))  icon |= BAR_LEFT;
            if(isntScreenBlock(world, side.right.clone().add(pos))) icon |= BAR_RIGHT;

            ret = ret.withProperty(sideFlags[side.ordinal()], icon);
        }

        return ret;
    }

    @Override
    @Nonnull
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(hasTE, (meta & 1) != 0).withProperty(emitting, (meta & 2) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int ret = 0;
        if(state.getValue(hasTE))
            ret |= 1;

        if(state.getValue(emitting))
            ret |= 2;

        return ret;
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos bpos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack heldItem = player.getHeldItem(hand);
        if(heldItem.isEmpty())
            heldItem = null; //Easier to work with
        else if(!(heldItem.getItem() instanceof IUpgrade))
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
            } else if(heldItem != null && !te.hasUpgrade(side, heldItem)) { //Add upgrade
                if((scr.rightsFor(player) & ScreenRights.MANAGE_UPGRADES) == 0) {
                    Util.toast(player, "restrictions");
                    return true;
                }

                if(te.addUpgrade(side, heldItem, player, false)) {
                    if(!player.isCreative())
                        heldItem.shrink(1);

                    Util.toast(player, TextFormatting.AQUA, "upgradeOk");
                    if(player instanceof EntityPlayerMP)
                        WebDisplays.INSTANCE.criterionUpgradeScreen.trigger(((EntityPlayerMP) player).getAdvancements());
                } else
                    Util.toast(player, "upgradeError");

                return true;
            } else { //Click
                if((scr.rightsFor(player) & ScreenRights.CLICK) == 0) {
                    Util.toast(player, "restrictions");
                    return true;
                }

                Vector2i tmp = new Vector2i();
                if(hit2pixels(side, bpos, pos, scr, hitX, hitY, hitZ, tmp))
                    te.click(side, tmp);

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

        if(size.x > WebDisplays.INSTANCE.maxScreenX || size.y > WebDisplays.INSTANCE.maxScreenY) {
            Util.toast(player, "tooBig", WebDisplays.INSTANCE.maxScreenX, WebDisplays.INSTANCE.maxScreenY);
            return true;
        }

        Vector3i err = Multiblock.check(world, pos, size, side);
        if(err != null) {
            Util.toast(player, "invalid", err.toString());
            return true;
        }

        boolean created = false;
        Log.info("Player %s (UUID %s) created a screen at %s of size %dx%d", player.getName(), player.getGameProfile().getId().toString(), pos.toString(), size.x, size.y);

        if(te == null) {
            BlockPos bp = pos.toBlock();
            world.setBlockState(bp, world.getBlockState(bp).withProperty(hasTE, true));
            te = (TileEntityScreen) world.getTileEntity(bp);
            created = true;
        }

        te.addScreen(side, size, null, player, !created);
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos source) {
        if(block != this && !world.isRemote && !state.getValue(emitting)) {
            for(BlockSide side: BlockSide.values()) {
                Vector3i vec = new Vector3i(pos);
                Multiblock.findOrigin(world, vec, side, null);

                TileEntityScreen tes = (TileEntityScreen) world.getTileEntity(vec.toBlock());
                if(tes != null && tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_INPUT)) {
                    EnumFacing facing = EnumFacing.VALUES[side.reverse().ordinal()]; //Opposite face
                    vec.sub(pos.getX(), pos.getY(), pos.getZ()).neg();
                    tes.updateJSRedstone(side, new Vector2i(vec.dot(side.right), vec.dot(side.up)), world.getRedstonePower(pos, facing));
                }
            }
        }
    }

    public static boolean hit2pixels(BlockSide side, BlockPos bpos, Vector3i pos, TileEntityScreen.Screen scr, float hitX, float hitY, float hitZ, Vector2i dst) {
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

            switch(scr.rotation) {
                case ROT_90:
                    cy = 1.0f - cy;
                    break;

                case ROT_180:
                    cx = 1.0f - cx;
                    cy = 1.0f - cy;
                    break;

                case ROT_270:
                    cx = 1.0f - cx;
                    break;

                default:
                    break;
            }

            cx *= (float) scr.resolution.x;
            cy *= (float) scr.resolution.y;

            if(scr.rotation.isVertical) {
                dst.x = (int) cy;
                dst.y = (int) cx;
            } else {
                dst.x = (int) cx;
                dst.y = (int) cy;
            }

            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(@Nonnull World world, int meta) {
        if((meta & 1) == 0)
            return null;

        return ((meta & 1) == 0) ? null : new TileEntityScreen();
    }

    /************************************************* DESTRUCTION HANDLING *************************************************/

    private void onDestroy(World world, BlockPos pos, EntityPlayer ply) {
        if(!world.isRemote) {
            Vector3i bp = new Vector3i(pos);
            Multiblock.BlockOverride override = new Multiblock.BlockOverride(bp, Multiblock.OverrideAction.SIMULATE);

            for(BlockSide bs: BlockSide.values())
                destroySide(world, bp.clone(), bs, override, ply);
        }
    }

    private void destroySide(World world, Vector3i pos, BlockSide side, Multiblock.BlockOverride override, EntityPlayer source) {
        Multiblock.findOrigin(world, pos, side, override);
        BlockPos bp = pos.toBlock();
        TileEntity te = world.getTileEntity(bp);

        if(te != null && te instanceof TileEntityScreen) {
            ((TileEntityScreen) te).onDestroy(source);
            world.setBlockState(bp, world.getBlockState(bp).withProperty(hasTE, false)); //Destroy tile entity.
        }
    }

    @Override
    public boolean removedByPlayer(@Nonnull IBlockState state, World world, @Nonnull BlockPos pos, @Nonnull EntityPlayer ply, boolean willHarvest) {
        onDestroy(world, pos, ply);
        return super.removedByPlayer(state, world, pos, ply, willHarvest);
    }

    @Override
    public void onBlockDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        onDestroy(world, pos, null);
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
                    destroySide(world, neighbor.clone(), bs, override, (whoDidThisShit instanceof EntityPlayer) ? ((EntityPlayer) whoDidThisShit) : null);
            }
        }
    }

    @Override
    @Nonnull
    public EnumPushReaction getMobilityFlag(IBlockState state) {
        return EnumPushReaction.IGNORE;
    }

    @Override
    public int getWeakPower(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
        return state.getValue(emitting) ? 15 : 0;
    }

    @Override
    public boolean canProvidePower(IBlockState state) {
        return state.getValue(emitting);
    }

    @Override
    protected ItemBlock createItemBlock() {
        return new ItemBlockScreen(this);
    }

    private static class ItemBlockScreen extends ItemBlock implements WDItem {

        public ItemBlockScreen(BlockScreen screen) {
            super(screen);
        }

        @Nullable
        @Override
        public String getWikiName(@Nonnull ItemStack is) {
            return "Screen";
        }

        @Override
        public void addInformation(@Nullable ItemStack is, @Nullable World world, @Nullable List<String> tt, @Nullable ITooltipFlag ttFlags) {
            super.addInformation(is, world, tt, ttFlags);
            WDItem.addInformation(tt);
        }

    }

}
