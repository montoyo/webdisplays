/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;

@SideOnly(Side.CLIENT)
public final class LaserPointerRenderer implements IItemRenderer {

    private static final float PI = (float) Math.PI;
    private final Tessellator t = Tessellator.getInstance();
    private final BufferBuilder bb = t.getBuffer();
    private final FloatBuffer matrix1 = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer renderBuffer = BufferUtils.createFloatBuffer(8);

    public boolean isOn = false;

    public LaserPointerRenderer() {
        for(int i = 0; i < 8; i++)
            renderBuffer.put(0.0f);

        renderBuffer.position(0);
    }

    @Override
    public final void render(ItemStack is, float handSideSign, float swingProgress, float equipProgress) {
        //This whole method is a fucking hack
        float sqrtSwingProg = (float) Math.sqrt((double) swingProgress);
        float sinSqrtSwingProg1 = MathHelper.sin(sqrtSwingProg * PI);

        GlStateManager.disableCull();
        GlStateManager.disableTexture2D();
        GlStateManager.enableRescaleNormal();

        //Laser pointer
        glPushMatrix();
        glTranslatef(handSideSign * -0.4f * sinSqrtSwingProg1, 0.2f * MathHelper.sin(sqrtSwingProg * PI * 2.0f), -0.2f * MathHelper.sin(swingProgress * PI));
        glTranslatef(handSideSign * 0.56f, -0.52f - equipProgress * 0.6f, -0.72f);
        glRotatef(handSideSign * (45.0f - MathHelper.sin(swingProgress * swingProgress * PI) * 20.0f), 0.0f, 1.0f, 0.0f);
        glRotatef(handSideSign * sinSqrtSwingProg1 * -20.0f, 0.0f, 0.0f, 1.0f);
        glRotatef(sinSqrtSwingProg1 * -80.0f, 1.0f, 0.0f, 0.0f);
        glRotatef(handSideSign * -30.0f, 0.0f, 1.0f, 0.0f);
        glTranslatef(0.0f, 0.2f, 0.0f);
        glRotatef(10.0f, 1.0f, 0.0f, 0.0f);
        glScalef(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);

        glColor4f(0.5f, 0.5f, 0.5f, 1.0f);
        bb.begin(GL_QUADS, DefaultVertexFormats.POSITION_NORMAL);
        bb.pos(0.0, 0.0, 0.0).normal(0.0f, 1.0f, 0.0f).endVertex();
        bb.pos(1.0, 0.0, 0.0).normal(0.0f, 1.0f, 0.0f).endVertex();
        bb.pos(1.0, 0.0, 4.0).normal(0.0f, 1.0f, 0.0f).endVertex();
        bb.pos(0.0, 0.0, 4.0).normal(0.0f, 1.0f, 0.0f).endVertex();

        bb.pos(0.0, 0.0, 0.0).normal(-1.0f, 0.0f, 0.0f).endVertex();
        bb.pos(0.0, -1.0, 0.0).normal(-1.0f, 0.0f, 0.0f).endVertex();
        bb.pos(0.0, -1.0, 4.0).normal(-1.0f, 0.0f, 0.0f).endVertex();
        bb.pos(0.0, 0.0, 4.0).normal(-1.0f, 0.0f, 0.0f).endVertex();

        bb.pos(1.0, 0.0, 0.0).normal(1.0f, 0.0f, 0.0f).endVertex();
        bb.pos(1.0, -1.0, 0.0).normal(1.0f, 0.0f, 0.0f).endVertex();
        bb.pos(1.0, -1.0, 4.0).normal(1.0f, 0.0f, 0.0f).endVertex();
        bb.pos(1.0, 0.0, 4.0).normal(1.0f, 0.0f, 0.0f).endVertex();

        bb.pos(0.0, -1.0, 4.0).normal(0.0f, 0.0f, 1.0f).endVertex();
        bb.pos(1.0, -1.0, 4.0).normal(0.0f, 0.0f, 1.0f).endVertex();
        bb.pos(1.0, 0.0, 4.0).normal(0.0f, 0.0f, 1.0f).endVertex();
        bb.pos(0.0, 0.0, 4.0).normal(0.0f, 0.0f, 1.0f).endVertex();
        t.draw();

        if(isOn) {
            glTranslatef(0.5f, -0.5f, 0.0f);
            matrix1.position(0);
            glGetFloat(GL_MODELVIEW_MATRIX, matrix1); //Hax to get that damn position
        }

        glPopMatrix();

        if(isOn) {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

            //Actual laser
            glPushMatrix();
            glLoadIdentity();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.DST_ALPHA);
            glColor4f(1.0f, 0.0f, 0.0f, 0.5f);
            GlStateManager.glLineWidth(3.0f);

            matrix1.position(12);
            renderBuffer.put(matrix1.get());
            renderBuffer.put(matrix1.get());
            renderBuffer.put(matrix1.get() - 0.02f); //I know this is stupid, but it's the only thing that worked...
            renderBuffer.put(matrix1.get());
            renderBuffer.position(0);
            glVertexPointer(4, 0, renderBuffer);
            glEnableClientState(GL_VERTEX_ARRAY);
            glDrawArrays(GL_LINES, 0, 2);
            glDisableClientState(GL_VERTEX_ARRAY);
            glPopMatrix();
        }

        GlStateManager.enableTexture2D(); //Fix for shitty minecraft fire
    }

}
