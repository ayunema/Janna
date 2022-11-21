package com.project610.async;

import com.project610.Janna;
import com.project610.Sfx;

import java.sql.SQLException;

import static com.project610.Janna.error;
import static com.project610.Janna.sfxList;

public class SfxUpdateThread implements Runnable {
    public void run() {
        try {
            while (true) {
                if (Janna.usedSfx.size() > 0) {
                    for (String sfxString : Janna.usedSfx) {
                        Sfx sfx = sfxList.get(sfxString);
                        try {
                            Janna.instance.executeUpdate("UPDATE reaction SET uses = '" + sfx.uses + "' WHERE phrase = '" + sfxString + "';");
                        } catch (SQLException ex) {
                            error("Failed to update SFX uses", ex);
                        }
                    }
                    Janna.usedSfx.clear();
                    Janna.sfxDirty = true;
                }
                Thread.sleep(6000);
            }
        } catch (InterruptedException ex) {

        }
    }
}
