package com.project610.commands;

public class RemoveSfx extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        instance.removeReaction("sfx", message, channel);

        return 0;
    }
}
