package com.project610.async;

import com.project610.Janna;

import java.util.ArrayList;

public class MessageQueue implements Runnable {

    ArrayList<String> channels = new ArrayList<>(), messages = new ArrayList<>();
    public void run() {
        while (true) { // Don't @ me
            if (channels.size() > 0) {
                Janna.sendMessage(channels.get(0), messages.get(0));
                channels.remove(0);
                messages.remove(0);
            }

            try {
                Thread.sleep(1000); // pee ell zed
            } catch (InterruptedException ex) {
                // ¯\_(ツ)_/¯
            }
        }
    }

    public void queueMessage(String channel, String message) {
        channels.add(channel);
        messages.add(message);
    }
}
