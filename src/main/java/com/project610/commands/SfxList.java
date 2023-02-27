package com.project610.commands;

import com.project610.Janna;
import com.project610.SeVoice;

import java.util.ArrayList;
import java.util.HashMap;

public class SfxList extends Command {

    @Override
    public Object apply(Object o) {
        super.apply(o);

        if ("1".equals(instance.appConfig.get("sfx_page_enabled")) && split.length == 1) {
            instance.sendMessage(channel, "All SFX: " + instance.appConfig.get("sfx_page_url"));
            return 0;
        }
        String sfxString = "";
        ArrayList<String> sfxResults = new ArrayList<>();
        if (split.length > 1) {
            String search = split[1];
            for (String key : instance.sfxList.keySet()) {
                if (key.matches(".*?"+search+".*?")) {
                    sfxResults.add(key);
                }
            }
        } else {
            for (String key : instance.sfxList.keySet()) {
                sfxResults.add(key);
            }
        }

        String sfxMessage = "All SFX";
        if (split.length > 1) {
            if (split[1].length() < 2) {
                Janna.sendMessage(channel, "Refine that SFX search a little, why don't you? (2 chars minimum)");
                return 1;
            }
            sfxMessage+= " containing '" + split[1] + "'";
        }
        sfxMessage+= ": ";

        for (String sfx : sfxResults) {
            sfxString += (sfxString.isEmpty() ? "" : ", ") + sfx;
        }
        Janna.messageQueue.queueLongMessage(channel, sfxMessage, sfxString);

        if (sfxResults.size() == 1) {
            new SeVoice(
                    user,
                    instance.makeTTSMessage(
                            new HashMap<>(),
                            Janna.butcher(sfxResults.get(0), user)
                    )
            );
        }
//        int limit = 400;
//        if (sfxString.length() < limit) {
//            Janna.sendMessage(channel, sfxMessage + sfxString);
//        } else {
//            ArrayList<String> sfxStrings = new ArrayList<>();
//            while (!sfxString.isEmpty()) {
//                // String short enough yet?
//                if (sfxString.length() < limit) {
//                    sfxStrings.add(sfxString);
//                    sfxString = "";
//                }
//                // String still too long
//                else {
//                    String temp = sfxString.substring(0, limit);
//                    int tempIndex = temp.lastIndexOf(",");
//                    if (tempIndex == -1) {
//                        // Bruh did you make a sfx with like 500char long name? Get outta here
//                        Janna.warn("C'mon, yo");
//                        sfxString = "";
//                    } else {
//                        sfxStrings.add(sfxString.substring(0, tempIndex));
//                        sfxString = sfxString.substring(tempIndex + 1).trim();
//                    }
//                }
//            }
//            for (int i = 0; i < sfxStrings.size(); i++) {
//                Janna.messageQueue.queueMessage(channel, sfxMessage + "["+(i+1)+"/"+sfxStrings.size()+"]: " + sfxStrings.get(i));
//            }
//        }

        return 0;
    }
}
