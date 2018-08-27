/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.controls.Container;
import net.montoyo.wd.client.gui.controls.Control;
import net.montoyo.wd.client.gui.controls.Event;
import net.montoyo.wd.client.gui.controls.List;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.client.gui.loading.GuiLoader;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import net.montoyo.wd.net.server.SMessageACQuery;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Bounds;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.NameUUIDPair;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public abstract class WDScreen extends GuiScreen {

    public static WDScreen CURRENT_SCREEN = null;

    protected final ArrayList<Control> controls = new ArrayList<>();
    protected final ArrayList<Control> postDrawList = new ArrayList<>();
    private final HashMap<Class<? extends Event>, Method> eventMap = new HashMap<>();
    protected boolean quitOnEscape = true;
    protected boolean defaultBackground = true;
    protected int syncTicks = 40;
    private int syncTicksLeft = -1;

    public WDScreen() {
        Method[] methods = getClass().getMethods();

        for(Method m : methods) {
            if(m.getAnnotation(GuiSubscribe.class) != null) {
                if(!Modifier.isPublic(m.getModifiers()))
                    throw new RuntimeException("Found non public @GuiSubscribe");

                Class<?> params[] = m.getParameterTypes();
                if(params.length != 1 || !Event.class.isAssignableFrom(params[0]))
                    throw new RuntimeException("Invalid parameters for @GuiSubscribe");

                eventMap.put((Class<? extends Event>) params[0], m);
            }
        }
    }

    protected <T extends Control> T addControl(T ctrl) {
        controls.add(ctrl);
        return ctrl;
    }

    public int screen2DisplayX(int x) {
        double ret = ((double) x) / ((double) width) * ((double) mc.displayWidth);
        return (int) ret;
    }

    public int screen2DisplayY(int y) {
        double ret = ((double) y) / ((double) height) * ((double) mc.displayHeight);
        return (int) ret;
    }

    public int display2ScreenX(int x) {
        double ret = ((double) x) / ((double) mc.displayWidth) * ((double) width);
        return (int) ret;
    }

    public int display2ScreenY(int y) {
        double ret = ((double) y) / ((double) mc.displayHeight) * ((double) height);
        return (int) ret;
    }

    protected void centerControls() {
        //Determine bounding box
        Bounds bounds = Control.findBounds(controls);

        //Translation vector
        int diffX = (width - bounds.maxX - bounds.minX) / 2;
        int diffY = (height - bounds.maxY - bounds.minY) / 2;

        //Translate controls
        for(Control ctrl : controls) {
            int x = ctrl.getX();
            int y = ctrl.getY();

            ctrl.setPos(x + diffX, y + diffY);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float ptt) {
        if(defaultBackground)
            drawDefaultBackground();

        for(Control ctrl: controls)
            ctrl.draw(mouseX, mouseY, ptt);

        for(Control ctrl: postDrawList)
            ctrl.postDraw(mouseX, mouseY, ptt);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(quitOnEscape && keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }

        for(Control ctrl: controls)
            ctrl.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        for(Control ctrl: controls)
            ctrl.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        for(Control ctrl: controls)
            ctrl.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        for(Control ctrl: controls)
            ctrl.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void initGui() {
        CURRENT_SCREEN = this;
        Keyboard.enableRepeatEvents(true);
    }

    @Override
    public void onGuiClosed() {
        if(syncTicksLeft >= 0) {
            sync();
            syncTicksLeft = -1;
        }

        for(Control ctrl : controls)
            ctrl.destroy();

        Keyboard.enableRepeatEvents(false);
        CURRENT_SCREEN = null;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int x = Mouse.getEventX() * width / mc.displayWidth;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int dw = Mouse.getEventDWheel();

        if(dw != 0)
            onMouseScroll(x, y, dw);
        else if(Mouse.getEventButton() == -1)
            onMouseMove(x, y);
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();

        int key = Keyboard.getEventKey();
        if(key != Keyboard.KEY_NONE) {
            if(Keyboard.getEventKeyState()) {
                for(Control ctrl : controls)
                    ctrl.keyDown(key);
            } else {
                for(Control ctrl : controls)
                    ctrl.keyUp(key);
            }
        }
    }

    public void onMouseScroll(int mouseX, int mouseY, int amount) {
        for(Control ctrl : controls)
            ctrl.mouseScroll(mouseX, mouseY, amount);
    }

    public void onMouseMove(int mouseX, int mouseY) {
        for(Control ctrl : controls)
            ctrl.mouseMove(mouseX, mouseY);
    }

    public Object actionPerformed(Event ev) {
        Method m = eventMap.get(ev.getClass());

        if(m != null) {
            try {
                return m.invoke(this, ev);
            } catch(IllegalAccessException e) {
                Log.errorEx("Access to event %s of screen %s is denied", e, ev.getClass().getSimpleName(), getClass().getSimpleName());
            } catch(InvocationTargetException e) {
                Log.errorEx("Event %s of screen %s failed", e, ev.getClass().getSimpleName(), getClass().getSimpleName());
            }
        }

        return null;
    }

    public <T extends Control> T getControlByName(String name) {
        for(Control ctrl : controls) {
            if(name.equals(ctrl.getName()))
                return (T) ctrl;

            if(ctrl instanceof Container) {
                Control ret = ((Container) ctrl).getByName(name);

                if(ret != null)
                    return (T) ret;
            }
        }

        return null;
    }

    protected void addLoadCustomVariables(Map<String, Double> vars) {
    }

    public void loadFrom(ResourceLocation resLoc) {
        JsonObject root = GuiLoader.getJson(resLoc);
        if(root == null)
            throw new RuntimeException("Could not load GUI file " + resLoc.toString());

        if(!root.has("controls") || !root.get("controls").isJsonArray())
            throw new RuntimeException("In GUI file " + resLoc.toString() + ": missing root 'controls' object.");

        HashMap<String, Double> vars = new HashMap<>();
        vars.put("width", (double) width);
        vars.put("height", (double) height);
        vars.put("displayWidth", (double) mc.displayWidth);
        vars.put("displayHeight", (double) mc.displayHeight);
        addLoadCustomVariables(vars);

        JsonArray content = root.get("controls").getAsJsonArray();
        for(JsonElement elem: content)
            controls.add(GuiLoader.create(new JsonOWrapper(elem.getAsJsonObject(), vars)));

        Field[] fields = getClass().getDeclaredFields();
        for(Field f: fields) {
            f.setAccessible(true);
            FillControl fc = f.getAnnotation(FillControl.class);

            if(fc != null) {
                String name = fc.name().isEmpty() ? f.getName() : fc.name();
                Control ctrl = getControlByName(name);

                if(ctrl == null) {
                    if(fc.required())
                        throw new RuntimeException("In GUI file " + resLoc.toString() + ": missing required control " + name);

                    continue;
                }

                if(!f.getType().isAssignableFrom(ctrl.getClass()))
                    throw new RuntimeException("In GUI file " + resLoc.toString() + ": invalid type for control " + name);

                try {
                    f.set(this, ctrl);
                } catch(IllegalAccessException e) {
                    if(fc.required())
                        throw new RuntimeException(e);
                }
            }
        }

        if(root.has("center") && root.get("center").getAsBoolean())
            centerControls();
    }

    @Override
    public void onResize(@Nonnull Minecraft mcIn, int w, int h) {
        for(Control ctrl : controls)
            ctrl.destroy();

        controls.clear();
        super.onResize(mcIn, w, h);
    }

    protected void requestAutocomplete(String beginning, boolean matchExact) {
        WebDisplays.NET_HANDLER.sendToServer(new SMessageACQuery(beginning, matchExact));
    }

    public void onAutocompleteResult(NameUUIDPair pairs[]) {
    }

    public void onAutocompleteFailure() {
    }

    protected void requestSync() {
        syncTicksLeft = syncTicks - 1;
    }

    protected boolean syncRequested() {
        return syncTicksLeft >= 0;
    }

    protected void abortSync() {
        syncTicksLeft = -1;
    }

    protected void sync() {
    }

    @Override
    public void updateScreen() {
        if(syncTicksLeft >= 0) {
            if(--syncTicksLeft < 0)
                sync();
        }
    }

    public void drawItemStackTooltip(ItemStack is, int x, int y) {
        renderToolTip(is, x, y); //Since it's protected...
    }

    public void drawTooltip(java.util.List<String> lines, int x, int y) {
        drawHoveringText(lines, x, y, fontRenderer); //This is also protected...
    }

    public void requirePostDraw(Control ctrl) {
        if(!postDrawList.contains(ctrl))
            postDrawList.add(ctrl);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public abstract boolean isForBlock(BlockPos bp, BlockSide side);

    @Nullable
    public String getWikiPageName() {
        return null;
    }

}
