package com.project610.commands;

public class No extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isVIP(user.name)) {
            if (!instance.speechQueue.getCurrentSpeakers(null).contains(user.name)) return 1;
            instance.silenceCurrentVoices(user.name);
        } else {
            instance.silenceCurrentVoices();
        }

        return 0;
    }
}
