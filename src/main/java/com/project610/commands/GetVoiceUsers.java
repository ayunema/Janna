package com.project610.commands;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GetVoiceUsers extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        try {
            PreparedStatement prep = instance.sqlCon.prepareStatement("SELECT COUNT(*) FROM user WHERE voicename LIKE ?;");
            prep.setString(1, split[1]);
            ResultSet result = prep.executeQuery();
            int count = result.getInt(1);
            String are_is = (count == 1 ? "There is " + count + " person" : "There are " + count + " people");
            instance.sendMessage(channel, are_is + " using the voice: " + split[1]);
        } catch (SQLException ex) {
            // TODO: Eh
            return 1;
        }

        return 0;
    }
}
