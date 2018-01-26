/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.montoyo.wd.client.gui.loading.JsonOWrapper;

public abstract class BasicControl extends Control {

    protected int x;
    protected int y;
    protected boolean visible = true;
    protected boolean disabled = false;

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public void enable() {
        disabled = false;
    }

    public void disable() {
        disabled = true;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void show() {
        visible = true;
    }

    public void hide() {
        visible = false;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        x = json.getInt("x", 0);
        y = json.getInt("y", 0);
        disabled = json.getBool("disabled", false);
        visible = json.getBool("visible", true);
    }

}
