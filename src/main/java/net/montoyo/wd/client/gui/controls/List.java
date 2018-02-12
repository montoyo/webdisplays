/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.minecraft.client.shader.Framebuffer;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;

public class List extends BasicControl {

    private static class Entry {
        public final String text;
        public final Object userdata;

        public Entry(String t, Object o) {
            text = t;
            userdata = o;
        }

    }

    public static class EntryClick extends Event<List> {

        private final int id;
        private final Entry entry;

        private EntryClick(List lst) {
            source = lst;
            id = lst.selected;
            entry = lst.content.get(lst.selected);
        }

        public int getId() {
            return id;
        }

        public String getLabel() {
            return entry.text;
        }

        public Object getUserdata() {
            return entry.userdata;
        }

    }

    private int width;
    private int height;
    private final ArrayList<Entry> content = new ArrayList<>();
    private Framebuffer fbo;
    private int selected = -1;
    private boolean update;
    private int selColor = 0xFF0080FF;

    //Scroll handling
    private int contentH = 0;
    private int scrollSize;
    private int scrollPos = 0;
    private boolean scrolling = false;
    private int scrollGrab;

    public List() {
    }

    public List(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        width = w;
        height = h;
        scrollSize = h - 2;
        createFBO();
    }

    private int getYOffset() {
        double amount = ((double) scrollPos) / ((double) (height - 2 - scrollSize)) * ((double) (contentH - height));
        return (int) amount;
    }

    private boolean isInScrollbar(int mouseX, int mouseY) {
        return mouseX >= x + width - 5 && mouseX <= x + width - 1 && mouseY >= y + 1 + scrollPos && mouseY <= y + 1 + scrollPos + scrollSize;
    }

    private void createFBO() {
        if(fbo != null)
            fbo.deleteFramebuffer();

        fbo = new Framebuffer(parent.screen2DisplayX(width), parent.screen2DisplayY(height), false);
        fbo.setFramebufferFilter(GL_NEAREST);
        fbo.bindFramebuffer(false);
        glClearColor(0.0f, 0.0f, 0.0f, 1.f); //Set alpha to 1
        glClear(GL_COLOR_BUFFER_BIT);
        fbo.unbindFramebuffer();
        update = true;
    }

    private void renderToFBO() {
        beginFramebuffer(fbo, width, height);
        fillRect(0, 0, width, height, COLOR_BLACK);
        glColor4f(1.f, 1.f, 1.f, 1.f);

        int offset = 4 - getYOffset();
        for(int i = 0; i < content.size(); i++) {
            int pos = i * 12 + offset;

            if(pos + 12 >= 1) {
                if(pos >= height - 1)
                    break;

                int color = (i == selected) ? selColor : COLOR_WHITE;
                font.drawString(content.get(i).text, 4, i * 12 + offset, color);
            }
        }

        drawBorder(0, 0, width, height, 0xFF808080);
        endFramebuffer(fbo);
    }

    @Override
    public void destroy() {
        if(fbo != null)
            fbo.deleteFramebuffer();
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
        createFBO();
    }

    public void setWidth(int width) {
        this.width = width;
        createFBO();
    }

