/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import io.netty.buffer.ByteBuf;

public final class Vector2i {

    public int x;
    public int y;

    public Vector2i()
    {
        x = 0;
        y = 0;
    }

    public Vector2i(int val)
    {
        x = val;
        y = val;
    }

    public Vector2i(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Vector2i(ByteBuf bb)
    {
        x = bb.readInt();
        y = bb.readInt();
    }

    public void writeTo(ByteBuf bb)
    {
        bb.writeInt(x);
        bb.writeInt(y);
    }

}
