/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.util.ResourceLocation;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.TextField;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.SMessagePadCtrl;
import net.montoyo.wd.net.SMessageScreenCtrl;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;

import java.util.Map;

public class GuiSetURL2 extends WDScreen {

    //Screen data
    private TileEntityScreen tileEntity;
    private BlockSide screenSide;

    //Pad data
    private boolean isPad;

    //Common
    private String screenURL;

    @FillControl
    private TextField tfURL;

    @FillControl
    private Button btnShutDown;

    @FillControl
    private Button btnCancel;

    @FillControl
    private Button btnOk;

    @FillControl
    private Button btnYT;

    public GuiSetURL2(TileEntityScreen tes, BlockSide side, String url) {
        tileEntity = tes;
        screenSide = side;
        screenURL = url;
        isPad = false;
    }

    public GuiSetURL2(String url) {
        screenURL = url;
        isPad = true;
    }

    @Override
    public void initGui() {
        super.initGui();
        loadFrom(new ResourceLocation("webdisplays", "gui/seturl.json"));
        tfURL.setText(screenURL);
    }

    @Override
    protected void addLoadCustomVariables(Map<String, Double> vars) {
        vars.put("isPad", isPad ? 1.0 : 0.0);
    }

    @GuiSubscribe
    public void onButtonClicked(Button.ClickEvent ev) {
        if(ev.getSource() == btnCancel)
            mc.displayGuiScreen(null);
        else if(ev.getSource() == btnOk)
            validate(tfURL.getText());
        else if(ev.getSource() == btnShutDown) {
            if(isPad)
                WebDisplays.NET_HANDLER.sendToServer(new SMessagePadCtrl(""));

            mc.displayGuiScreen(null);
        } else
            Log.info("GuiSetURL2: TODO"); //TODO
    }

    @GuiSubscribe
    public void onEnterPressed(TextField.EnterPressedEvent ev) {
        validate(ev.getText());
    }

    private void validate(String url) {
        if(!url.isEmpty()) {
            if(isPad)
                WebDisplays.NET_HANDLER.sendToServer(new SMessagePadCtrl(url));
            else
                WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tileEntity, screenSide, url));
        }

        mc.displayGuiScreen(null);
    }

}
