package com.project610.commands;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RemoveBingoSquare extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if (!instance.isMod(user.name)) return 1;

        String name = split[1];

        try {
            PreparedStatement prep = instance.sqlCon.prepareStatement("DELETE FROM bingo_square WHERE name LIKE ?;");
            prep.setString(1, name);
            if (prep.executeUpdate() > 0) {
                instance.sendMessage(channel, "Removed bingo square: " + name);
            }
        }
        catch (SQLException ex) {
            instance.sendMessage(channel, "Failed to remove bingo square: " + ex);
            return 1;
        }
        catch (IndexOutOfBoundsException ex) {
            instance.sendMessage(channel, "Malformed command; Usage: !removebingosquare <name>");
            return 1;
        }

        return 0;
    }
}
