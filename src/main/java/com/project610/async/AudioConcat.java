package com.project610.async;

import com.project610.Audio;

import java.util.ArrayList;

public class AudioConcat implements Runnable {
    ArrayList<String> files;
    String username;

    public AudioConcat(String username, ArrayList<String> files) {
        this.username = username;
        this.files = files;
    }

    public void run() {
        Audio.concat(username, files);
    }
}
