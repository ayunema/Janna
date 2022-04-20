package com.project610;

import java.util.HashMap;

public class Sfx {
    public String url;
    public String extra;

    public Sfx(String url, String extra) {
        this.url = url;
        String temp = "";
        for (String param : extra.split(",")) {
            if (!param.trim().isEmpty()) {
                temp += " " + parseParam(param);
            }
        }
        this.extra = temp;
    }

    public static String parseParam(String s) {
        String[] split = s.split("=", 2);
        String key = split[0], value = (split.length > 1 ? split[1] : "");
        if (key.equalsIgnoreCase("volume") || key.equalsIgnoreCase("soundlevel")) {
            value = value.replaceAll("(?i)db", "");
            if (Janna.ttsMode.equalsIgnoreCase("google")) {
                key = "soundLevel";
            } else if (Janna.ttsMode.equalsIgnoreCase("se")) {
                key = ",volume=";
            }
            try {
                if (key.equalsIgnoreCase("soundLevel")) {
                    // Set a reasonable maximum (+20dB)
                    try {
                        double db = Double.parseDouble(value);
                        if (db > 20) {
                            value = "20";
                        }
                    } catch (NumberFormatException ex) {
                        value = "";
                    }
                    value = "=\"" + value + "dB\"";
                } else if (key.equalsIgnoreCase(",volume=")) {
                    try {
                        double db = Double.parseDouble(value);
                        if (db > 20) {
                            value = "20";
                        }
                    } catch (NumberFormatException ex) {
                        value = "";
                    }
                    value = value + "dB";
                }
            } catch (Exception ex) {
            }
        }
        return key + value;
    }

    public static String getFileLocation(Sfx sfx) {
        return getFileLocation(sfx.url);
    }

    public static String getFileLocation(String url) {
        return "sfx/" + url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf(".")) + Audio.desiredExt;
    }
}
