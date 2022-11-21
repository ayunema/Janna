package com.project610.commands;

public class AddSfx extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        instance.addReaction("sfx", message, channel);

        return 0;
    }
}
