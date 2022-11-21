package com.project610.commands;

public class RemoveResponse extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        instance.removeReaction("response", message, channel);

        return 0;
    }
}
