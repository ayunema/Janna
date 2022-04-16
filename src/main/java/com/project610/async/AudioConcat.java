package com.project610.async;

import com.project610.Audio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;

public class AudioConcat implements Runnable {
    LinkedHashMap<String, String> files;
    String username;

    public AudioConcat(String username, LinkedHashMap<String, String> files) {
        this.username = username;
        this.files = files;
    }

    public void run() {
        Audio.concat(username, files);
    }
}
