/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import com.google.gson.annotations.SerializedName;

public class TypeData {

    public enum Action {
        @SerializedName("i")
        INVALID,

        @SerializedName("p")
        PRESS,

        @SerializedName("r")
        RELEASE,

        @SerializedName("t")
        TYPE
    }

    private Action a;
    private int k;
    private int c;

    public TypeData() {
        a = Action.INVALID;
        k = 0;
        c = 0;
    }

    public TypeData(Action action, int code, char chr) {
        a = action;
        k = code;
        c = (int) chr;
    }

    public Action getAction() {
        return a;
    }

    public char getKeyChar() {
        return (char) c;
    }

    public int getKeyCode() {
        return k;
    }

}
