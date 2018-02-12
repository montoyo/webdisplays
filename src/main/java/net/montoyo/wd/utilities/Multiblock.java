/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.montoyo.wd.WebDisplays;

public abstract class Multiblock {

    public enum OverrideAction {
        NONE,
        SIMULATE,
        IGNORE
    }

    public static class BlockOverride {
        final Vector3i pos;
        final OverrideAction action;

        public BlockOverride(Vector3i p, OverrideAction act) {
            pos = p;
            action = act;
        }

        public boolean apply(Vector3i bp, boolean originalResult) {
            if(action == OverrideAction.NONE || !bp.equals(pos))
                return originalResult;
            else if(action == OverrideAction.SIMULATE)
                return true;
            else //action == OverrideAction.IGNORE
                return false;
        }

    }

    public static final BlockOverride NULL_OVERRIDE = new BlockOverride(null, OverrideAction.NONE);

    //Modifies pos
    public static void findOrigin(IBlockAccess world, Vector3i pos, BlockSide side, BlockOverride override)
    {
        if(override == null)
            override = NULL_OVERRIDE;

        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();

        //Go left
        do {
            pos.add(side.left);
            pos.toBlock(bp);
        } while(override.apply(pos, world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen));

        pos.add(side.right);

        //Go down
        do {
            pos.add(side.down);
            pos.toBlock(bp);
        } while(override.apply(pos, world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen));

        pos.add(side.up);
    }

    //Origin stays constant
    public static Vector2i measure(IBlockAccess world, Vector3i origin, BlockSide side)
    {
        Vector2i ret = new Vector2i();
        Vector3i pos = origin.clone();

        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        pos.toBlock(bp);

        //Go up
        do {
            pos.add(side.up);
            pos.toBlock(bp);
            ret.y++;
        } while(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen);

        pos.add(side.down);

        //Go right
        do {
            pos.add(side.right);
            pos.toBlock(bp);
            ret.x++;
        } while(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen);

        return ret;
    }

    //Origin and size stays constant.
    //Returns null if structure is okay, otherwise the erroring block pos.
    public static Vector3i check(IBlockAccess world, Vector3i origin, Vector2i size, BlockSide side)
    {
        Vector3i pos = origin.clone();
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();

        //Check inner
        for(int y = 0; y < size.y; y++) {
            for(int x = 0; x < size.x; x++) {
                pos.toBlock(bp);
                if(!(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen))
                    return pos; //Hole

                pos.add(side.forward);
                pos.toBlock(bp);
                if(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen)
                    return pos; //Back should be empty

                pos.addMul(side.backward, 2);
                pos.toBlock(bp);
                if(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen)
                    return pos; //Front should be empty

                pos.add(side.forward);
                pos.add(side.right);
            }

            pos.addMul(side.left, size.x);
            pos.add(side.up);
        }

        //Check left edge
        pos.set(origin);
        pos.add(side.left);

        for(int y = 0; y < size.y; y++) {
            pos.toBlock(bp);
            if(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen)
                return pos; //Left edge should be empty

            pos.add(side.up);
        }

        //Check right edge
        pos.set(origin);
        pos.addMul(side.right, size.x);

        for(int y = 0; y < size.y; y++) {
            pos.toBlock(bp);
            if(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen)
                return pos; //Left edge should be empty

            pos.add(side.up);
        }

        //Check bottom edge
        pos.set(origin);
        pos.add(side.down);

        for(int x = 0; x < size.x; x++) {
            pos.toBlock(bp);
            if(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen)
                return pos; //Left edge should be empty

            pos.add(side.right);
        }

        //Check top edge
        pos.set(origin);
        pos.addMul(side.up, size.y);

        for(int x = 0; x < size.x; x++) {
            pos.toBlock(bp);
            if(world.getBlockState(bp).getBlock() == WebDisplays.INSTANCE.blockScreen)
                return pos; //Left edge should be empty

            pos.add(side.right);
        }

        //All good.
        return null;
    }

}
