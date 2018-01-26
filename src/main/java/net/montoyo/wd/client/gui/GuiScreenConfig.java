/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.util.ResourceLocation;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.controls.*;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.net.SMessageScreenCtrl;
import net.montoyo.wd.utilities.*;

import java.util.ArrayList;
import java.util.Arrays;

public class GuiScreenConfig extends WDScreen {

    //Screen data
    private TileEntityScreen tes;
    private BlockSide side;
    private NameUUIDPair owner;
    private NameUUIDPair[] friends;
    private int friendRights;
    private int otherRights;

    //Autocomplete handling
    private boolean waitingAC;
    private int acFailTicks = -1;

    private ArrayList<NameUUIDPair> acResults = new ArrayList<NameUUIDPair>();
    private boolean adding;

    //Controls
    @FillControl
    private Label lblOwner;

    @FillControl
    private List lstFriends;

    @FillControl
    private Button btnAdd;

    @FillControl
    private TextField tfFriend;

    @FillControl
    private TextField tfResX;

    @FillControl
    private TextField tfResY;

    @FillControl
    private ControlGroup grpFriends;

    @FillControl
    private ControlGroup grpOthers;

    @FillControl
    private CheckBox boxFSetUrl;

    @FillControl
    private CheckBox boxFClick;

    @FillControl
    private CheckBox boxFFriends;

    @FillControl
    private CheckBox boxFOthers;

    @FillControl
    private CheckBox boxFUpgrades;

    @FillControl
    private CheckBox boxFResolution;

    @FillControl
    private CheckBox boxOSetUrl;

    @FillControl
    private CheckBox boxOClick;

    @FillControl
    private CheckBox boxOUpgrades;

    @FillControl
    private CheckBox boxOResolution;

    @FillControl
    private Button btnSetRes;

    private CheckBox[] friendBoxes;
    private CheckBox[] otherBoxes;

    public GuiScreenConfig(TileEntityScreen tes, BlockSide side, NameUUIDPair owner, NameUUIDPair[] friends, int fr, int or) {
        this.tes = tes;
        this.side = side;
        this.owner = owner;
        this.friends = friends;
        friendRights = fr;
        otherRights = or;
    }

    @Override
    public void initGui() {
        super.initGui();
        loadFrom(new ResourceLocation("webdisplays", "gui/screencfg.json"));

        friendBoxes = new CheckBox[] { boxFResolution, boxFUpgrades, boxFOthers, boxFFriends, boxFClick, boxFSetUrl };
        boxFResolution.setUserdata(ScreenRights.CHANGE_RESOLUTION);
        boxFUpgrades.setUserdata(ScreenRights.MANAGE_UPGRADES);
        boxFOthers.setUserdata(ScreenRights.MANAGE_OTHER_RIGHTS);
        boxFFriends.setUserdata(ScreenRights.MANAGE_FRIEND_LIST);
        boxFClick.setUserdata(ScreenRights.CLICK);
        boxFSetUrl.setUserdata(ScreenRights.CHANGE_URL);

        otherBoxes = new CheckBox[] { boxOResolution, boxOUpgrades, boxOClick, boxOSetUrl };
        boxOResolution.setUserdata(ScreenRights.CHANGE_RESOLUTION);
        boxOUpgrades.setUserdata(ScreenRights.MANAGE_UPGRADES);
        boxOClick.setUserdata(ScreenRights.CLICK);
        boxOSetUrl.setUserdata(ScreenRights.CHANGE_URL);

        TileEntityScreen.Screen scr = tes.getScreen(side);
        if(scr != null) {
            tfResX.setText("" + scr.resolution.x);
            tfResY.setText("" + scr.resolution.y);
        }

        lblOwner.setLabel(lblOwner.getLabel() + owner.name);
        for(NameUUIDPair f : friends)
            lstFriends.addElementRaw(f.name, f);

        lstFriends.updateContent();
        updateRights(friendRights, friendRights, friendBoxes, true);
        updateRights(otherRights, otherRights, otherBoxes, true);
        updateMyRights();
    }

