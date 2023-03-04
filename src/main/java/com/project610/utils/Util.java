package com.project610.utils;

import com.github.twitch4j.helix.domain.Clip;
import com.project610.Creds;
import com.project610.Janna;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.project610.Janna.*;

public class Util {
    public enum OS {
        WINDOWS, LINUX, MAC
    }

    private static OS os = null;

    public static OS getOS() {
        if (os == null) {
            String operSys = System.getProperty("os.name").toLowerCase();
            if (operSys.contains("win")) {
                os = OS.WINDOWS;
            } else if (operSys.contains("nix") || operSys.contains("nux")
                    || operSys.contains("aix")) {
                os = OS.LINUX;
            } else if (operSys.contains("mac")) {
                os = OS.MAC;
            }
        }
        return os;
    }

    public static String currentTime() {
        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return Instant.now().toString();
    }

    public static String[] parseArgs(String s) {
        Pattern pat = Pattern.compile("(\\S*\\\".*?\\\"|\\S+)");
        Matcher matcher = pat.matcher(s);

        ArrayList<String> args = new ArrayList<>();
        while (matcher.find()) {
            String group = matcher.group();
            if (group.chars().filter(x -> x == '"').count() >= 2) {
                group = replaceCharAt(group, group.indexOf('"'), "");
                group = replaceCharAt(group, group.lastIndexOf('"'), "");
            }
            args.add(group);
        }

        return args.toArray(new String[0]);
    }

    public static String replaceCharAt(String s, int pos, String replace) {
        return s.substring(0, pos) + s.substring(pos+1);
    }

    public static String getGameById(String gameId) {
        String game = "";
        try {
            PreparedStatement prep = Janna.instance.sqlCon.prepareStatement("SELECT name FROM twitch_games WHERE id = ?");
            prep.setString(1, gameId);
            ResultSet results = prep.executeQuery();

            if (results.next()) {
                game = results.getString("name");
                trace("Pulled game title from cache; gameId: " + gameId + "=" + game);
                return game;
            }
        } catch (SQLException ex) {
            error("Error pulling game title from SQL cache", ex);
        }

        game = twitch.getHelix().getGames(Creds._helixtoken, Arrays.asList(gameId), null, null).execute().getGames().get(0).getName();
        try {
            PreparedStatement prep = Janna.instance.sqlCon.prepareStatement("INSERT INTO twitch_games (id, name) VALUES (?, ?);");
            prep.setString(1, gameId);
            prep.setString(2, game);
            if (prep.executeUpdate() > 0) {
                trace("Cached gameId: " + gameId + "=" + game);
            } else {
                trace("Tried and failed to cache gameId: " + gameId + "=" + game);
            }
        } catch (SQLException ex) {
            error("Failed to cache game name from game ID", ex);
        }

        return game;
    }
}