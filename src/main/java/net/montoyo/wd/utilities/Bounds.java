/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

public final class Bounds {

    public final int minX;
    public final int minY;
    public final int maxX;
    public final int maxY;

    public Bounds(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public final int getWidth() {
        return maxX - minX;
    }

    public final int getHeight() {
        return maxY - minY;
    }

}