    private void addFriend(String name) {
        if(!name.isEmpty()) {
            requestAutocomplete(name, true);
            tfFriend.setDisabled(true);
            adding = true;
            waitingAC = true;
        }
    }

    private void clickSetRes() {
        TileEntityScreen.Screen scr = tes.getScreen(side);
        if(scr == null)
            return; //WHATDAFUQ?

        try {
            int x = Integer.parseInt(tfResX.getText());
            int y = Integer.parseInt(tfResY.getText());
            if(x < 1 || y < 1)
                throw new NumberFormatException(); //I'm lazy

            if(x != scr.resolution.x || y != scr.resolution.y)
                WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tes, side, new Vector2i(x, y)));
        } catch(NumberFormatException ex) {
            //Roll back
            tfResX.setText("" + scr.resolution.x);
            tfResY.setText("" + scr.resolution.y);
        }

        btnSetRes.setDisabled(true);
    }

    @GuiSubscribe
    public void onClick(Button.ClickEvent ev) {
        if(ev.getSource() == btnAdd && !waitingAC)
            addFriend(tfFriend.getText().trim());
        else if(ev.getSource() == btnSetRes)
            clickSetRes();
    }

    @GuiSubscribe
    public void onEnterPressed(TextField.EnterPressedEvent ev) {
        if(ev.getSource() == tfFriend && !waitingAC)
            addFriend(ev.getText().trim());
        else if((ev.getSource() == tfResX || ev.getSource() == tfResY) && !btnSetRes.isDisabled())
            clickSetRes();
    }

    @GuiSubscribe
    public void onAutocomplete(TextField.TabPressedEvent ev) {
        if(ev.getSource() == tfFriend && !waitingAC && !ev.getBeginning().isEmpty()) {
            if(acResults.isEmpty()) {
                waitingAC = true;
                requestAutocomplete(ev.getBeginning(), false);
            } else {
                NameUUIDPair pair = acResults.remove(0);
                tfFriend.setText(pair.name);
            }
        } else if(ev.getSource() == tfResX) {
            tfResX.setFocused(false);
            tfResY.focus();
            tfResY.getMcField().setCursorPositionZero();
            tfResY.getMcField().setSelectionPos(tfResY.getText().length());
        }
    }

    @GuiSubscribe
    public void onTextChanged(TextField.TextChangedEvent ev) {
        if(ev.getSource() == tfResX || ev.getSource() == tfResY) {
            for(int i = 0; i < ev.getNewContent().length(); i++) {
                if(!Character.isDigit(ev.getNewContent().charAt(i))) {
                    ev.getSource().setText(ev.getOldContent());
                    return;
                }
            }

            btnSetRes.setDisabled(false);
        }
    }

    @GuiSubscribe
    public void onRemovePlayer(List.EntryClick ev) {
        if(ev.getSource() == lstFriends)
            WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tes, side, (NameUUIDPair) ev.getUserdata(), true));
    }

    @GuiSubscribe
    public void onCheckboxChanged(CheckBox.CheckedEvent ev) {
        if(isFriendCheckbox(ev.getSource())) {
            int flag = (Integer) ev.getSource().getUserdata();
            if(ev.isChecked())
                friendRights |= flag;
            else
                friendRights &= ~flag;

            requestSync();
        } else if(isOtherCheckbox(ev.getSource())) {
            int flag = (Integer) ev.getSource().getUserdata();
            if(ev.isChecked())
                otherRights |= flag;
            else
                otherRights &= ~flag;

            requestSync();
        }
    }

    public boolean isFriendCheckbox(CheckBox cb) {
        for(CheckBox box : friendBoxes) {
            if(box == cb)
                return true;
        }

        return false;
    }

    public boolean isOtherCheckbox(CheckBox cb) {
        for(CheckBox box : otherBoxes) {
            if(box == cb)
                return true;
        }

        return false;
    }

    public boolean hasFriend(NameUUIDPair f) {
        for(NameUUIDPair pair : friends) {
            if(pair.equals(f))
                return true;
        }

        return false;
    }

    @Override
    public void onAutocompleteResult(NameUUIDPair pairs[]) {
        waitingAC = false;

        if(adding) {
            if(!hasFriend(pairs[0]))
                WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tes, side, pairs[0], false));

            tfFriend.setDisabled(false);
            tfFriend.clear();
            tfFriend.focus();
            adding = false;
        } else {
            acResults.clear();
            acResults.addAll(Arrays.asList(pairs));

            NameUUIDPair pair = acResults.remove(0);
            tfFriend.setText(pair.name);
        }
    }

    @Override
    public void onAutocompleteFailure() {
        waitingAC = false;
        acResults.clear();
        acFailTicks = 0;
        tfFriend.setTextColor(Control.COLOR_RED);

        if(adding) {
            tfFriend.setDisabled(false);
            adding = false;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        if(acFailTicks >= 0) {
            if(++acFailTicks >= 10) {
                acFailTicks = -1;
                tfFriend.setTextColor(TextField.DEFAULT_TEXT_COLOR);
            }
        }
    }

    public void updateFriends(NameUUIDPair[] friends) {
        boolean diff = false;
        if(friends.length != this.friends.length)
            diff = true;
        else {
            for(NameUUIDPair pair : friends) {
                if(!hasFriend(pair)) {
                    diff = true;
                    break;
                }
            }
        }

        if(diff) {
            this.friends = friends;
            lstFriends.clearRaw();
            for(NameUUIDPair pair : friends)
                lstFriends.addElementRaw(pair.name, pair);

            lstFriends.updateContent();
        }
    }

    private int updateRights(int current, int newVal, CheckBox[] boxes, boolean force) {
        if(force || current != newVal) {
            for(CheckBox box : boxes) {
                int flag = (Integer) box.getUserdata();
                box.setChecked((newVal & flag) != 0);
            }

            if(!force) {
                Log.info("Screen check boxes were updated");
                abortSync(); //Value changed by another user, abort modifications by local user
            }
        }

        return newVal;
    }

    public void updateFriendRights(int rights) {
        friendRights = updateRights(friendRights, rights, friendBoxes, false);
    }

    public void updateOtherRights(int rights) {
        otherRights = updateRights(otherRights, rights, otherBoxes, false);
    }

    public boolean isScreen(Vector3i pos, BlockSide side) {
        return pos.x == tes.getPos().getX() && pos.y == tes.getPos().getY() && pos.z == tes.getPos().getZ() && side == this.side;
    }

    @Override
    protected void sync() {
        WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tes, side, friendRights, otherRights));
        Log.info("Sent sync packet");
    }

    public void updateMyRights() {
        NameUUIDPair me = new NameUUIDPair(mc.player.getGameProfile());
        int myRights = 0;
        boolean clientIsOwner = false;

        if(me.equals(owner)) {
            myRights = ScreenRights.ALL;
            clientIsOwner = true;
        } else if(hasFriend(me))
            myRights = friendRights;
        else
            myRights = otherRights;

        //Disable components according to client rights
        grpFriends.setDisabled(!clientIsOwner);

        boolean flag = (myRights & ScreenRights.MANAGE_FRIEND_LIST) == 0;
        lstFriends.setDisabled(flag);
        tfFriend.setDisabled(flag);
        btnAdd.setDisabled(flag);

        flag = (myRights & ScreenRights.MANAGE_OTHER_RIGHTS) == 0;
        grpOthers.setDisabled(flag);

        flag = (myRights & ScreenRights.CHANGE_RESOLUTION) == 0;
        tfResX.setDisabled(flag);
        tfResY.setDisabled(flag);

        if(flag)
            btnSetRes.setDisabled(true);
    }

    public void updateResolution(Vector2i res) {
        tfResX.setText("" + res.x);
        tfResY.setText("" + res.y);
        btnSetRes.setDisabled(true);
    }

}
