package com.project610.commands;

public class ReplaceSfx extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

                if (!instance.isMod(user.name)) return 1;
                instance.editReaction("sfx", message, channel);

        return 0;
    }
}
