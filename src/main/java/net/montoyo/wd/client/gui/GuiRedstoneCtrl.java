/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.util.ResourceLocation;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.TextField;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.net.SMessageRedstoneCtrl;
import net.montoyo.wd.utilities.Vector3i;

public class GuiRedstoneCtrl extends WDScreen {

    private int dimension;
    private Vector3i pos;
    private String risingEdgeURL;
    private String fallingEdgeURL;

    @FillControl
    private TextField tfRisingEdge;

    @FillControl
    private TextField tfFallingEdge;

    @FillControl
    private Button btnOk;

    public GuiRedstoneCtrl() {
    }

    public GuiRedstoneCtrl(int d, Vector3i p, String r, String f) {
        dimension = d;
        pos = p;
        risingEdgeURL = r;
        fallingEdgeURL = f;
    }

    @Override
    public void initGui() {
        super.initGui();
        loadFrom(new ResourceLocation("webdisplays", "gui/redstonectrl.json"));
        tfRisingEdge.setText(risingEdgeURL);
        tfFallingEdge.setText(fallingEdgeURL);
    }

    @GuiSubscribe
    public void onClick(Button.ClickEvent ev) {
        if(ev.getSource() == btnOk)
            WebDisplays.NET_HANDLER.sendToServer(new SMessageRedstoneCtrl(dimension, pos, tfRisingEdge.getText(), tfFallingEdge.getText()));

        mc.displayGuiScreen(null);
    }

}
