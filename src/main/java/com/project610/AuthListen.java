package com.project610;

import java.sql.ResultSet;
import java.util.Arrays;

public class AuthListen {
    public static void getToken() {
        String token = "";
        try {

            ResultSet rs = Janna.instance.sqlCon.prepareStatement("SELECT * FROM auth ORDER BY id ASC").executeQuery();

            do  {
                try {
                    token = rs.getString(2);
                    System.out.println("Existing token: " + token);
                    Janna.instance.mainchannel_user = Janna.instance.twitch.getHelix().getUsers(token, null, Arrays.asList(Janna.mainchannel)).execute().getUsers().get(0);
                    System.out.println(Janna.instance.twitch.getHelix().getCustomRewards(token, Janna.mainchannel_user.getId(), null, true).execute().toString());
                    // If no exception, we can use this token!
                    Janna.instance.setAuthToken(token);
                    return;
                } catch (Exception ex) {
                    Janna.warn("Missing or expired Helix Auth Token. Prompting to authenticate... ");
                    Janna.debug("Helix auth token exception: " + ex.toString());
                }
            } while (rs.next());



            WebBrowser wb = new WebBrowser("https://id.twitch.tv/oauth2/authorize?client_id=" + Creds._clientid +
                    "&redirect_uri=https://www.project610.com/janna/auth.html&response_type=token&scope=" +
                    "channel:manage:redemptions"
                    , false, false, new String[0]);
            System.out.println("address: " + wb.getAddress());


        }
        catch (Exception ex) {
            System.err.println("Screwed up auth: " + ex.toString());
            ex.printStackTrace();
        }
    }
}
