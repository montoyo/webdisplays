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
import net.minecraft.util.math.BlockPos;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.miniserv.Constants;
import net.montoyo.wd.miniserv.client.*;
import net.montoyo.wd.utilities.*;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.*;

public class GuiServer extends WDScreen {

    private static final ResourceLocation BG_IMAGE = new ResourceLocation("webdisplays", "textures/gui/server_bg.png");
    private static final ResourceLocation FG_IMAGE = new ResourceLocation("webdisplays", "textures/gui/server_fg.png");
    private static final HashMap<String, Method> COMMAND_MAP = new HashMap<>();
    private static final int MAX_LINE_LEN = 32;
    private static final int MAX_LINES = 12;

    private final Vector3i serverPos;
    private final NameUUIDPair owner;
    private final ArrayList<String> lines = new ArrayList<>();
    private String prompt = "";
    private String userPrompt;
    private int blinkTime;
    private String lastCmd;
    private boolean promptLocked;
    private volatile long queryTime;
    private ClientTask<?> currentTask;
    private int selectedLine = -1;

    //Access command
    private int accessTrials;
    private int accessTime;
    private int accessState = -1;
    private PositionedSoundRecord accessSound;

    //Upload wizard
    private boolean uploadWizard;
    private File uploadDir;
    private final ArrayList<File> uploadFiles = new ArrayList<>();
    private int uploadOffset;
    private boolean uploadFirstIsParent;
    private String uploadFilter = "";
    private long uploadFilterTime;

    public GuiServer(Vector3i vec, NameUUIDPair owner) {
        serverPos = vec;
        this.owner = owner;
        //userPrompt = owner.name + "@miniserv$ ";
        userPrompt = "> ";

        if(COMMAND_MAP.isEmpty())
            buildCommandMap();

        lines.add("MiniServ 1.0");
        lines.add(tr("info"));
        uploadCD(FileSystemView.getFileSystemView().getDefaultDirectory());
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

        for(int i = 0; i < lines.size(); i++) {
            if(selectedLine == i) {
                drawWhiteQuad(x - 1, y - 2, fontRenderer.getStringWidth(lines.get(i)) + 1, 12);
                fontRenderer.drawString(lines.get(i), x, y, 0xFF129700, false);
            } else
                fontRenderer.drawString(lines.get(i), x, y, 0xFFFFFFFF, false);

            y += 12;
        }

        if(!promptLocked) {
            x = fontRenderer.drawString(userPrompt, x, y, 0xFFFFFFFF, false);
            x = fontRenderer.drawString(prompt, x, y, 0xFFFFFFFF, false);
        }

        if(!uploadWizard && blinkTime < 5)
            drawWhiteQuad(x + 1, y, 6, 8);

        GlStateManager.disableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        mc.renderEngine.bindTexture(FG_IMAGE);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        drawTexturedModalRect((width - 256) / 2, (height - 176) / 2, 0, 0, 256, 176);
        GlStateManager.enableAlpha();
    }

