/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.miniserv.client.ClientTask;
import net.montoyo.wd.miniserv.client.ClientTaskGetFileList;
import net.montoyo.wd.miniserv.client.ClientTaskGetQuota;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.Util;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.*;

public class GuiServer extends WDScreen {

    private static final ResourceLocation BG_IMAGE = new ResourceLocation("webdisplays", "textures/gui/server_bg.png");
    private static final ResourceLocation FG_IMAGE = new ResourceLocation("webdisplays", "textures/gui/server_fg.png");
    private static final HashMap<String, Method> COMMAND_MAP = new HashMap<>();

    private final NameUUIDPair owner;
    private final ArrayList<String> lines = new ArrayList<>();
    private String prompt = "";
    private String userPrompt;
    private int blinkTime;
    private String lastCmd;
    private boolean promptLocked;
    private long queryTime;
    private ClientTask<?> currentTask;

    //Access command
    private int accessTrials;
    private int accessTime;
    private int accessState = -1;
    private PositionedSoundRecord accessSound;

    public GuiServer(NameUUIDPair owner) {
        this.owner = owner;
        //userPrompt = owner.name + "@miniserv$ ";
        userPrompt = "> ";

        if(COMMAND_MAP.isEmpty())
            buildCommandMap();

        lines.add("MiniServ 1.0");
        lines.add(tr("info"));
    }

    private static String tr(String key, Object ... args) {
        return I18n.format("webdisplays.server." + key, args);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float ptt) {
        super.drawScreen(mouseX, mouseY, ptt);

        int x = (width - 256) / 2;
        int y = (height - 176) / 2;

        GlStateManager.enableTexture2D();
        mc.renderEngine.bindTexture(BG_IMAGE);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedModalRect(x, y, 0, 0, 256, 176);

        x += 18;
        y += 18;

        for(String line: lines) {
            fontRenderer.drawString(line, x, y, 0xFFFFFFFF, false);
            y += 12;
        }

        if(!promptLocked) {
            x = fontRenderer.drawString(userPrompt, x, y, 0xFFFFFFFF, false);
            x = fontRenderer.drawString(prompt, x, y, 0xFFFFFFFF, false);
        }

        if(blinkTime < 5) {
            double xd = (double) (x + 1);
            double yd = (double) y;
            double zd = (double) zLevel;

            GlStateManager.disableTexture2D();
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            Tessellator t = Tessellator.getInstance();
            BufferBuilder bb = t.getBuffer();
            bb.begin(GL_QUADS, DefaultVertexFormats.POSITION);
            bb.pos(xd, yd + 8.0f, zd).endVertex();
            bb.pos(xd + 6.0f, yd + 8.0f, zd).endVertex();
            bb.pos(xd + 6.0f, yd, zd).endVertex();
            bb.pos(xd, yd, zd).endVertex();
            t.draw();
        }

        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        mc.renderEngine.bindTexture(FG_IMAGE);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedModalRect((width - 256) / 2, (height - 176) / 2, 0, 0, 256, 176);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if(accessState >= 0) {
            if(--accessTime <= 0) {
                accessState++;

                if(accessState == 1) {
                    if(lines.size() > 0)
                        lines.remove(lines.size() - 1);

                    lines.add("access: PERMISSION DENIED....and...");
                    accessTime = 20;
                } else {
                    if(accessSound == null) {
                        accessSound = new PositionedSoundRecord(WebDisplays.INSTANCE.soundServer.getSoundName(), SoundCategory.MASTER, 1.0f, 1.0f, true, 0, ISound.AttenuationType.NONE, 0.0f, 0.0f, 0.0f);
                        mc.getSoundHandler().playSound(accessSound);
                    }

                    writeLine("YOU DIDN'T SAY THE MAGIC WORD!");
                    accessTime = 2;
                }
            }
        } else {
            blinkTime = (blinkTime + 1) % 10;

            if(currentTask != null && System.currentTimeMillis() - queryTime >= 10000) {
                writeLine(tr("timeout"));
                currentTask.cancel();
                clearTask();
            }
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        if(!promptLocked && Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_UP) {
            if(lastCmd != null)
                prompt = lastCmd;

            return;
        }

        super.handleKeyboardInput();

        if(Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_L && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)))
            lines.clear();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if(promptLocked)
            return;

