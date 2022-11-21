package com.project610.commands;

import com.project610.BingoSquare;
import com.project610.Janna;

import java.util.Locale;

public class BingoSquares extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        String find = (split.length > 1) ? split[1].toLowerCase() : "";
        String squaresMessage = "Bingo squares: ";
        String bingoString = "";
        for (BingoSquare square : Janna.bingoSquares.values()) {
            if (square.name.toLowerCase().contains(find) || square.description.toLowerCase().contains(find)) {
                bingoString += (bingoString.equalsIgnoreCase("")) ? square.name : ", " + square.name;
            }
        }
        Janna.messageQueue.queueLongMessage(channel, squaresMessage, bingoString);

        return 0;
    }
}
