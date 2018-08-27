/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;

import java.util.Arrays;

public class CheckBox extends BasicControl {

    private static final ResourceLocation texUnchecked = new ResourceLocation("webdisplays", "textures/gui/checkbox.png");
    private static final ResourceLocation texChecked = new ResourceLocation("webdisplays", "textures/gui/checkbox_checked.png");
    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;

    public static class CheckedEvent extends Event<CheckBox> {

        private final boolean checked;

        private CheckedEvent(CheckBox cb) {
            source = cb;
            checked = cb.checked;
        }

        public boolean isChecked() {
            return checked;
        }

    }

    private String label;
    private int labelW;
    private boolean checked;
    private java.util.List<String> tooltip;

    public CheckBox() {
        label = "";
    }

    public CheckBox(int x, int y, String label) {
        this.label = label;
        labelW = font.getStringWidth(label);
        checked = false;
        this.x = x;
        this.y = y;
    }

    public CheckBox(int x, int y, String label, boolean val) {
        this.label = label;
        labelW = font.getStringWidth(label);
        checked = val;
        this.x = x;
        this.y = y;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(mouseButton == 0 && !disabled) {
            if(mouseX >= x && mouseX <= x + WIDTH + 2 + labelW && mouseY >= y && mouseY < y + HEIGHT) {
                checked = !checked;
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                parent.actionPerformed(new CheckedEvent(this));
            }
        }
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        if(visible) {
            GlStateManager.disableAlpha();

            bindTexture(checked ? texChecked : texUnchecked);
            blend(true);
            fillTexturedRect(x, y, WIDTH, HEIGHT, 0.0, 0.0, 1.0, 1.0);
            blend(false);
            bindTexture(null);

            boolean inside = (!disabled && mouseX >= x && mouseX <= x + WIDTH + 2 + labelW && mouseY >= y && mouseY < y + HEIGHT);
            font.drawString(label, x + WIDTH + 2, y + 4, inside ? 0xFF0080FF : COLOR_WHITE);
        }
    }

    public void setLabel(String label) {
        this.label = label;
        labelW = font.getStringWidth(label);
    }

    public String getLabel() {
        return label;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    public int getWidth() {
        return WIDTH + 2 + labelW;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        label = tr(json.getString("label", ""));
        labelW = font.getStringWidth(label);
        checked = json.getBool("checked", false);

        String tt = tr(json.getString("tooltip", ""));
        if(!tt.isEmpty()) {
            tooltip = Arrays.asList(tt.split("\\\\n"));
            parent.requirePostDraw(this);
        }
    }

    @Override
    public void postDraw(int mouseX, int mouseY, float ptt) {
        if(tooltip != null && !disabled && mouseX >= x && mouseX <= x + WIDTH + 2 + labelW && mouseY >= y && mouseY < y + HEIGHT)
            parent.drawTooltip(tooltip, mouseX, mouseY);
    }

}