        if(keyCode == Keyboard.KEY_BACK) {
            if(prompt.length() > 0)
                prompt = prompt.substring(0, prompt.length() - 1);
        } else if(keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if(prompt.length() > 0) {
                writeLine(userPrompt + prompt);
                evaluateCommand(prompt);
                lastCmd = prompt;
                prompt = "";
            } else
                writeLine(userPrompt);
        } else if(prompt.length() + 1 < 30 && typedChar >= 32 && typedChar <= 126)
            prompt = prompt + typedChar;

        blinkTime = 0;
    }

    private void evaluateCommand(String str) {
        String[] args = str.trim().split("\\s+");
        Method handler = COMMAND_MAP.get(args[0].toLowerCase());

        if(handler == null) {
            writeLine(tr("unknowncmd"));
            return;
        }

        Object[] params;
        if(handler.getParameterCount() == 0)
            params = new Object[0];
        else {
            String[] args2 = new String[args.length - 1];
            System.arraycopy(args, 1, args2, 0, args2.length);
            params = new Object[] { args2 };
        }

        try {
            handler.invoke(this, params);
        } catch(IllegalAccessException | InvocationTargetException e) {
            Log.errorEx("Caught exception while running command \"%s\"", e, str);
            writeLine(tr("error"));
        }
    }

    private void writeLine(String line) {
        while(lines.size() >= 11)
            lines.remove(0);

        lines.add(line);
    }

    private static void buildCommandMap() {
        COMMAND_MAP.clear();

        Method[] methods = GuiServer.class.getMethods();
        for(Method m: methods) {
            CommandHandler cmd = m.getAnnotation(CommandHandler.class);

            if(cmd != null && Modifier.isPublic(m.getModifiers())) {
                if(m.getParameterCount() == 0 || (m.getParameterCount() == 1 && m.getParameterTypes()[0] == String[].class))
                    COMMAND_MAP.put(cmd.value().toLowerCase(), m);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();

        if(accessSound != null)
            mc.getSoundHandler().stopSound(accessSound);
    }

    private boolean queueTask(ClientTask<?> task) {
        if(Client.getInstance().addTask(task)) {
            promptLocked = true;
            queryTime = System.currentTimeMillis();
            currentTask = task;
            return true;
        } else {
            writeLine(tr("queryerr"));
            return false;
        }
    }

    private void clearTask() {
        promptLocked = false;
        currentTask = null;
    }

    @CommandHandler("clear")
    public void commandClear() {
        lines.clear();
    }

    @CommandHandler("help")
    public void commandHelp() {
        for(String c : COMMAND_MAP.keySet())
            writeLine(c + " - " + tr("help." + c));
    }

    @CommandHandler("exit")
    public void commandExit() {
        mc.displayGuiScreen(null);
    }

    @CommandHandler("access")
    public void commandAccess(String[] args) {
        boolean handled = false;

        if(args.length >= 1 && args[0].equalsIgnoreCase("security")) {
            if(args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("grid")))
                handled = true;
        } else if(args.length == 3 && args[0].equalsIgnoreCase("main") && args[1].equalsIgnoreCase("security") && args[2].equalsIgnoreCase("grid"))
            handled = true;

        if(handled) {
            writeLine("access: PERMISSION DENIED.");

            if(++accessTrials >= 3) {
                promptLocked = true;
                accessState = 0;
                accessTime = 20;
            }
        } else
            writeLine(tr("argerror"));
    }

    @CommandHandler("owner")
    public void commandOwner() {
        writeLine(tr("ownername", owner.name));
        writeLine(tr("owneruuid"));
        writeLine(owner.uuid.toString());
    }

    @CommandHandler("quota")
    public void commandQuota() {
        if(!mc.player.getGameProfile().getId().equals(owner.uuid)) {
            writeLine(tr("errowner"));
            return;
        }

        ClientTaskGetQuota task = new ClientTaskGetQuota();
        task.setFinishCallback((t) -> {
            writeLine(tr("quota", Util.sizeString(t.getQuota()), Util.sizeString(t.getMaxQuota())));
            clearTask();
        });

        queueTask(task);
    }

    @CommandHandler("ls")
    public void commandList() {
        ClientTaskGetFileList task = new ClientTaskGetFileList(owner.uuid);
        task.setFinishCallback((t) -> {
            String[] files = t.getFileList();
            if(files != null)
                Arrays.stream(files).forEach(this::writeLine);

            clearTask();
        });

        queueTask(task);
    }

}
