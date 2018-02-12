/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import net.montoyo.wd.client.gui.WDScreen;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import net.montoyo.wd.utilities.Bounds;

import java.io.IOException;

import static org.lwjgl.opengl.GL11.*;

public abstract class Control {

    public static final int COLOR_BLACK    = 0xFF000000;
    public static final int COLOR_WHITE    = 0xFFFFFFFF;
    public static final int COLOR_RED      = 0xFFFF0000;
    public static final int COLOR_GREEN    = 0xFF00FF00;
    public static final int COLOR_BLUE     = 0xFF0000FF;
    public static final int COLOR_CYAN     = 0xFF00FFFF;
    public static final int COLOR_MANGENTA = 0xFFFF00FF;
    public static final int COLOR_YELLOW   = 0xFFFFFF00;

    protected final Minecraft mc;
    protected final FontRenderer font;
    protected final Tessellator tessellator;
    protected final BufferBuilder vBuffer;
    protected final WDScreen parent;
    protected String name;
    protected Object userdata;

    public Control() {
        mc = Minecraft.getMinecraft();
        font = mc.fontRenderer;
        tessellator = Tessellator.getInstance();
        vBuffer = tessellator.getBuffer();
        parent = WDScreen.CURRENT_SCREEN;
    }

    public Object getUserdata() {
        return userdata;
    }

    public void setUserdata(Object userdata) {
        this.userdata = userdata;
    }

    public void keyTyped(char typedChar, int keyCode) throws IOException {
    }

    public void keyUp(int key) {
    }

    public void keyDown(int key) {
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
    }

    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
    }

    public void mouseMove(int mouseX, int mouseY) {
    }

    public void mouseScroll(int mouseX, int mouseY, int amount) {
    }

    public void draw(int mouseX, int mouseY, float ptt) {
    }

    public void postDraw(int mouseX, int mouseY, float ptt) {
    }

    public void destroy() {
    }

    public WDScreen getParent() {
        return parent;
    }

    public abstract int getX();
    public abstract int getY();
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract void setPos(int x, int y);

    public void fillRect(int x, int y, int w, int h, int color) {
        double x1 = (double) x;
        double y1 = (double) y;
        double x2 = (double) (x + w);
        double y2 = (double) (y + h);
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8 ) & 0xFF;
        int b =  color & 0xFF;

        glColor4f(((float) r) / 255.f, ((float) g) / 255.f, ((float) b) / 255.f, ((float) a) / 255.f);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        vBuffer.begin(GL_QUADS, DefaultVertexFormats.POSITION);
        vBuffer.pos(x1, y2, 0.0).endVertex();
        vBuffer.pos(x2, y2, 0.0).endVertex();
        vBuffer.pos(x2, y1, 0.0).endVertex();
        vBuffer.pos(x1, y1, 0.0).endVertex();
        tessellator.draw();

        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
    }

    public void fillTexturedRect(int x, int y, int w, int h, double u1, double v1, double u2, double v2) {
        double x1 = (double) x;
        double y1 = (double) y;
        double x2 = (double) (x + w);
        double y2 = (double) (y + h);

        vBuffer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        vBuffer.pos(x1, y2, 0.0).tex(u1, v2).color(255, 255, 255, 255).endVertex();
        vBuffer.pos(x2, y2, 0.0).tex(u2, v2).color(255, 255, 255, 255).endVertex();
        vBuffer.pos(x2, y1, 0.0).tex(u2, v1).color(255, 255, 255, 255).endVertex();
        vBuffer.pos(x1, y1, 0.0).tex(u1, v1).color(255, 255, 255, 255).endVertex();
        tessellator.draw();
    }

    public static void blend(boolean enable) {
        if(enable) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        } else
            glDisable(GL_BLEND);
    }

    public void bindTexture(ResourceLocation resLoc) {
        if(resLoc == null)
            GlStateManager.bindTexture(0); //Damn state manager
        else
            mc.renderEngine.bindTexture(resLoc);
    }

    public void drawBorder(int x, int y, int w, int h, int color) {
        drawBorder(x, y, w, h, color, 1.0);
    }

    public void drawBorder(int x, int y, int w, int h, int color, double sz) {
        double x1 = (double) x;
        double y1 = (double) y;
        double x2 = (double) (x + w);
        double y2 = (double) (y + h);
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8 ) & 0xFF;
        int b =  color & 0xFF;

        glColor4f(((float) r) / 255.f, ((float) g) / 255.f, ((float) b) / 255.f, ((float) a) / 255.f);
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        vBuffer.begin(GL_QUADS, DefaultVertexFormats.POSITION);
        //Top edge (y = y1)
        vBuffer.pos(x1, y1 + sz, 0.0).endVertex();
        vBuffer.pos(x2, y1 + sz, 0.0).endVertex();
        vBuffer.pos(x2, y1, 0.0).endVertex();
        vBuffer.pos(x1, y1, 0.0).endVertex();

        //Bottom edge (y = y2)
        vBuffer.pos(x1, y2, 0.0).endVertex();
        vBuffer.pos(x2, y2, 0.0).endVertex();
        vBuffer.pos(x2, y2 - sz, 0.0).endVertex();
        vBuffer.pos(x1, y2 - sz, 0.0).endVertex();

        //Left edge (x = x1)
        vBuffer.pos(x1, y2, 0.0).endVertex();
        vBuffer.pos(x1 + sz, y2, 0.0).endVertex();
        vBuffer.pos(x1 + sz, y1, 0.0).endVertex();
        vBuffer.pos(x1, y1, 0.0).endVertex();

        //Right edge (x = x2)
        vBuffer.pos(x2 - sz, y2, 0.0).endVertex();
        vBuffer.pos(x2, y2, 0.0).endVertex();
        vBuffer.pos(x2, y1, 0.0).endVertex();
        vBuffer.pos(x2 - sz, y1, 0.0).endVertex();
        tessellator.draw();

        glDisable(GL_BLEND);
        glEnable(GL_TEXTURE_2D);
    }

    public void beginFramebuffer(Framebuffer fbo, int vpW, int vpH) {
        fbo.bindFramebuffer(true);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0.0, (double) vpW, (double) vpH, 0.0, -1.0,1.0);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        if(!fbo.useDepth)
            glDisable(GL_DEPTH_TEST);
    }

    public void endFramebuffer(Framebuffer fbo) {
        if(!fbo.useDepth)
            glEnable(GL_DEPTH_TEST);

        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        fbo.unbindFramebuffer();
        mc.getFramebuffer().bindFramebuffer(true);
    }

    public static String tr(String text) {
        if(text.length() >= 2 && text.charAt(0) == '$') {
            if(text.charAt(1) == '$')
                return text.substring(1);
            else
                return I18n.format(text.substring(1));
        } else
            return text;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void load(JsonOWrapper json) {
        name = json.getString("name", "");
    }

    public static Bounds findBounds(java.util.List<Control> controlList) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for(Control ctrl : controlList) {
            int x = ctrl.getX();
            int y = ctrl.getY();
            if(x < minX)
                minX = x;

            if(y < minY)
                minY = y;

            x += ctrl.getWidth();
            y += ctrl.getHeight();

            if(x > maxX)
                maxX = x;

            if(y >= maxY)
                maxY = y;
        }

        return new Bounds(minX, minY, maxX, maxY);
    }

}
