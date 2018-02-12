/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.minecraft.client.gui.GuiTextField;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;

public class TextField extends Control {

    public static class EnterPressedEvent extends Event<TextField> {

        private final String text;

        private EnterPressedEvent(TextField field) {
            source = field;
            text = field.field.getText();
        }

        public String getText() {
            return text;
        }

    }

    public static class TabPressedEvent extends Event<TextField> {

        private final String beginning;

        private TabPressedEvent(TextField field) {
            source = field;

            String text = field.field.getText();
            int max = field.field.getCursorPosition();
            int spacePos = 0;

            for(int i = max - 1; i >= 0; i--) {
                if(Character.isSpaceChar(text.charAt(i))) {
                    spacePos = i;
                    break;
                }
            }

            beginning = text.substring(spacePos, max).trim();
        }

        public String getBeginning() {
            return beginning;
        }

    }

    public static class TextChangedEvent extends Event<TextField> {

        private final String oldContent;
        private final String newContent;

        private TextChangedEvent(TextField tf, String old) {
            source = tf;
            oldContent = old;
            newContent = tf.field.getText();
        }

        public String getOldContent() {
            return oldContent;
        }

        public String getNewContent() {
            return newContent;
        }

    }

    public interface TextChangeListener {

        void onTextChange(TextField tf, String oldContent, String newContent);

    }

    public static final int DEFAULT_TEXT_COLOR = 14737632;
    public static final int DEFAULT_DISABLED_COLOR = 7368816;

    private final GuiTextField field;
    private boolean enabled = true;
    private int textColor = DEFAULT_TEXT_COLOR;
    private int disabledColor = DEFAULT_DISABLED_COLOR;
    private final ArrayList<TextChangeListener> listeners = new ArrayList<>();

    public TextField() {
        field = new GuiTextField(0, font, 1, 1, 198, 20);
    }

    public TextField(int x, int y, int width, int height) {
        field = new GuiTextField(0, font, x + 1, y + 1, width - 2, height - 2);
    }

    public TextField(int x, int y, int width, int height, String text) {
        field = new GuiTextField(0, font, x + 1, y + 1, width - 2, height - 2);
        field.setText(text);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER)
            parent.actionPerformed(new EnterPressedEvent(this));
        else if(keyCode == Keyboard.KEY_TAB)
            parent.actionPerformed(new TabPressedEvent(this));
        else {
            String old;
            if(enabled && field.isFocused())
                old = field.getText();
            else
                old = null;

            field.textboxKeyTyped(typedChar, keyCode);

            if(enabled && field.isFocused() && !field.getText().equals(old)) {
                for(TextChangeListener tcl : listeners)
                    tcl.onTextChange(this, old, field.getText());

                parent.actionPerformed(new TextChangedEvent(this, old));
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void draw(int mouseX, int mouseY, float ptt) {
        field.drawTextBox();
    }

    public void setText(String text) {
        String old = field.getText();
        field.setText(text);

        if(!old.equals(text)) {
            for(TextChangeListener tcl : listeners)
                tcl.onTextChange(this, old, text);
        }
    }

    public void clear() {
        field.setText("");
    }

    public String getText() {
        return field.getText();
    }

    public String getSelectedText() {
        return field.getSelectedText();
    }

    public void setWidth(int width) {
        field.width = width - 2;
    }

    @Override
    public int getWidth() {
        return field.width + 2;
    }

    public void setHeight(int height) {
        field.height = height - 2;
    }

    @Override
    public int getHeight() {
        return field.height + 2;
    }

    public void setSize(int w, int h) {
        field.width = w - 2;
        field.height = h - 2;
    }

    @Override
    public void setPos(int x, int y) {
        field.x = x + 1;
        field.y = y + 1;
    }

    @Override
    public int getX() {
        return field.x - 1;
    }

    @Override
    public int getY() {
        return field.y - 1;
    }

    public void setDisabled(boolean en) {
        enabled = !en;
        field.setEnabled(enabled);
    }

    public boolean isDisabled() {
        return !enabled;
    }

    public void enable() {
        field.setEnabled(true);
        enabled = true;
    }

    public void disable() {
        field.setEnabled(false);
        enabled = false;
    }

    public void setVisible(boolean vi) {
        field.setVisible(vi);
    }

    public boolean isVisible() {
        return field.getVisible();
    }

    public void show() {
        field.setVisible(true);
    }

    public void hide() {
        field.setVisible(false);
    }

    public void setFocused(boolean val) {
        field.setFocused(val);
    }

    public boolean hasFocus() {
        return field.isFocused();
    }

    public void focus() {
        field.setFocused(true);
    }

    public void setMaxLength(int len) {
        field.setMaxStringLength(len);
    }

    public int getMaxLength() {
        return field.getMaxStringLength();
    }

    public void setTextColor(int color) {
        field.setTextColor(color);
        textColor = color;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setDisabledTextColor(int color) {
        field.setDisabledTextColour(color);
        disabledColor = color;
    }

    public int getDisabledTextColor() {
        return disabledColor;
    }

    public GuiTextField getMcField() {
        return field;
    }

    public void addTextChangeListener(TextChangeListener l) {
        if(l != null && !listeners.contains(l))
            listeners.add(l);
    }

    public void removeTextChangeListener(TextChangeListener l) {
        listeners.remove(l);
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        field.x = json.getInt("x", 0) + 1;
        field.y = json.getInt("y", 0) + 1;
        field.width = json.getInt("width", 200) - 2;
        field.height = json.getInt("height", 22) - 2;
        field.setText(tr(json.getString("text", "")));
        field.setVisible(json.getBool("visible", true));
        field.setMaxStringLength(json.getInt("maxLength", 32));

        enabled = !json.getBool("disabled", false);
        textColor = json.getColor("textColor", DEFAULT_TEXT_COLOR);
        disabledColor = json.getColor("disabledColor", DEFAULT_DISABLED_COLOR);

        field.setTextColor(textColor);
        field.setDisabledTextColour(disabledColor);
        field.setEnabled(enabled);
    }

}
