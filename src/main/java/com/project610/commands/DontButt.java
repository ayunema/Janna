package com.project610.commands;

public class DontButt extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (instance.setUserPref(user, "butt_stuff", "0")) {
            instance.twitch.getChat().sendMessage(channel, "Okay, I won't butt you, bro.");
        }

        return 0;
    }
}
