package com.project610.commands;

public class RemoveAlias extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        instance.removeAlias(message, channel);

        return 0;
    }
}
