/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;

public enum VideoType {

    YOUTUBE("document.getElementById(\"movie_player\").setVolume(", ")"),
    YOUTUBE_EMBED("document.getElementsByClassName(\"html5-video-player\")[0].setVolume(", ")");

    private final String volumePrefix;
    private final String volumeSuffix;
    private final int volumeCap;

    VideoType(String prefix, String suffix) {
        volumePrefix = prefix;
        volumeSuffix = suffix;
        volumeCap = prefix.length() + 5 + suffix.length();
    }

    @Nullable
    public static VideoType getTypeFromURL(@Nonnull URL url) {
        String loHost = url.getHost().toLowerCase();
        if(loHost.equals("youtu.be"))
            return url.getPath().length() > 1 ? YOUTUBE : null;
        else if(!loHost.equals("www.youtube.com") && !loHost.equals("youtube.com"))
            return null;

        String loPath = url.getPath().toLowerCase();
        if(loPath.equals("/watch")) {
            if(url.getQuery() != null && (url.getQuery().startsWith("v=") || url.getQuery().contains("&v=")))
                return YOUTUBE;
        } else if(loPath.startsWith("/embed/"))
            return loPath.length() > 7 ? YOUTUBE_EMBED : null;

        return null;
    }

    @Nullable
    public static VideoType getTypeFromURL(@Nonnull String url) {
        try {
            return getTypeFromURL(new URL(url));
        } catch(MalformedURLException ex) {
            return null;
        }
    }

    @Nonnull
    public String getVideoIDFromURL(@Nonnull URL url) {
        if(this == YOUTUBE) {
            if(url.getHost().equalsIgnoreCase("youtu.be"))
                return url.getPath().substring(1);

            String args[] = url.getQuery().split("&");
            for(String arg : args) {
                if(arg.startsWith("v="))
                    return arg.substring(2);
            }
        } else if(this == YOUTUBE_EMBED)
            return url.getPath().substring(7);

        return "";
    }

    @Nonnull
    public String getURLFromID(@Nonnull String vid, boolean autoplay) {
        String format;
        if(this == YOUTUBE)
            format = autoplay ? "https://www.youtube.com/watch?v=%s&autoplay=1" : "https://www.youtube.com/watch?v=%s";
        else if(this == YOUTUBE_EMBED)
            format = autoplay ? "https://www.youtube.com/embed/%s?autoplay=1" : "https://www.youtube.com/embed/%s";
        else
            return "";

        return String.format(format, vid);
    }

    @Nonnull
    public String getVolumeJSQuery(int volInt, int volFrac) {
        return (new StringBuilder(volumeCap)).append(volumePrefix).append(volInt).append('.').append(volFrac).append(volumeSuffix).toString();
    }

}
