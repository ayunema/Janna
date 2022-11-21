package com.project610.commands;

import com.project610.BingoSheet;
import com.project610.BingoSquare;
import com.project610.Janna;

import java.security.cert.CertificateRevokedException;
import java.sql.SQLException;

public class JoinBingo extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (Janna.bingoSheets.get(user.id) != null) {
            Janna.sendMessage(channel, "There's already a bingo sheet for: " + user.name);
            return 1;
        }

        BingoSheet sheet = new BingoSheet();
        if (!sheet.valid) {
            Janna.sendMessage(channel, "Couldn't generate bingo sheet, do you have enough squares?");
            return 1;
        }
        Janna.bingoSheets.put(user.id, sheet);

        // Save stuff
        String squaresValue = "";
        try {
            BingoSquare[][] squares = Janna.bingoSheets.get(user.id).squares;
            for (int x = 0; x < squares.length; x++) {
                for (int y = 0; y < squares[x].length; y++) {
                    squaresValue += squares[x][y].id + ",";
                }
            }
            instance.executeUpdate("INSERT INTO bingo_sheet (user_id, square_ids) VALUES ("+user.id + ", '" + squaresValue + "')");
        } catch (SQLException ex) {
            Janna.error("Error saving bingo sheet", ex);
        }

        return 0;
    }
}
