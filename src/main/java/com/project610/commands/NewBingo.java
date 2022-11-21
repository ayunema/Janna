package com.project610.commands;

import com.project610.BingoSquare;
import com.project610.Janna;

import java.sql.SQLException;

public class NewBingo extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;
        try {
            instance.executeUpdate("DELETE FROM bingo_sheet");
            instance.executeUpdate("UPDATE bingo_square SET state = 0");
            Janna.bingoSheets.clear();
            for (BingoSquare square : Janna.bingoSquares.values()) {
                square.state = 0;
            }
            Janna.sendMessage(channel, "New bingo started! Type !joinbingo to get a sheet");
        } catch (SQLException ex) {
            Janna.error("Failed to clear old bingo sheets", ex);
        }

        return 0;
    }
}
