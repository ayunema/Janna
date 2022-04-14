package com.project610;

public class TTSMessage {
    public String type;
    public String text;
    public String sfxName;
    public String extra;

    public TTSMessage(String type, String text) {
        this.type = type;
        this.text = text;
    }

    public TTSMessage(String type, Sfx sfx, String sfxName) {
        this.type = type;
        this.text = sfx.url;
        this.extra = sfx.extra;
        this.sfxName = "";
    }
}