    public void setHeight(int height) {
        this.height = height;
        createFBO();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void updateContent() {
        contentH = content.size() * 12 + 4;

        int h2 = height - 2;
        if(contentH <= h2) {
            scrollSize = h2;
            scrollPos = 0;
        } else {
            scrollSize = h2 * h2 / contentH;

            if(scrollSize < 4)
                scrollSize = 4;
        }

        update = true;
    }

    public int addElement(String str) {
        return addElement(str, null);
    }

    public int addElement(String str, Object ud) {
        content.add(new Entry(str, ud));
        updateContent();
        return content.size() - 1;
    }

    public int addElementRaw(String str) {
        return addElement(str, null);
    }

    public int addElementRaw(String str, Object ud) {
        content.add(new Entry(str, ud));
        return content.size() - 1;
    }

    @Override
    public void setDisabled(boolean dis) {
        disabled = dis;

        if(dis) {
            selected = -1;
            update = true;
        }
    }

    @Override
    public void disable() {
        disabled = true;
        selected = -1;
        update = true;
    }

    @Override
    public void mouseMove(int mouseX, int mouseY) {
        int sel = -1;
        if(!disabled && mouseX >= x + 1 && mouseX <= x + width - 6 && mouseY >= y + 2 && mouseY <= y + height - 2) {
            int offset = y + 4 - getYOffset();
            sel = (mouseY - offset) / 12;

            if(sel < 0 || sel >= content.size())
                sel = -1;
        }

        if(selected != sel) {
            selected = sel;
            update = true;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if(!disabled && mouseButton == 0) {
            if(isInScrollbar(mouseX, mouseY)) {
                scrolling = true;
                scrollGrab = mouseY - (y + 1 + scrollPos);
            } else if(selected >= 0)
                parent.actionPerformed(new EntryClick(this));
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if(!disabled && scrolling)
            scrolling = false;
    }

    @Override
    public void mouseScroll(int mouseX, int mouseY, int amount) {
        if(!disabled && !scrolling && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            double disp = 12.d * ((double) (height - 2 - scrollSize)) / ((double) (contentH - height));
            int sp = scrollPos;

            if(amount < 0)
                sp += (int) disp;
            else
                sp -= (int) disp;

            if(sp < 0)
                sp = 0;
            else if(sp > height - 2 - scrollSize)
                sp = height - 2 - scrollSize;

            if(sp != scrollPos) {
                scrollPos = sp;
                update = true;
            }
        }
    }

    @Override
    public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if(!disabled && scrolling) {
            int sp = mouseY - scrollGrab - y - 1;
            if(sp < 0)
                sp = 0;
            else if(sp > height - 2 - scrollSize)
                sp = height - 2 - scrollSize;

            if(scrollPos != sp) {
                scrollPos = sp;
                update = true;
            }
        }
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        if(visible) {
            if(update) {
                renderToFBO();
                update = false;
            }

            fbo.bindFramebufferTexture();
            glColor4f(1.f, 1.f, 1.f, 1.f);
            fillTexturedRect(x, y, width, height, 0.0, 1.0, 1.0, 0.0);
            fbo.unbindFramebufferTexture();

            fillRect(x + width - 5, y + 1 + scrollPos, 4, scrollSize, (scrolling || isInScrollbar(mouseX, mouseY)) ? 0xFF202020 : 0xFF404040);
        }
    }

    public String getEntryLabel(int id) {
        return content.get(id).text;
    }

    public Object getEntryUserdata(int id) {
        return content.get(id).userdata;
    }

    public int findEntryByLabel(String label) {
        for(int i = 0; i < content.size(); i++) {
            if(content.get(i).text.equals(label))
                return i;
        }

        return -1;
    }

    public int findEntryByUserdata(Object o) {
        if(o == null) {
            for(int i = 0; i < content.size(); i++) {
                if(content.get(i).userdata == null)
                    return i;
            }
        } else {
            for(int i = 0; i < content.size(); i++) {
                if(content.get(i).userdata != null && content.get(i).userdata.equals(o))
                    return i;
            }
        }

        return -1;
    }

    public void setSelectionColor(int selColor) {
        this.selColor = selColor;
    }

    public int getSelectionColor() {
        return selColor;
    }

    public int getElementCount() {
        return content.size();
    }

    public void removeElement(int id) {
        if(selected != -1 && id == content.size() - 1)
                selected = -1;

        content.remove(id);
        updateContent();
    }

    public void removeElementRaw(int id) {
        if(selected != -1 && id == content.size() - 1)
            selected = -1;

        content.remove(id);
    }

    public void clear() {
        content.clear();
        scrollPos = 0;
        scrolling = false;
        scrollSize = height - 2;
        selected = -1;
        update = true;
    }

    public void clearRaw() {
        content.clear();
        scrollPos = 0;
        scrolling = false;
        selected = -1;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        width = json.getInt("width", 100);
        height = json.getInt("height", 100);
        selColor = json.getColor("selectionColor", 0xFF0080FF);
        createFBO();
    }

}
