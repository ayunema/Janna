package com.project610;

import java.util.HashMap;

public class Sfx {
    public String url;
    public String extra;

    public Sfx (String url, String extra) {
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
        String key = split[0], value = (split.length > 1 ? "=\"" + split[1] + "\"" : "");
        if (key.equalsIgnoreCase("volume") || key.equalsIgnoreCase("soundlevel")) {
            key = "soundLevel";
            try {
                value = value.replaceFirst("(?i)db", "dB");
                // Set a reasonable maximum (+15dB)
                if (value.matches("(?i)=\\\"\\+\\d+db\\\"")) {
                    try {
                        double db = Double.parseDouble(value.substring(value.indexOf("+")+1, value.indexOf("dB")));
                        if (db > 15) {
                            value = "=\"+15dB\"";
                        }
                    } catch (NumberFormatException ex) {
                        value = "";
                    }
                }
            } catch (Exception ex) { }
        }
        return key+value;
    }
}
