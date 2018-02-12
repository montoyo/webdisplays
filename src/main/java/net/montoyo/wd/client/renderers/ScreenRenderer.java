/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.utilities.Vector3f;
import net.montoyo.wd.utilities.Vector3i;

import static org.lwjgl.opengl.GL11.*;

public class ScreenRenderer extends TileEntitySpecialRenderer<TileEntityScreen> {

    private final Vector3f mid = new Vector3f();
    private final Vector3i tmpi = new Vector3i();
    private final Vector3f tmpf = new Vector3f();

    @Override
    public void render(TileEntityScreen te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if(!te.isLoaded())
            return;

        //Disable lighting
        RenderHelper.disableStandardItemLighting();
        setLightmapDisabled(true);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_CULL_FACE);
        glDisable(GL_BLEND);

        for(int i = 0; i < te.screenCount(); i++) {
            TileEntityScreen.Screen scr = te.getScreen(i);
            if(scr.browser == null) {
                scr.browser = ((ClientProxy) WebDisplays.PROXY).getMCEF().createBrowser(WebDisplays.applyBlacklist(scr.url));

                if(scr.rotation.isVertical)
                    scr.browser.resize(scr.resolution.y, scr.resolution.x);
                else
                    scr.browser.resize(scr.resolution.x, scr.resolution.y);

                scr.doTurnOnAnim = true;
                scr.turnOnTime = System.currentTimeMillis();
            }

            tmpi.set(scr.side.right);
            tmpi.mul(scr.size.x);
            tmpi.addMul(scr.side.up, scr.size.y);
            tmpf.set(tmpi);

            mid.set(x + 0.5, y + 0.5, z + 0.5);
            mid.addMul(tmpf, 0.5f);
            tmpf.set(scr.side.left);
            mid.addMul(tmpf, 0.5f);
            tmpf.set(scr.side.down);
            mid.addMul(tmpf, 0.5f);

            glPushMatrix();
            glTranslatef(mid.x, mid.y, mid.z);

            switch(scr.side) {
                case BOTTOM:
                    glRotatef(90.f, 1.f, 0.f, 0.f);
                    break;

                case TOP:
                    glRotatef(-90.f, 1.f, 0.f, 0.f);
                    break;

                case NORTH:
                    glRotatef(180.f, 0.f, 1.f, 0.f);
                    break;

                case SOUTH:
                    break;

                case WEST:
                    glRotatef(-90.f, 0.f, 1.f, 0.f);
                    break;

                case EAST:
                    glRotatef(90.f, 0.f, 1.f, 0.f);
                    break;
            }

            if(scr.doTurnOnAnim) {
                long lt = System.currentTimeMillis() - scr.turnOnTime;
                float ft = ((float) lt) / 100.0f;

                if(ft >= 1.0f) {
                    ft = 1.0f;
                    scr.doTurnOnAnim = false;
                }

                glScalef(ft, ft, 1.0f);
            }

            if(!scr.rotation.isNull)
                glRotatef(scr.rotation.angle, 0.0f, 0.0f, 1.0f);

            float sw = ((float) scr.size.x) * 0.5f - 2.f / 16.f;
            float sh = ((float) scr.size.y) * 0.5f - 2.f / 16.f;

            if(scr.rotation.isVertical) {
                float tmp = sw;
                sw = sh;
                sh = tmp;
            }

            //TODO: Use tesselator
            glBindTexture(GL_TEXTURE_2D, scr.browser.getTextureID());
            glBegin(GL_QUADS);
            glColor4f(1.f, 1.f, 1.f, 1.f); glTexCoord2f(0.f, 1.f); glVertex3f(-sw, -sh, 0.505f);
            glColor4f(1.f, 1.f, 1.f, 1.f); glTexCoord2f(1.f, 1.f); glVertex3f( sw, -sh, 0.505f);
            glColor4f(1.f, 1.f, 1.f, 1.f); glTexCoord2f(1.f, 0.f); glVertex3f( sw,  sh, 0.505f);
            glColor4f(1.f, 1.f, 1.f, 1.f); glTexCoord2f(0.f, 0.f); glVertex3f(-sw,  sh, 0.505f);
            glEnd();
            GlStateManager.bindTexture(0); //Minecraft does shit with mah texture otherwise...
            glPopMatrix();
        }

        /*
        //Bounding box debugging
        glPushMatrix();
        glTranslated(-rendererDispatcher.entityX, -rendererDispatcher.entityY, -rendererDispatcher.entityZ);
        renderAABB(te.getRenderBoundingBox());
        glPopMatrix();
        */

        //Re-enable lighting
        RenderHelper.enableStandardItemLighting();
        setLightmapDisabled(false);
        glEnable(GL_CULL_FACE);
    }

    public void renderAABB(AxisAlignedBB bb) {
        glDisable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_CULL_FACE);
        glColor4f(0.f, 0.5f, 1.f, 0.75f);
        glDepthMask(false);

        Tessellator t = Tessellator.getInstance();
        BufferBuilder vb = t.getBuffer();
        vb.begin(GL_QUADS, DefaultVertexFormats.POSITION);

        //Bottom
        vb.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        vb.pos(bb.minX, bb.minY, bb.maxZ).endVertex();

        //Top
        vb.pos(bb.minX, bb.maxY, bb.minZ).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();

        //Left
        vb.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        vb.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        //Right
        vb.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();

        //Front
        vb.pos(bb.minX, bb.minY, bb.minZ).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.minZ).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.minZ).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.minZ).endVertex();

        //Back
        vb.pos(bb.minX, bb.minY, bb.maxZ).endVertex();
        vb.pos(bb.maxX, bb.minY, bb.maxZ).endVertex();
        vb.pos(bb.maxX, bb.maxY, bb.maxZ).endVertex();
        vb.pos(bb.minX, bb.maxY, bb.maxZ).endVertex();
        t.draw();

        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_BLEND);
    }

    @Override
    public boolean isGlobalRenderer(TileEntityScreen te) {
        //I don't like making it a global renderer for performance reasons,
        //but Minecraft's AABB-in-view-frustum checking is crappy as hell.
        return te.isLoaded();
    }

}
