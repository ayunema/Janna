package com.project610;

import java.lang.Runnable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.*;
import java.io.IOException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.project610.Janna.*;

public class PlaySound implements Runnable {

    BufferedInputStream soundFile;
    Clip clip;
    AudioInputStream stream;
    String filename;
    String username;
    boolean busy, cleanup;

    public PlaySound(String filename, String username) throws Exception {
        this.filename = filename;
        this.username = username;
        InputStream is = new FileInputStream(filename);
        soundFile = new BufferedInputStream(is);
        busy = true;
        cleanup = false;
    }

    public void run() {
        debug(System.currentTimeMillis() + ": run");

        AudioFormat format;
        DataLine.Info info;

        try {
            // Pre-load stuff so it's ready to go when needed
            stream = AudioSystem.getAudioInputStream(soundFile);
            format = stream.getFormat();
            info = new DataLine.Info(Clip.class, format);
            clip = (Clip) AudioSystem.getLine(info);
            debug(System.currentTimeMillis() + ": open");
            clip.open(stream);

            // Verify that nothing else in the speechQueue is 'blocking' this from playing
            waitUntilNotBusy();

            // Play sound, then wait to proceed until it's done
            debug(System.currentTimeMillis() + ": start");
            clip.start();
            debug(System.currentTimeMillis() + ": wait");
            while (clip.getMicrosecondLength() != clip.getMicrosecondPosition() && busy) {
                Thread.sleep(1);
            }

            // This is hacky, but hear me out: In order to let the next clip (If any) play without interruption,
            //  we need to flag this as no longer in use, but closing the streams opened earlier sometimes takes a bit.
            //  So by marking it as not-busy and then waiting to be told to clean up by the speechQueue, the next sound
            //  can start playing as soon as possible.
            //  Don't @ me
            debug(System.currentTimeMillis() + ": close");
            busy = false;
            while (!cleanup) { // Take your time, once speechQueue says it's okay to clean up, move on
                Thread.sleep(1000);
            }
            clip.close();
            stream.close();
            soundFile.close();
            debug(System.currentTimeMillis() + ": closed");

            // Delete the file now that it's no longer needed
            debug(System.currentTimeMillis() + ": Sound played");
            Files.deleteIfExists(Paths.get(filename));
            debug(System.currentTimeMillis() + ": Deleted");
        } catch (Exception ex) {
            error("PlaySound error", ex);
        }

        soundFile = null;
        clip = null;
        stream = null;

        debug(System.currentTimeMillis() + ": All done");
    }

    private void waitUntilNotBusy() {
        while (speechQueue.isBusy(this)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                break;
            }
        }
    }
}