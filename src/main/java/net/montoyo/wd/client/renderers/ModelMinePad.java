/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ModelMinePad extends ModelBase {

    // fields
    ModelRenderer base;
    ModelRenderer left;
    ModelRenderer right;

    public ModelMinePad() {
        textureWidth = 64;
        textureHeight = 32;

        base = new ModelRenderer(this, 0, 0);
        base.addBox(0F, 0F, 0F, 14, 1, 9);
        base.setRotationPoint(1F, 0F, 3.5F);
        base.setTextureSize(64, 32);
        base.mirror = true;
        setRotation(base, 0F, 0F, 0F);
        left = new ModelRenderer(this, 0, 10);
        left.addBox(0F, 0F, 0F, 1, 1, 7);
        left.setRotationPoint(0F, 0F, 4.5F);
        left.setTextureSize(64, 32);
        left.mirror = true;
        setRotation(left, 0F, 0F, 0F);
        right = new ModelRenderer(this, 30, 10);
        right.addBox(0F, 0F, 0F, 1, 1, 7);
        right.setRotationPoint(15F, 0F, 4.5F);
        right.setTextureSize(64, 32);
        right.mirror = true;
        setRotation(right, 0F, 0F, 0F);
    }

    public final void render(float f5) {
        base.render(f5);
        left.render(f5);
        right.render(f5);
    }

    private void setRotation(ModelRenderer model, float x, float y, float z) {
        model.rotateAngleX = x;
        model.rotateAngleY = y;
        model.rotateAngleZ = z;
    }

}
