package com.project610;

import java.util.HashMap;

public class User {
    public int id;
    public String name;
    public String voiceName;
    public double voiceSpeed;
    public double voicePitch;
    public double voiceVolume;
    public int freeVoice;
    Janna janna;
    HashMap<String, String> prefs;

    public User (Janna janna, int id, String name, String voiceName, double voiceSpeed, double voicePitch, double voiceVolume, int freeVoice) {
        this.id = id;
        this.name = name;
        this.voiceName = voiceName;
        this.voiceSpeed = voiceSpeed;
        this.voicePitch = voicePitch;
        this.janna = janna; // Dunno why I did this instead of making a static instance to start, or just
                            //  doing static everything. TODO: Clean this up eventually
        this.voiceVolume = voiceVolume;
        this.freeVoice = freeVoice;

        prefs = getPrefs();
    }

    public void save() {
        janna.saveUser(this);
    }

    public HashMap<String, String> getPrefs() {
        return janna.getUserPrefs(this);
    }
}
