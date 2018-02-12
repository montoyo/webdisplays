/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

public abstract class Constants {

    public static final int FUPA_STATUS_BAD_NAME = 1;
    public static final int FUPA_STATUS_INVALID_SIZE = 2;
    public static final int FUPA_STATUS_EXCEEDS_QUOTA = 3;
    public static final int FUPA_STATUS_OCCUPIED = 4;
    public static final int FUPA_STATUS_FILE_EXISTS = 5;
    public static final int FUPA_STATUS_INTERNAL_ERROR = 6;
    public static final int FUPA_STATUS_USER_ABORT = 7;
    public static final int FUPA_STATUS_LIER = 8;
    public static final int FUPA_STATUS_CONNECTION_LOST = 9;

    public static final int GETF_STATUS_BAD_NAME = 1;
    public static final int GETF_STATUS_NOT_FOUND = 2;
    public static final int GETF_STATUS_INTERNAL_ERROR = 3;
    public static final int GETF_STATUS_CONNECTION_LOST = 4;
    public static final int GETF_STATUS_TIMED_OUT = 5;

    public static final long CLIENT_TIMEOUT = 30000L;
    public static final long CLIENT_PING_PERIOD = 5000L;

}
