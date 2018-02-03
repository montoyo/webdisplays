/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.VideoType;

import java.net.MalformedURLException;
import java.net.URL;

public class YTButton extends Button implements TextField.TextChangeListener {

    private TextField urlField;

    public YTButton() {
        btn.displayString = "YT";
        btn.enabled = false;
        shiftColor = 0xFFFF6464;
    }

    @Override
    protected boolean onClick() {
        if(urlField != null) {
            String urlStr = Util.addProtocol(urlField.getText());
            URL url;

            try {
                url = new URL(urlStr);
            } catch(MalformedURLException ex) {
                return true;
            }

            VideoType vt = VideoType.getTypeFromURL(url);
            if(vt == VideoType.YOUTUBE)
                urlField.setText(VideoType.YOUTUBE_EMBED.getURLFromID(vt.getVideoIDFromURL(url), shiftDown));
        }

        return true;
    }

    public void setURLField(TextField tf) {
        if(urlField != null)
            tf.removeTextChangeListener(this);

        urlField = tf;

        if(urlField != null)
            tf.addTextChangeListener(this);
    }

    public TextField getURLField() {
        return urlField;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);

        String tfName = json.getString("urlField", null);
        if(tfName != null) {
            Control ctrl = parent.getControlByName(tfName);

            if(ctrl != null && ctrl instanceof TextField) {
                urlField = (TextField) ctrl;
                urlField.addTextChangeListener(this);
            }
        }
    }

    @Override
    public void onTextChange(TextField tf, String oldContent, String newContent) {
        btn.enabled = (VideoType.getTypeFromURL(Util.addProtocol(newContent)) == VideoType.YOUTUBE);
    }

}
