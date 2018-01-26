/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.loading;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.montoyo.wd.client.gui.controls.Control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonOWrapper {

    private static final HashMap<String, Integer> defaultColors = new HashMap<>();
    static {
        defaultColors.put("black", Control.COLOR_BLACK);
        defaultColors.put("white", Control.COLOR_WHITE);
        defaultColors.put("red", Control.COLOR_RED);
        defaultColors.put("green", Control.COLOR_GREEN);
        defaultColors.put("blue", Control.COLOR_BLUE);
        defaultColors.put("magenta", Control.COLOR_MANGENTA);
        defaultColors.put("cyan", Control.COLOR_CYAN);
        defaultColors.put("yellow", Control.COLOR_YELLOW);
    }

    private final JsonObject object;
    private final Map<String, Double> variables;

    public JsonOWrapper(JsonObject obj, Map<String, Double> vars) {
        object = obj;
        variables = vars;
    }

    public String getString(String key, String def) {
        return object.has(key) ? object.get(key).getAsString() : def;
    }

    public long getLong(String key, long def) {
        return object.has(key) ? object.get(key).getAsLong() : def;
    }

    public int getInt(String key, int def) {
        if(!object.has(key))
            return def;

        JsonPrimitive prim = object.get(key).getAsJsonPrimitive();
        if(prim.isNumber())
            return prim.getAsInt();

        return (int) evalExpr(prim.getAsString(), variables);
    }

    public float getFloat(String key, float def) {
        if(!object.has(key))
            return def;

        JsonPrimitive prim = object.get(key).getAsJsonPrimitive();
        if(prim.isNumber())
            return prim.getAsFloat();

        return (float) evalExpr(prim.getAsString(), variables);
    }

    public double getDouble(String key, double def) {
        if(!object.has(key))
            return def;

        JsonPrimitive prim = object.get(key).getAsJsonPrimitive();
        if(prim.isNumber())
            return prim.getAsDouble();

        return evalExpr(prim.getAsString(), variables);
    }

    public boolean getBool(String key, boolean def) {
        if(!object.has(key))
            return def;

        JsonPrimitive prim = object.get(key).getAsJsonPrimitive();
        if(prim.isBoolean())
            return prim.getAsBoolean();
        else if(prim.isNumber())
            return prim.getAsInt() != 0;

        return evalExpr(prim.getAsString(), variables) != 0.0;
    }

    public JsonOWrapper getObject(String key) {
        return new JsonOWrapper(object.has(key) ? object.get(key).getAsJsonObject() : (new JsonObject()), variables);
    }

    public JsonAWrapper getArray(String key) {
        return new JsonAWrapper(object.has(key) ? object.get(key).getAsJsonArray() : (new JsonArray()), variables);
    }

    public JsonObject getObject() {
        return object;
    }

    public int getColor(String key, int def) {
        if(!object.has(key))
            return def;

        JsonElement c = object.get(key);
        if(c.isJsonPrimitive()) {
            JsonPrimitive prim = c.getAsJsonPrimitive();

            if(prim.isNumber())
                return (int) prim.getAsLong();
            else if(prim.isString()) {
                String str = prim.getAsString();
                Integer dc = defaultColors.get(str.toLowerCase());
                if(dc != null)
                    return dc;

                if(!str.isEmpty() && str.charAt(0) == '#')
                    str = str.substring(1);

                long ret = Long.parseLong(str, 16);
                if(str.length() <= 6)
                    ret |= 0xFF000000L;

                return (int) ret;
            } else
                return def;
        }

        int r, g, b, a;
        if(c.isJsonArray()) {
            JsonArray array = c.getAsJsonArray();

            r = array.get(0).getAsInt();
            g = array.get(1).getAsInt();
            b = array.get(2).getAsInt();
            a = (array.size() >= 4) ? array.get(3).getAsInt() : 255;
        } else if(c.isJsonObject()) {
            JsonObject obj = c.getAsJsonObject();

            r = obj.get("r").getAsInt();
            g = obj.get("g").getAsInt();
            b = obj.get("b").getAsInt();
            a = obj.has("a") ? obj.get("a").getAsInt() : 255;
        } else
            return def;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static final String OPS = "+*/%&|"; //The - sign is an exception, don't add it here
    private static final String[] OPS_PRIORITY = new String[] { "*/%", "+-", "&|" };

    private static class VarOpPair {
        double var;
        char op;

        void setVar(String str, boolean isNumber, String expr, Map<String, Double> variables) {
            if(isNumber)
                var = Double.parseDouble(str);
            else {
                boolean neg = (str.charAt(0) == '-');
                String varName = neg ? str.substring(1) : str;
                Double d = variables.get(varName);

                if(d == null)
                    throw new RuntimeException("Unknown variable \"" + varName + "\" in expression \"" + expr + "\"");

                var = neg ? -d : d;
            }
        }

        void setOp(char op) {
            this.op = op;
        }
    }

    private static int findPair(List<VarOpPair> list, String ops) {
        for(int i = 0; i < list.size(); i++) {
            if(ops.indexOf(list.get(i).op) >= 0)
                return i;
        }

        return -1;
    }

    private static double evalExpr(String expr, Map<String, Double> variables) {
        //Apply parenthesis
        while(true) {
            int pos = expr.indexOf('(');
            if(pos < 0)
                break;

            int end = ++pos;
            int lvl = 0;

            for(; end < expr.length(); end++) {
                char chr = expr.charAt(end);

                if(chr == '(')
                    lvl++;
                else if(chr == ')') {
                    if(lvl == 0)
                        break;

                    lvl--;
                }
            }

            if(end >= expr.length())
                throw new RuntimeException("Unclosed parenthesis in expression \"" + expr + "\"");

            double val = evalExpr(expr.substring(pos, end), variables);
            expr = expr.substring(0, pos - 1) + val + expr.substring(end + 1);
        }

        //Parse into ops
        ArrayList<VarOpPair> ops = new ArrayList<>();
        StringBuilder str = new StringBuilder();
        boolean negIsPartOfStr = true;
        boolean strIsNumber = true;

        for(int i = 0; i < expr.length(); i++) {
            char chr = expr.charAt(i);
            if(Character.isSpaceChar(chr))
                continue;

            if((chr == '-' && !negIsPartOfStr) || OPS.indexOf(chr) >= 0) {
                //Parse
                VarOpPair pair = new VarOpPair();
                pair.setVar(str.toString(), strIsNumber, expr, variables);
                pair.setOp(chr);
                ops.add(pair);

                //Reset
                str.setLength(0);
                negIsPartOfStr = true;
                strIsNumber = true;
            } else {
                if(strIsNumber && chr != '-' && chr != '.' && !Character.isDigit(chr))
                    strIsNumber = false;

                if(negIsPartOfStr)
                    negIsPartOfStr = false;

                str.append(chr);
            }
        }

        if(str.length() > 0) {
            VarOpPair pair = new VarOpPair();
            pair.setVar(str.toString(), strIsNumber, expr, variables);
            pair.setOp((char) 0);
            ops.add(pair);
        }

        //Compute
        while(true) {
            int pairId = -1;
            for(String opList : OPS_PRIORITY) {
                pairId = findPair(ops, opList);

                if(pairId >= 0)
                    break;
            }

            if(pairId < 0)
                break;

            VarOpPair a = ops.get(pairId);
            VarOpPair b = ops.get(pairId + 1);

            if(a.op == '*')
                b.var = a.var * b.var;
            else if(a.op == '/')
                b.var = a.var / b.var;
            else if(a.op == '%')
                b.var = a.var % b.var;
            else if(a.op == '+')
                b.var = a.var + b.var;
            else if(a.op == '-')
                b.var = a.var - b.var;
            else if(a.op == '&') {
                if(a.var == 0.0)
                    b.var = 0.0;

                //if b.var == 0, b.var stays 0
                //if a.var != 0, b.var keeps its value
            } else if(a.op == '|') {
                if(a.var != 0.0)
                    b.var = a.var;

                //if a.var == 0, b.var keeps its value
                //if a.var != 0, b.var takes the value of a
            }

            ops.remove(pairId);
        }

        //Check
        if(ops.size() != 1 || ops.get(0).op != (char) 0)
            throw new RuntimeException("Error while parsing evaluating \"" + expr + "\"");

        return ops.get(0).var;
    }

}
