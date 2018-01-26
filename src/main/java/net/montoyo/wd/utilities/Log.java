/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;

public abstract class Log {

    public static void info(String what, Object ... data) {
        FMLLog.log("WebDisplays", Level.INFO, what, data);
    }

    public static void warning(String what, Object ... data) {
        FMLLog.log("WebDisplays", Level.WARN, what, data);
    }

    public static void error(String what, Object ... data) {
        FMLLog.log("WebDisplays", Level.ERROR, what, data);
    }

    public static void infoEx(String what, Throwable e, Object ... data) {
        FMLLog.log("WebDisplays", Level.INFO, e, what, data);
    }

    public static void warningEx(String what, Throwable e, Object ... data) {
        FMLLog.log("WebDisplays", Level.WARN, e, what, data);
    }

    public static void errorEx(String what, Throwable e, Object ... data) {
        FMLLog.log("WebDisplays", Level.ERROR, e, what, data);
    }

}