    private void drawWhiteQuad(int x, int y, int w, int h) {
        double xd = (double) x;
        double xd2 = (double) (x + w);
        double yd = (double) y;
        double yd2 = (double) (y + h);
        double zd = (double) zLevel;

        GlStateManager.disableTexture2D();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        bb.begin(GL_QUADS, DefaultVertexFormats.POSITION);
        bb.pos(xd, yd2, zd).endVertex();
        bb.pos(xd2, yd2, zd).endVertex();
        bb.pos(xd2, yd, zd).endVertex();
        bb.pos(xd, yd, zd).endVertex();
        t.draw();
        GlStateManager.enableTexture2D();
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

            if(currentTask != null) {
                long queryTime;
                synchronized(this) {
                    queryTime = this.queryTime;
                }

                if(System.currentTimeMillis() - queryTime >= 10000) {
                    writeLine(tr("timeout"));
                    currentTask.cancel();
                    clearTask();
                }
            }

            if(!uploadFilter.isEmpty() && System.currentTimeMillis() - uploadFilterTime >= 1000) {
                Log.info("Upload filter cleared");
                uploadFilter = "";
            }
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        boolean keyState = Keyboard.getEventKeyState();
        int keyCode = Keyboard.getEventKey();

        if(uploadWizard) {
            if(keyState) {
                if(keyCode == Keyboard.KEY_UP) {
                    if(selectedLine > 3)
                        selectedLine--;
                    else if(uploadOffset > 0) {
                        uploadOffset--;
                        updateUploadScreen();
                    }
                } else if(keyCode == Keyboard.KEY_DOWN) {
                    if(selectedLine < MAX_LINES - 1)
                        selectedLine++;
                    else if(uploadOffset + selectedLine - 2 < uploadFiles.size()) {
                        uploadOffset++;
                        updateUploadScreen();
                    }
                } else if(keyCode == Keyboard.KEY_PRIOR) {
                    selectedLine = 3;
                    int dst = uploadOffset - (MAX_LINES - 3);
                    if(dst < 0)
                        dst = 0;

                    selectFile(dst);
                } else if(keyCode == Keyboard.KEY_NEXT) {
                    selectedLine = 3;
                    int dst = uploadOffset + (MAX_LINES - 3);
                    if(dst >= uploadFiles.size())
                        dst = uploadFiles.size() - 1;

                    selectFile(dst);
                } else if(keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
                    File file = uploadFiles.get(uploadOffset + selectedLine - 3);

                    if(file.isDirectory()) {
                        uploadCD(file);
                        updateUploadScreen();
                    } else
                        startFileUpload(file, true);
                } else if(keyCode == Keyboard.KEY_F5) {
                    uploadCD(uploadDir);
                    updateUploadScreen();
                }
            }

            if(keyCode == Keyboard.KEY_ESCAPE) {
                quitUploadWizard();
                return; //Don't let the screen handle this
            }

            super.handleKeyboardInput();
        } else {
            super.handleKeyboardInput();

            if(keyState) {
                boolean ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);

                if(keyCode == Keyboard.KEY_L && ctrl)
                    lines.clear();
                else if(keyCode == Keyboard.KEY_V && ctrl) {
                    prompt += getClipboardString();

                    if(prompt.length() > MAX_LINE_LEN)
                        prompt = prompt.substring(0, MAX_LINE_LEN);
                } else if(keyCode == Keyboard.KEY_UP) {
                    if(lastCmd != null) {
                        String tmp = prompt;
                        prompt = lastCmd;
                        lastCmd = tmp;
                    }
                }
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);

        if(uploadWizard) {
            boolean found = false;
            uploadFilter += Character.toLowerCase(typedChar);
            uploadFilterTime = System.currentTimeMillis();

            for(int i = uploadFirstIsParent ? 1 : 0; i < uploadFiles.size(); i++) {
                if(uploadFiles.get(i).getName().toLowerCase().startsWith(uploadFilter)) {
                    selectFile(i);
                    found = true;
                    break;
                }
            }

            if(!found && uploadFilter.length() == 1)
                uploadFilter = "";

            return;
        } else if(promptLocked)
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
        } else if(prompt.length() + 1 < MAX_LINE_LEN && typedChar >= 32 && typedChar <= 126)
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
        final int maxl = uploadWizard ? MAX_LINES : (MAX_LINES - 1); //Cuz prompt is hidden
        while(lines.size() >= maxl)
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

