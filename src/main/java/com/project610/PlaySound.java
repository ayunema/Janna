package com.project610;

import java.lang.Runnable;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.*;
import java.io.IOException;
import java.io.*;

import static com.project610.Janna.error;

public class PlaySound implements Runnable {

    BufferedInputStream soundFile;
    Clip clip;
    AudioInputStream stream;

    public PlaySound(String file) throws Exception {
        // we have to create InputStream by ourselves
        InputStream is = new FileInputStream(file);
        soundFile = new BufferedInputStream(is);
    }

    public void run() {
        synchronized(this){

            System.out.println("run");

            AudioFormat format;
            DataLine.Info info;

            try {
                // we pass stream instead of file
                // it looks like getAudioInputStream messes around with
                // file
                stream = AudioSystem.getAudioInputStream(soundFile);
                format = stream.getFormat();
                info = new DataLine.Info(Clip.class, format);
                clip = (Clip) AudioSystem.getLine(info);
                System.out.println("open");
                clip.open(stream);
                System.out.println("start");
                clip.start();
                System.out.println("wait");
                while(clip.getMicrosecondLength() != clip.getMicrosecondPosition()) {
                    //
                }
                // we can close everything by ourselves
                System.out.println("close");
                clip.close();
                stream.close();
                soundFile.close();
                System.out.println("closed");
            }catch(Exception ex){
                error("PlaySound error", ex);
            }

            soundFile = null;
            clip = null;
            stream = null;

            notifyAll();
            System.out.println("All done");
        }
    }
}