package com.project610.async;

import com.project610.Janna;

public class SfxPageUploader implements Runnable {
    public void run () {
        try {
            while (true) {
                if (Janna.sfxDirty) {
                    Janna.instance.uploadSfxListPage();
                    Janna.sfxDirty = false;
                }
                Thread.sleep(2500);
            }
        } catch (InterruptedException ex) {}
    }
}
