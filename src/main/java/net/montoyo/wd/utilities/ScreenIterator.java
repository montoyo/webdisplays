/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import net.minecraft.util.math.BlockPos;

import java.util.Iterator;

public final class ScreenIterator implements Iterator<BlockPos> {

    private final Vector3i vec1;
    private final Vector3i vec2;
    private final BlockSide side;
    private final Vector2i size;
    private final BlockPos.MutableBlockPos blockPos;
    private int x = 0;
    private int y = 0;
    private boolean hasNext = true;

    public ScreenIterator(BlockPos pos, BlockSide side, Vector2i size) {
        vec1 = new Vector3i(pos);
        vec2 = vec1.clone();
        this.side = side;
        this.size = size;
        blockPos = new BlockPos.MutableBlockPos();
    }

    @Override
    public final boolean hasNext() {
        return hasNext;
    }

    @Override
    public final BlockPos next() {
        vec2.toBlock(blockPos);

        if(++x >= size.x) {
            if(++y >= size.y)
                hasNext = false;
            else {
                x = 0;
                vec2.set(vec1.add(side.up));
            }
        } else
            vec2.add(side.right);

        return blockPos;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getIndex() {
        return y * size.x + x;
    }

}
