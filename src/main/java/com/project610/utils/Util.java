package com.project610.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class Util {
    public enum OS {
        WINDOWS, LINUX, MAC
    }

    private static OS os = null;

    public static OS getOS() {
        if (os == null) {
            String operSys = System.getProperty("os.name").toLowerCase();
            if (operSys.contains("win")) {
                os = OS.WINDOWS;
            } else if (operSys.contains("nix") || operSys.contains("nux")
                    || operSys.contains("aix")) {
                os = OS.LINUX;
            } else if (operSys.contains("mac")) {
                os = OS.MAC;
            }
        }
        return os;
    }

    public static String currentTime() {
        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return Instant.now().toString();
    }
}