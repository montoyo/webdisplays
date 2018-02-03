/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

public enum Rotation {

    ROT_0(0.0f, false),
    ROT_90(90.0f, true),
    ROT_180(180.0f, false),
    ROT_270(270.0f, true);

    public final float angle;
    public final boolean isVertical;
    public final boolean isNull;

    Rotation(float a, boolean v) {
        angle = a;
        isVertical = v;
        isNull = (a == 0.0f);
    }

    public int getAngleAsInt() {
        return (int) angle;
    }

}
