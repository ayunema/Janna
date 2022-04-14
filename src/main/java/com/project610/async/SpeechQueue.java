package com.project610.async;

import com.project610.PlaySound;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;

import static com.project610.Janna.debug;

public class SpeechQueue implements Runnable {
    public ArrayList<PlaySound> sounds;
    public ArrayList<PlaySound> currentlyPlaying;
    public boolean allowConsecutive;
    public boolean stop;

    public SpeechQueue(boolean allowConsecutive) {
        stop = false;
        this.allowConsecutive = allowConsecutive;
        sounds = new ArrayList<>();
        currentlyPlaying = new ArrayList<>();
    }

    public void run() {
        while (!stop) {
            // Playing ping-pong with acknowledgements to keep things from
            try {
                for (int i = 0; i < currentlyPlaying.size(); i++) {
                    if (!currentlyPlaying.get(i).busy) {
                        currentlyPlaying.get(i).cleanup = true;
                        currentlyPlaying.remove(i--);
                    }
                }
            } catch (ConcurrentModificationException ex) {
                // That's okay, just wait 'til the next cycle
                debug("ConcurrentModification on speechQueue cleanup cycle");
            }

            // Start playing any sounds in the queue (This might be vestigial)
            if (!sounds.isEmpty()) {
                for (int i = 0; i < sounds.size(); i++) {
                    play(sounds.get(0));
                }
            }

            // Save the CPU a bit of work
            try {
                Thread.sleep(10);
            } catch (Exception ex) {}
        }
    }

    public boolean isBusy (PlaySound sound) {
        try {
            // If this is the first thing in the queue, then it needn't wait for anything
            // If it's not the first thing, it definitely needs to wait if allowConsecutive is false
            if (currentlyPlaying.get(0).equals(sound)) {
                return false;
            } else if (!allowConsecutive) {
                return true;
            }

            // Otherwise allowConsecutive is true, and this isn't the first thing in the queue, so check to
            //   make sure it's not playing something by the same user
            for (PlaySound current : currentlyPlaying) {
                if (current.username.equalsIgnoreCase(sound.username) && !current.equals(sound)) {
                    return true;
                }
            }
            return false;
        } catch (ConcurrentModificationException ex) {
            return true;
        } catch (IndexOutOfBoundsException ex) {
            // SpeechQueue likely purged
            return true;
        }
    }

    // This is probably unnecessary now, as `isBusy` is handling
    private void play (PlaySound sound) {
        currentlyPlaying.add(sound);
        sounds.remove(sound);
        new Thread(sound).start();
    }
}
