package com.project610.commands;

public class Mute extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        String result = "";
        if (split.length == 1) {
            instance.sendMessage(channel, "Malformed command; Usage: !mute <username>"/* [duration]"*/);
            return 1;
        } else if (split.length == 2) {
            result = instance.muteUser(split[1], "-1");
        } else {
            result = instance.muteUser(split[1], split[2]);
        }
        instance.sendMessage(channel, result);

        return 0;
    }
}
