package com.project610.commands;

import com.project610.Janna;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AddBingoSquare extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        String[] split = message.split(" ", 4);
        String name = "";
        String description = split.length == 4 ? split[3] : "";
        int difficulty = 1;
        try {
            difficulty = Integer.parseInt(split[2]);
        } catch (Exception ex) {
            malformed();
        }
        try {
            name = split[1];
            PreparedStatement prep = instance.sqlCon.prepareStatement("INSERT INTO bingo_square (name, difficulty, description) VALUES (?, ?, ?);");
            prep.setString(1, name);
            prep.setInt(2, difficulty);
            prep.setString(3, description);
            prep.executeUpdate();
        } catch (SQLException ex) {
            if (ex.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]")) {
                Janna.sendMessage(channel, "A bingo square for: `" + split[1] + "` already exists");
            } else {
                Janna.error("Failed to insert " + name + ": " + message.split(" ")[1], ex);
                Janna.sendMessage(channel, "Failed to add " + name + ": " + ex);
            }
            return 1;
        } catch (IndexOutOfBoundsException ex) {
            malformed();
            return 1;
        }

        return 0;
    }

    private void malformed() {
        Janna.warn("AddBingoSquare command malformed");
        Janna.sendMessage(channel, "Malformed command; Usage: `!addbingosquare <name> <difficulty> [description]");
    }
}
