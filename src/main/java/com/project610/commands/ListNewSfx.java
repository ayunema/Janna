package com.project610.commands;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ListNewSfx extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        String sfxString = "";
        String sfxMessage = "Some new SFX (!sfx for full list)";
        sfxMessage+= ": ";

        int count = 10;
        if (split.length > 1) {
            try {
                count = Integer.parseInt(split[1]);
                if (count > 25) count = 25;
                else if (count < 1) count = 1;
            } catch (NumberFormatException ex) {
                // Meh
            }
        }

        int limit = 420; // Truncate if too long

        try {
            ResultSet results = instance.executeQuery("SELECT * FROM reaction WHERE type = 'sfx' ORDER BY created_timestamp DESC LIMIT " + count + ";");
            while (results.next()) {
                sfxString += (sfxString.isEmpty() ? "" : ", ") + results.getString("phrase");
            }
        } catch (SQLException ex) {
            // This shouldn't happen
            instance.error("Error getting new SFX", ex);
        }
        if (sfxString.length() > limit) {
            sfxString = sfxString.substring(0, limit) + "...";
        }
        instance.sendMessage(channel, sfxMessage + sfxString);

        return 0;
    }
}
