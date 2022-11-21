package com.project610.commands;

import com.project610.Bingo;
import com.project610.Janna;

public class BingoCheck extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (Janna.bingoSheets.get(user.id) == null) {
            Janna.sendMessage(channel, "No bingo sheet found for: " + user.name);
            return 1;
        }

        Janna.sendMessage(channel, user.name + " has " + (Bingo.check(Janna.bingoSheets.get(user.id)) ? "" : "not ") + "won bingo!");

        return 0;
    }
}
