package com.project610.commands;

public class ModSfx extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        instance.modReaction("sfx", message, channel);

        return 0;
    }
}
