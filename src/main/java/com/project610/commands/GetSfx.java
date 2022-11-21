package com.project610.commands;

public class GetSfx extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        instance.getReaction("sfx", message, channel);

        return 0;
    }
}
