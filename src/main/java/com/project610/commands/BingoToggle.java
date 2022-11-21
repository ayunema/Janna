package com.project610.commands;

import com.project610.Janna;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BingoToggle extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        String name = split[1];

        try {
            PreparedStatement prep = instance.sqlCon.prepareStatement("SELECT name, state FROM bingo_square WHERE name like ?");
            prep.setString(1, name);
            ResultSet result = prep.executeQuery();
            name = result.getString("name");
            int state = result.getInt("state");


            prep = instance.sqlCon.prepareStatement("UPDATE bingo_square SET state = ? WHERE name LIKE ?;");
            prep.setInt(1, (++state)%2);
            prep.setString(2, name);
            if (prep.executeUpdate() > 0) {
                Janna.sendMessage(channel, "Toggled square: " + name + "=" + (state == 1 ? "Yes" : "No"));
            }
        }
        catch (SQLException ex) {
            Janna.sendMessage(channel, "Failed to toggle bingo square: " + ex);
            return 1;
        }
        catch (IndexOutOfBoundsException ex) {
            Janna.sendMessage(channel, "Malformed command; Usage: !bingotoggle <name>");
            return 1;
        }

        return 0;
    }
}
