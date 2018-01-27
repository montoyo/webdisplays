/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.minecraft.util.ResourceLocation;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import org.lwjgl.opengl.GL11;

public class Icon extends BasicControl {

    protected int width;
    protected int height;
    protected double u1;
    protected double v1;
    protected double u2;
    protected double v2;
    protected ResourceLocation texture;

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);

        width = json.getInt("width", 16);
        height = json.getInt("height", 16);
        u1 = json.getDouble("u1", 0.0);
        v1 = json.getDouble("v1", 0.0);
        u2 = json.getDouble("u2", 1.0);
        v2 = json.getDouble("v2", 1.0);
        texture = new ResourceLocation(json.getString("resourceLocation", ""));
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        bindTexture(texture);
        blend(true);
        fillTexturedRect(x, y, width, height, u1, v1, u2, v2);
        blend(false);
        bindTexture(null);
    }

}
