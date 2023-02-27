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

        String check = Bingo.check(Janna.bingoSheets.get(user.id));
        boolean win = check.charAt(0) == 'Y';
        check = check.substring(1);

        if (win) {
            Janna.sendMessage(channel, user.name + " has won bingo!");
        }

        Janna.messageQueue.queueMessage(channel, user.name + " bingo: ");
        for (String msg : check.split("\n")) {
            Janna.messageQueue.queueMessage(channel, msg);
        }


        return 0;
    }
}
