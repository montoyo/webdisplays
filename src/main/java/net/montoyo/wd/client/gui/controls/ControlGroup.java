/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import net.montoyo.wd.utilities.Bounds;

import static org.lwjgl.opengl.GL11.*;

public class ControlGroup extends Container {

    private int width;
    private int height;
    private String label;
    private int labelW;
    private int labelColor = COLOR_WHITE;
    private boolean labelShadowed = true;

    public ControlGroup() {
        width = 100;
        height = 100;
        label = "";
        paddingX = 8;
        paddingY = 8;
    }

    public ControlGroup(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        width = w;
        height = h;
        paddingX = 8;
        paddingY = 8;
        label = "";
        labelW = 0;
    }

    public ControlGroup(int x, int y, int w, int h, String label) {
        this.x = x;
        this.y = y;
        width = w;
        height = h;
        this.label = label;
        this.labelW = font.getStringWidth(label);
        paddingX = 8;
        paddingY = 8;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    public void setLabel(String label) {
        this.label = label;
        labelW = font.getStringWidth(label);
    }

    public String getLabel() {
        return label;
    }

    public int getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    public boolean isLabelShadowed() {
        return labelShadowed;
    }

    public void setLabelShadowed(boolean labelShadowed) {
        this.labelShadowed = labelShadowed;
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        super.draw(mouseX, mouseY, ptt);

        if(visible) {
            glColor4f(0.5f, 0.5f, 0.5f, 1.f);
            glDisable(GL_TEXTURE_2D);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            double x1 = (double) x;
            double y1 = (double) y;
            double x2 = (double) (x + width);
            double y2 = (double) (y + height);
            double bp = 4.0;
            double lw = (double) labelW;

            x1 += bp;
            y1 += bp;
            x2 -= bp;
            y2 -= bp;
            lw += 12.0;

            vBuffer.begin(GL_QUADS, DefaultVertexFormats.POSITION);

            //Top edge (y = y1)
            if(labelW == 0) {
                vBuffer.pos(x1, y1 + 1.0, 0.0).endVertex();
                vBuffer.pos(x2, y1 + 1.0, 0.0).endVertex();
                vBuffer.pos(x2, y1, 0.0).endVertex();
                vBuffer.pos(x1, y1, 0.0).endVertex();
            } else {
                //Left
                vBuffer.pos(x1, y1 + 1.0, 0.0).endVertex();
                vBuffer.pos(x1 + 8.0, y1 + 1.0, 0.0).endVertex();
                vBuffer.pos(x1 + 8.0, y1, 0.0).endVertex();
                vBuffer.pos(x1, y1, 0.0).endVertex();

                //Right
                vBuffer.pos(x1 + lw, y1 + 1.0, 0.0).endVertex();
                vBuffer.pos(x2, y1 + 1.0, 0.0).endVertex();
                vBuffer.pos(x2, y1, 0.0).endVertex();
                vBuffer.pos(x1 + lw, y1, 0.0).endVertex();
            }

            //Bottom edge (y = y2)
            vBuffer.pos(x1, y2, 0.0).endVertex();
            vBuffer.pos(x2, y2, 0.0).endVertex();
            vBuffer.pos(x2, y2 - 1.0, 0.0).endVertex();
            vBuffer.pos(x1, y2 - 1.0, 0.0).endVertex();

            //Left edge (x = x1)
            vBuffer.pos(x1, y2, 0.0).endVertex();
            vBuffer.pos(x1 + 1.0, y2, 0.0).endVertex();
            vBuffer.pos(x1 + 1.0, y1, 0.0).endVertex();
            vBuffer.pos(x1, y1, 0.0).endVertex();

            //Right edge (x = x2)
            vBuffer.pos(x2 - 1.0, y2, 0.0).endVertex();
            vBuffer.pos(x2, y2, 0.0).endVertex();
            vBuffer.pos(x2, y1, 0.0).endVertex();
            vBuffer.pos(x2 - 1.0, y1, 0.0).endVertex();
            tessellator.draw();

            glDisable(GL_BLEND);
            glEnable(GL_TEXTURE_2D);

            if(labelW != 0)
                font.drawString(label, x + 10 + ((int) bp), y, labelColor, labelShadowed);
        }
    }

    public void pack() {
        Bounds bounds = findBounds(childs);
        for(Control ctrl : childs)
            ctrl.setPos(ctrl.getX() - bounds.minX, ctrl.getY() - bounds.minY);

        width = bounds.getWidth() + paddingX * 2;
        height = bounds.getHeight() + paddingY * 2;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        width = json.getInt("width", 100);
        height = json.getInt("height", 100);
        label = tr(json.getString("label", ""));
        labelW = font.getStringWidth(label);
        labelColor = json.getColor("labelColor", COLOR_WHITE);
        labelShadowed = json.getBool("labelShadowed", true);

        if(json.getBool("pack", false))
            pack();
    }

}
