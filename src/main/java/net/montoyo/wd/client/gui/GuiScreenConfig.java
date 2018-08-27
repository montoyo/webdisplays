/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.gui.controls.*;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.TileEntityScreen;
import net.montoyo.wd.item.WDItem;
import net.montoyo.wd.net.server.SMessageScreenCtrl;
import net.montoyo.wd.utilities.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class GuiScreenConfig extends WDScreen {

    //Screen data
    private final TileEntityScreen tes;
    private final BlockSide side;
    private NameUUIDPair owner;
    private NameUUIDPair[] friends;
    private int friendRights;
    private int otherRights;
    private Rotation rotation = Rotation.ROT_0;
    private float aspectRatio;

    //Autocomplete handling
    private boolean waitingAC;
    private int acFailTicks = -1;

    private final ArrayList<NameUUIDPair> acResults = new ArrayList<>();
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

    @FillControl
    private UpgradeGroup ugUpgrades;

    @FillControl
    private Button btnChangeRot;

    @FillControl
    private CheckBox cbLockRatio;

    @FillControl
    private CheckBox cbAutoVolume;

    private CheckBox[] friendBoxes;
    private CheckBox[] otherBoxes;

    public GuiScreenConfig(TileEntityScreen tes, BlockSide side, NameUUIDPair[] friends, int fr, int or) {
        this.tes = tes;
        this.side = side;
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
            owner = scr.owner;
            rotation = scr.rotation;

            tfResX.setText("" + scr.resolution.x);
            tfResY.setText("" + scr.resolution.y);
            aspectRatio = ((float) scr.resolution.x) / ((float) scr.resolution.y);

            //Hopefully upgrades have been synchronized...
            ugUpgrades.setUpgrades(scr.upgrades);
            cbAutoVolume.setChecked(scr.autoVolume);
        }

        if(owner == null)
            owner = new NameUUIDPair("???", UUID.randomUUID());

        lblOwner.setLabel(lblOwner.getLabel() + ' ' + owner.name);
        for(NameUUIDPair f : friends)
            lstFriends.addElementRaw(f.name, f);

        lstFriends.updateContent();
        updateRights(friendRights, friendRights, friendBoxes, true);
        updateRights(otherRights, otherRights, otherBoxes, true);
        updateMyRights();
        updateRotationStr();

        mc.getSoundHandler().playSound(PositionedSoundRecord.getRecord(WebDisplays.INSTANCE.soundScreenCfg, 1.0f, 1.0f));
    }

    private void updateRotationStr() {
        btnChangeRot.setLabel(I18n.format("webdisplays.gui.screencfg.rot" + rotation.getAngleAsInt()));
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
                WebDisplays.NET_HANDLER.sendToServer(SMessageScreenCtrl.vec2(tes, side, SMessageScreenCtrl.CTRL_SET_RESOLUTION, new Vector2i(x, y)));
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
        else if(ev.getSource() == btnChangeRot) {
            Rotation[] rots = Rotation.values();
            WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tes, side, rots[(rotation.ordinal() + 1) % rots.length]));
        }
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

            if(cbLockRatio.isChecked()) {
                if(ev.getSource() == tfResX) {
                    try {
                        float val = (float) Integer.parseInt(ev.getNewContent());
                        val /= aspectRatio;
                        tfResY.setText("" + ((int) val));
                    } catch(NumberFormatException ex) {}
                } else {
                    try {
                        float val = (float) Integer.parseInt(ev.getNewContent());
                        val *= aspectRatio;
                        tfResX.setText("" + ((int) val));
                    } catch(NumberFormatException ex) {}
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
        } else if(ev.getSource() == cbLockRatio && ev.isChecked()) {
            try {
                int x = Integer.parseInt(tfResX.getText());
                int y = Integer.parseInt(tfResY.getText());

                aspectRatio = ((float) x) / ((float) y);
            } catch(NumberFormatException ex) {
                cbLockRatio.setChecked(false);
            }
        } else if(ev.getSource() == cbAutoVolume)
            WebDisplays.NET_HANDLER.sendToServer(SMessageScreenCtrl.autoVol(tes, side, ev.isChecked()));
    }

    @GuiSubscribe
    public void onRemoveUpgrade(UpgradeGroup.ClickEvent ev) {
        WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tes, side, ev.getMouseOverStack()));
    }

    public boolean isFriendCheckbox(CheckBox cb) {
        return Arrays.stream(friendBoxes).anyMatch(fb -> cb == fb);
    }

    public boolean isOtherCheckbox(CheckBox cb) {
        return Arrays.stream(otherBoxes).anyMatch(ob -> cb == ob);
    }

    public boolean hasFriend(NameUUIDPair f) {
        return Arrays.stream(friends).anyMatch(f::equals);
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

    @Override
    protected void sync() {
        WebDisplays.NET_HANDLER.sendToServer(new SMessageScreenCtrl(tes, side, friendRights, otherRights));
        Log.info("Sent sync packet");
    }

    public void updateMyRights() {
        NameUUIDPair me = new NameUUIDPair(mc.player.getGameProfile());
        int myRights;
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
        btnChangeRot.setDisabled(flag);

        if(flag)
            btnSetRes.setDisabled(true);

        flag = (myRights & ScreenRights.MANAGE_UPGRADES) == 0;
        ugUpgrades.setDisabled(flag);
        cbAutoVolume.setDisabled(flag);
    }

    public void updateResolution(Vector2i res) {
        aspectRatio = ((float) res.x) / ((float) res.y);
        tfResX.setText("" + res.x);
        tfResY.setText("" + res.y);
        btnSetRes.setDisabled(true);
    }

    public void updateRotation(Rotation rot) {
        rotation = rot;
        updateRotationStr();
    }

    public void updateAutoVolume(boolean av) {
        cbAutoVolume.setChecked(av);
    }

    @Override
    public boolean isForBlock(BlockPos bp, BlockSide side) {
        return bp.equals(tes.getPos()) && side == this.side;
    }

    @Nullable
    @Override
    public String getWikiPageName() {
        ItemStack is = ugUpgrades.getMouseOverUpgrade();
        if(is != null) {
            if(is.getItem() instanceof WDItem)
                return ((WDItem) is.getItem()).getWikiName(is);
            else
                return null;
        }

        return "Screen_Configurator";
    }

}
