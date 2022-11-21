package com.project610.commands;

public class AddSfxAlias extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        instance.addSfxAlias(message, channel);

        return 0;
    }
}
