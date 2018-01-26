/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.loading;

import com.google.gson.JsonArray;

import java.util.Map;

public class JsonAWrapper {

    private final JsonArray array;
    private final Map<String, Double> variables;

    public JsonAWrapper(JsonArray a, Map<String, Double> vars) {
        array = a;
        variables = vars;
    }

    public int size() {
        return array.size();
    }

    public String getString(int i) {
        return array.get(i).getAsString();
    }

    public int getInt(int i) {
        return array.get(i).getAsInt();
    }

    public long getLong(int i) {
        return array.get(i).getAsLong();
    }

    public float getFloat(int i) {
        return array.get(i).getAsFloat();
    }

    public double getDouble(int i) {
        return array.get(i).getAsDouble();
    }

    public boolean getBool(int i) {
        return array.get(i).getAsBoolean();
    }

    public JsonOWrapper getObject(int i) {
        return new JsonOWrapper(array.get(i).getAsJsonObject(), variables);
    }

    public JsonAWrapper getArray(int i) {
        return new JsonAWrapper(array.get(i).getAsJsonArray(), variables);
    }

    public JsonArray getArray() {
        return array;
    }

}