    private void quitUploadWizard() {
        lines.clear();
        promptLocked = false;
        uploadWizard = false;
        selectedLine = -1;
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
            queryTime = System.currentTimeMillis(); //No task is running so it's okay to have an unsynchronized access here
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

    private static String trimStringL(String str) {
        int delta = str.length() - MAX_LINE_LEN;
        if(delta <= 0)
            return str;

        return "..." + str.substring(delta + 3);
    }

    private static String trimStringR(String str) {
        return (str.length() <= MAX_LINE_LEN) ? str : (str.substring(0, MAX_LINE_LEN - 3) + "...");
    }

    @CommandHandler("clear")
    public void commandClear() {
        lines.clear();
    }

    @CommandHandler("help")
    public void commandHelp(String[] args) {
        if(args.length > 0) {
            String cmd = args[0].toLowerCase();

            if(COMMAND_MAP.containsKey(cmd))
                writeLine(tr("help." + cmd));
            else
                writeLine(tr("unknowncmd"));
        } else {
            for(String c : COMMAND_MAP.keySet())
                writeLine(c + " - " + tr("help." + c));
        }
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

    @CommandHandler("url")
    public void commandURL(String[] args) {
        if(args.length < 1) {
            writeLine(tr("fnamearg"));
            return;
        }

        String fname = Util.join(args, " ");
        if(Util.isFileNameInvalid(fname)) {
            writeLine(tr("nameerr"));
            return;
        }

        ClientTaskCheckFile task = new ClientTaskCheckFile(owner.uuid, fname);
        task.setFinishCallback((t) -> {
            int status = t.getStatus();
            if(status == 0) {
                writeLine(tr("urlcopied"));
                setClipboardString(t.getURL());
            } else if(status == Constants.GETF_STATUS_NOT_FOUND)
                writeLine(tr("notfound"));
            else
                writeLine(tr("error2", status));

            clearTask();
        });

        queueTask(task);
    }

    private void uploadCD(File newDir) {
        try {
            uploadDir = newDir.getCanonicalFile();
        } catch(IOException ex) {
            uploadDir = newDir;
        }

        uploadFiles.clear();
        File parent = uploadDir.getParentFile();

        if(parent != null && parent.exists()) {
            uploadFiles.add(parent);
            uploadFirstIsParent = true;
        } else
            uploadFirstIsParent = false;

        File[] children = uploadDir.listFiles();
        if(children != null) {
            Collator c = Collator.getInstance();
            c.setStrength(Collator.SECONDARY);
            c.setDecomposition(Collator.CANONICAL_DECOMPOSITION);

            Arrays.stream(children).filter(f -> !f.isHidden() && (f.isDirectory() || f.isFile())).sorted((a, b) -> c.compare(a.getName(), b.getName())).forEach(uploadFiles::add);
        }

        uploadOffset = 0;
        uploadFilter = "";

        if(uploadWizard)
            selectedLine = 3;
    }

    private void updateUploadScreen() {
        lines.clear();

        lines.add(tr("upload.info"));
        lines.add(trimStringL(uploadDir.getPath()));
        lines.add("");

        for(int i = uploadOffset; i < uploadFiles.size() && lines.size() < MAX_LINES; i++) {
            if(i == 0 && uploadFirstIsParent)
                lines.add(tr("upload.parent"));
            else
                lines.add(trimStringR(uploadFiles.get(i).getName()));
        }
    }

    private void selectFile(int i) {
        int pos = 3 + i - uploadOffset;
        if(pos >= 3 && pos < MAX_LINES) {
            selectedLine = pos;
            return;
        }

        uploadOffset = i;
        if(uploadOffset + MAX_LINES - 3 > uploadFiles.size())
            uploadOffset = uploadFiles.size() - MAX_LINES + 3;

        updateUploadScreen();
        selectedLine = 3 + i - uploadOffset;
    }

    @CommandHandler("upload")
    public void commandUpload(String[] args) {
        if(!mc.player.getGameProfile().getId().equals(owner.uuid)) {
            writeLine(tr("errowner"));
            return;
        }

        if(args.length > 0) {
            File fle = new File(Util.join(args, " "));
            if(!fle.exists()) {
                writeLine(tr("notfound"));
                return;
            }

            if(fle.isDirectory())
                uploadCD(fle);
            else if(fle.isFile()) {
                startFileUpload(fle, false);
                return;
            } else {
                writeLine(tr("notfound"));
                return;
            }
        }

        uploadWizard = true;
        promptLocked = true;
        uploadOffset = 0;
        selectedLine = 3;
        updateUploadScreen();
    }

    @CommandHandler("rm")
    public void commandDelete(String[] args) {
        if(!mc.player.getGameProfile().getId().equals(owner.uuid)) {
            writeLine(tr("errowner"));
            return;
        }

        if(args.length < 1) {
            writeLine(tr("fnamearg"));
            return;
        }

        String fname = Util.join(args, " ");
        if(Util.isFileNameInvalid(fname)) {
            writeLine(tr("nameerr"));
            return;
        }

        ClientTaskDeleteFile task = new ClientTaskDeleteFile(fname);
        task.setFinishCallback((t) -> {
            int status = t.getStatus();
            if(status == 1)
                writeLine(tr("notfound"));
            else if(status != 0)
                writeLine(tr("error"));

            clearTask();
        });

        queueTask(task);
    }

    @CommandHandler("reconnect")
    public void commandReconnect() {
        Client.getInstance().stop();
        WebDisplays.NET_HANDLER.sendToServer(Client.getInstance().beginConnection());
    }

    private void startFileUpload(File f, boolean quit) {
        if(quit)
            quitUploadWizard();

        if(Util.isFileNameInvalid(f.getName()) || f.getName().length() >= MAX_LINE_LEN - 3) {
            writeLine(tr("nameerr"));
            return;
        }

        ClientTaskUploadFile task;
        try {
            task = new ClientTaskUploadFile(f);
        } catch(IOException ex) {
            writeLine(tr("error"));
            ex.printStackTrace();
            return;
        }

        task.setProgressCallback((cur, total) -> {
            synchronized(GuiServer.this) {
                queryTime = System.currentTimeMillis();
            }
        });

        task.setFinishCallback(t -> {
            int status = t.getUploadStatus();
            if(status == 0)
                writeLine(tr("upload.done"));
            else if(status == Constants.FUPA_STATUS_FILE_EXISTS)
                writeLine(tr("upload.exists"));
            else if(status == Constants.FUPA_STATUS_EXCEEDS_QUOTA)
                writeLine(tr("upload.quota"));
            else
                writeLine(tr("error2", status));

            clearTask();
        });

        if(queueTask(task))
            writeLine(tr("upload.uploading"));
    }

    @Override
    public boolean isForBlock(BlockPos bp, BlockSide side) {
        return serverPos.equalsBlockPos(bp);
    }

    @Nullable
    @Override
    public String getWikiPageName() {
        return "Server";
    }

}
