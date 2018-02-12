/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.montoyo.wd.client.gui.loading.GuiLoader;
import net.montoyo.wd.client.gui.loading.JsonAWrapper;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;

public abstract class Container extends BasicControl {

    protected int paddingX = 0;
    protected int paddingY = 0;
    protected final ArrayList<Control> childs = new ArrayList<>();

    public <T extends Control> T addControl(T ctrl) {
        childs.add(ctrl);
        return ctrl;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if(!disabled) {
            for(Control ctrl : childs)
                ctrl.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public void keyUp(int key) {
        if(!disabled) {
            for(Control ctrl : childs)
                ctrl.keyUp(key);
        }
    }

    @Override
    public void keyDown(int key) {
        if(!disabled) {
            for(Control ctrl : childs)
                ctrl.keyDown(key);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                ctrl.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                ctrl.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                ctrl.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                ctrl.mouseMove(mouseX, mouseY);
        }
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int amount) {
        if(!disabled) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            for(Control ctrl : childs)
                ctrl.mouseScroll(mouseX, mouseY, amount);
        }
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        if(visible) {
            mouseX -= x + paddingX;
            mouseY -= y + paddingY;

            GL11.glPushMatrix();
            GL11.glTranslated((double) (x + paddingX), (double) (y + paddingY), 0.0);

            if(disabled) {
                for(Control ctrl : childs)
                    ctrl.draw(-1, -1, ptt);
            } else {
                for(Control ctrl : childs)
                    ctrl.draw(mouseX, mouseY, ptt);
            }

            GL11.glPopMatrix();
        }
    }

    @Override
    public void destroy() {
        for(Control ctrl : childs)
            ctrl.destroy();
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);

        JsonAWrapper objs = json.getArray("childs");
        for(int i = 0; i < objs.size(); i++)
            childs.add(GuiLoader.create(objs.getObject(i)));
    }

    public Control getByName(String name) {
        for(Control ctrl : childs) {
            if(name.equals(ctrl.name))
                return ctrl;

            if(ctrl instanceof Container) {
                Control ret = ((Container) ctrl).getByName(name);

                if(ret != null)
                    return ret;
            }
        }

        return null;
    }

}
