package com.project610.commands;

public class Stfu extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isVIP(user.name)) return 1;
        if (split.length > 1) {
            instance.silenceAllVoices(split[1]);
        } else {
            instance.silenceAllVoices();
        }

        return 0;
    }
}
