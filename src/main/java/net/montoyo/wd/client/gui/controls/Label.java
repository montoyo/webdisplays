/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.montoyo.wd.client.gui.loading.JsonOWrapper;

public class Label extends BasicControl {

    private String label;
    private int labelW;
    private int color;
    private boolean shadowed;

    public Label() {
        label = "";
        color = COLOR_WHITE;
    }

    public Label(int x, int y, String str) {
        this.x = x;
        this.y = y;
        label = str;
        labelW = font.getStringWidth(str);
        color = COLOR_WHITE;
        shadowed = false;
    }

    public Label(int x, int y, String str, int color) {
        this.x = x;
        this.y = y;
        label = str;
        labelW = font.getStringWidth(str);
        this.color = color;
        shadowed = false;
    }

    public Label(int x, int y, String str, int color, boolean shadowed) {
        this.x = x;
        this.y = y;
        label = str;
        labelW = font.getStringWidth(str);
        this.color = color;
        this.shadowed = shadowed;
    }

    public void setLabel(String label) {
        this.label = label;
        labelW = font.getStringWidth(label);
    }

    public String getLabel() {
        return label;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }

    public void setShadowed(boolean shadowed) {
        this.shadowed = shadowed;
    }

    public boolean isShadowed() {
        return shadowed;
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        if(visible)
            font.drawString(label, x, y, color, shadowed);
    }

    @Override
    public int getWidth() {
        return labelW;
    }

    @Override
    public int getHeight() {
        return 12;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        label = tr(json.getString("label", ""));
        labelW = font.getStringWidth(label);
        color = json.getColor("color", COLOR_WHITE);
        shadowed = json.getBool("shadowed", false);
    }

}
