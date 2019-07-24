/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.Control;
import net.montoyo.wd.client.gui.controls.Label;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.server.SMessageScreenCtrl;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.TypeData;
import net.montoyo.wd.utilities.Util;
import org.lwjgl.input.Keyboard;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class GuiKeyboard extends WDScreen {

    private static final String WARNING_FNAME = "wd_keyboard_warning.txt";

    private TileEntityScreen tes;
    private BlockSide side;
    private final ArrayList<TypeData> evStack = new ArrayList<>();
    private BlockPos kbPos;
    private boolean showWarning = true;

    @FillControl
    private Label lblInfo;

    @FillControl
    private Button btnOk;

    public GuiKeyboard() {
    }

    public GuiKeyboard(TileEntityScreen tes, BlockSide side, BlockPos kbPos) {
        this.tes = tes;
        this.side = side;
        this.kbPos = kbPos;
    }

    @Override
    protected void addLoadCustomVariables(Map<String, Double> vars) {
        vars.put("showWarning", showWarning ? 1.0 : 0.0);
    }

    @Override
    public void initGui() {
        super.initGui();

        if(mc.isIntegratedServerRunning() && mc.getIntegratedServer() != null && !mc.getIntegratedServer().getPublic())
            showWarning = false; //NO NEED
        else
            showWarning = !hasUserReadWarning();

        loadFrom(new ResourceLocation("webdisplays", "gui/keyboard.json"));

        if(showWarning) {
            int maxLabelW = 0;
            int totalH = 0;

            for(Control ctrl : controls) {
                if(ctrl != lblInfo && ctrl instanceof Label) {
                    if(ctrl.getWidth() > maxLabelW)
                        maxLabelW = ctrl.getWidth();

                    totalH += ctrl.getHeight();
                    ctrl.setPos((width - ctrl.getWidth()) / 2, 0);
                }
            }

            btnOk.setWidth(maxLabelW);
            btnOk.setPos((width - maxLabelW) / 2, 0);
            totalH += btnOk.getHeight();

            int y = (height - totalH) / 2;
            for(Control ctrl : controls) {
                if(ctrl != lblInfo) {
                    ctrl.setPos(ctrl.getX(), y);
                    y += ctrl.getHeight();
                }
            }
        } else {
            mc.inGameHasFocus = true;
            mc.mouseHelper.grabMouseCursor();
        }

        defaultBackground = showWarning;
        syncTicks = 5;
    }

    @Override
    public void handleInput() {
        if(showWarning) {
            try {
                super.handleInput();
            } catch(IOException ex) {
                Log.warningEx("Caught exception while handling screen input", ex);
            }

            return;
        }

        if(Keyboard.isCreated()) {
            while(Keyboard.next()) {
                if(Keyboard.getEventKey() == Keyboard.KEY_ESCAPE)
                    mc.displayGuiScreen(null);
                else {
                    char chr = Keyboard.getEventCharacter();

                    if(Keyboard.getEventKeyState()) {
                        int kc = Keyboard.getEventKey();

                        evStack.add(new TypeData(TypeData.Action.PRESS, kc, chr));
                        evStack.add(new TypeData(TypeData.Action.RELEASE, kc, chr));
                    }

                    if(chr != 0)
                        evStack.add(new TypeData(TypeData.Action.TYPE, 0, chr));
                }
            }

            if(!evStack.isEmpty() && !syncRequested())
                requestSync();
        }
    }

    @Override
    protected void sync() {
        if(!evStack.isEmpty()) {
            WebDisplays.NET_HANDLER.sendToServer(SMessageScreenCtrl.type(tes, side, WebDisplays.GSON.toJson(evStack), kbPos));
            evStack.clear();
        }
    }

    @GuiSubscribe
    public void onClick(Button.ClickEvent ev) {
        if(showWarning && ev.getSource() == btnOk) {
            writeUserAcknowledge();

            for(Control ctrl: controls) {
                if(ctrl instanceof Label) {
                    Label lbl = (Label) ctrl;
                    lbl.setVisible(!lbl.isVisible());
                }
            }

            btnOk.setDisabled(true);
            btnOk.setVisible(false);
            showWarning = false;
            defaultBackground = false;
            mc.inGameHasFocus = true;
            mc.mouseHelper.grabMouseCursor();
        }
    }

    private boolean hasUserReadWarning() {
        try {
            File f = new File(mc.mcDataDir, WARNING_FNAME);

            if(f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String str = br.readLine();
                Util.silentClose(br);

                return str != null && str.trim().equalsIgnoreCase("read");
            }
        } catch(Throwable t) {
            Log.warningEx("Can't know if user has already read the warning", t);
        }

        return false;
    }

    private void writeUserAcknowledge() {
        try {
            File f = new File(mc.mcDataDir, WARNING_FNAME);

            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write("read\n");
            Util.silentClose(bw);
        } catch(Throwable t) {
            Log.warningEx("Can't write that the user read the warning", t);
        }
    }

    @Override
    public boolean isForBlock(BlockPos bp, BlockSide side) {
        return bp.equals(kbPos) || (bp.equals(tes.getPos()) && side == this.side);
    }

}
