package com.project610.commands;

public class Unmute extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        String result = "";
        if (split.length == 1) {
            instance.sendMessage(channel, "Malformed command; Usage: !unmute <username>");
            return 1;
        } else {
            result = instance.unmuteUser(split[1]);
        }
        if (!result.isEmpty()) {
            instance.sendMessage(channel, result);
        }

        return 0;
    }
}
