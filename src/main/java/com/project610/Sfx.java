package com.project610;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public class Sfx {
    public String url;
    public String extra;
    public String created;
    public HashMap<String, String> mods;
    public TreeSet<String> aliases;
    public long uses;

    public Sfx(String url, HashMap<String, String> mods, String created, long uses) {
        this.url = url;
        this.created = created;
        this.mods = mods;
        this.uses = uses;
        if (null == mods) {
            mods = new HashMap<>();
        }
        String temp = "";
        for (String key : mods.keySet()) {
            if (key.equalsIgnoreCase("volume")) {
                temp += " " + parseParam(key + "=" + mods.get(key));
            }
        }
        this.mods = mods;
        this.extra = temp;
        this.aliases = new TreeSet<>();
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
