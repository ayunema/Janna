package com.project610.commands;

public class GetVoice extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        instance.sendMessage(channel, "@" + user.name + ", Your current voice is: " + user.voiceName +
                " (Speed: " + user.voiceSpeed + " (0.75 ~ 4.0), Pitch: " + user.voicePitch + " (-20 ~ 20), Freebies: " + user.freeVoice);

        return 0;
    }
}
