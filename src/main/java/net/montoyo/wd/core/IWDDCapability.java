/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

public interface IWDDCapability {

    boolean isFirstRun();
    void clearFirstRun();
    void cloneTo(IWDDCapability dst);

}
