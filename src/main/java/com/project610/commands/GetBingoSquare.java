package com.project610.commands;

import com.project610.Janna;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetBingoSquare extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        try {
            String[] split = message.split(" ");
            String name = split[1].toLowerCase();
            if (name.length() < 3) {
                Janna.sendMessage("Try searching for something longer than 3 characters");
                return 1;
            }
            PreparedStatement prep = instance.sqlCon.prepareStatement("SELECT * FROM bingo_square WHERE name LIKE ?;");
            prep.setString(1, "%" + name + "%");
            ResultSet result = prep.executeQuery();

            while (result.next()) {
                Janna.messageQueue.queueMessage(channel, "[" + result.getString("name") + "] difficulty=" + result.getInt("difficulty") + ", description=" + result.getString("description"));
            }

        } catch (SQLException ex) {
            // Meh for now
        }

        return 0;
    }
}
