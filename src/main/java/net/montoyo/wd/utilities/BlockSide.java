/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

public enum BlockSide {

    BOTTOM(new Vector3i( 0,  0, -1), new Vector3i( 1,  0,  0), new Vector3i( 0, -1,  0)),
    TOP   (new Vector3i( 0,  0, -1), new Vector3i( 1,  0,  0), new Vector3i( 0,  1,  0)),
    NORTH (new Vector3i( 0,  1,  0), new Vector3i(-1,  0,  0), new Vector3i( 0,  0, -1)),
    SOUTH (new Vector3i( 0,  1,  0), new Vector3i( 1,  0,  0), new Vector3i( 0,  0,  1)),
    WEST  (new Vector3i( 0,  1,  0), new Vector3i( 0,  0,  1), new Vector3i(-1,  0,  0)),
    EAST  (new Vector3i( 0,  1,  0), new Vector3i( 0,  0, -1), new Vector3i( 1,  0,  0));

    public final Vector3i up;
    public final Vector3i right;
    public final Vector3i forward;
    public final Vector3i down;
    public final Vector3i left;
    public final Vector3i backward;

    BlockSide(Vector3i u, Vector3i r, Vector3i f) {
        up = u;
        right = r;
        forward = f;
        down = u.clone().neg();
        left = r.clone().neg();
        backward = f.clone().neg();
    }

    public BlockSide reverse()
    {
        int side = ordinal();
        int div = side / 2;
        int rest = 1 - side % 2;

        return values()[div * 2 + rest];
    }

    public static int reverse(int side) {
        int div = side / 2;
        int rest = 1 - side % 2;

        return div * 2 + rest;
    }

    public static BlockSide fromInt(int s) {
        BlockSide[] values = values();
        return (s < 0 || s >= values.length) ? null : values[s];
    }

}
