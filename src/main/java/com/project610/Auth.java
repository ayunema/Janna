package com.project610;

import java.sql.ResultSet;
import java.util.Arrays;

import static com.project610.Janna.*;

public class Auth {
    public static void getToken() {
        String token = "";
        try {

            // Try any tokens that are saved to see if we can avoid having to log in again
            ResultSet rs = Janna.instance.sqlCon.prepareStatement("SELECT * FROM auth ORDER BY id ASC").executeQuery();
            do  {
                try {
                    token = rs.getString(2);
                    // Try using Helix API; If no exception, we can use this token!
                    twitch.getHelix().getUsers(token, null, Arrays.asList(Janna.instance.appConfig.get("mainchannel"))).execute();
                    Janna.instance.setAuthToken(token);
                    return;
                } catch (Exception ex) {
                    warn("Missing or expired Helix Auth Token. Prompting to authenticate... ");
                    debug("Helix auth token exception: " + ex.toString());
                }
            } while (rs.next());

            // If no tokens saved, or all saved tokens seem invalid, load a browser to authenticate with Twitch
            WebBrowser wb = new WebBrowser("https://id.twitch.tv/oauth2/authorize?client_id=" + Creds._clientid +
                    "&redirect_uri=https://www.project610.com/janna/auth.html&response_type=token&scope=" +
                    "channel:manage:redemptions"
                    , false, false, new String[0]);
        }
        catch (Exception ex) {
            System.err.println("Screwed up auth: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
