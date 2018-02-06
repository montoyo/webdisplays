/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.miniserv;

public abstract class KeyParameters {

    public static final int RSA_KEY_SIZE = 2048;
    public static final int KEY_SIZE = 32;
    public static final int CHALLENGE_SIZE = 32;
    public static final String MAC_ALGORITHM = "HmacSHA256";
    public static final String RSA_CIPHER = "RSA/ECB/PKCS1Padding";

}
