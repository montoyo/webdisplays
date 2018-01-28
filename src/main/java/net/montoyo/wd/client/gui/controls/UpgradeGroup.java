/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.item.ItemStack;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;

import java.util.ArrayList;

public class UpgradeGroup extends BasicControl {

    private int width;
    private int height;
    private ArrayList<ItemStack> upgrades;
    private ItemStack overStack;
    private final RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();

    public UpgradeGroup() {
        parent.requirePostDraw(this);
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        if(upgrades != null) {
            int x = this.x;

            for(ItemStack is: upgrades) {
                renderItem.renderItemAndEffectIntoGUI(mc.player, is, x, y);
                renderItem.renderItemOverlayIntoGUI(font, is, x, y, null);
                x += 18;
            }
        }
    }

    @Override
    public void postDraw(int mouseX, int mouseY, float ptt) {
        if(overStack != null)
            parent.drawItemStackTooltip(overStack, mouseX, mouseY);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setWidth(int w) {
        width = w;
    }

    public void setHeight(int h) {
        height = h;
    }

    public void setUpgrades(ArrayList<ItemStack> upgrades) {
        this.upgrades = upgrades;
    }

    public ArrayList<ItemStack> getUpgrades() {
        return upgrades;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        width = json.getInt("width", 0);
        height = json.getInt("height", 16);
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        overStack = null;

        if(mouseY >= y && mouseY <= y + 16 && mouseX >= x) {
            mouseX -= x;
            int sel = mouseX / 18;

            if(sel < upgrades.size() && mouseX % 18 <= 16)
                overStack = upgrades.get(sel);
        }
    }

}
