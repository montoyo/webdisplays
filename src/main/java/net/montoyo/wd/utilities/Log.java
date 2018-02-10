/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

public abstract class Log {

    public static void info(String what, Object ... data) {
        LogManager.getLogger("WebDisplays").log(Level.INFO, String.format(what, data));
    }

    public static void warning(String what, Object ... data) {
        LogManager.getLogger("WebDisplays").log(Level.WARN, String.format(what, data));
    }

    public static void error(String what, Object ... data) {
        LogManager.getLogger("WebDisplays").log(Level.ERROR, String.format(what, data));
    }

    public static void infoEx(String what, Throwable e, Object ... data) {
        LogManager.getLogger("WebDisplays").log(Level.INFO, String.format(what, data), e);
    }

    public static void warningEx(String what, Throwable e, Object ... data) {
        LogManager.getLogger("WebDisplays").log(Level.WARN, String.format(what, data), e);
    }

    public static void errorEx(String what, Throwable e, Object ... data) {
        LogManager.getLogger("WebDisplays").log(Level.ERROR, String.format(what, data), e);
    }

}
