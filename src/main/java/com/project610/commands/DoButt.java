package com.project610.commands;

public class DoButt extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (instance.setUserPref(user, "butt_stuff", "1")) {
            instance.twitch.getChat().sendMessage(channel, "Can't get enough of that butt.");
        }

        return 0;
    }
}
