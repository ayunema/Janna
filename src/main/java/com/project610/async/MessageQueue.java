package com.project610.async;

import com.project610.Janna;

import java.util.ArrayList;

public class MessageQueue implements Runnable {
    int limit = 400;
    ArrayList<String> channels = new ArrayList<>(), messages = new ArrayList<>();

    public void run() {
        while (true) { // Don't @ me
            if (channels.size() > 0) {
                Janna.sendMessage(channels.get(0), messages.get(0));
                channels.remove(0);
                messages.remove(0);
            }

            try {
                Thread.sleep(150); // Wait for the rate (limiter)
            } catch (InterruptedException ex) {
                // ¯\_(ツ)_/¯
            }
        }
    }

    public void queueMessage(String channel, String message) {
        channels.add(channel);
        messages.add(message);
    }

    public void queueLongMessage(String channel, String lineHeader, String message) {
        if (message.length() < limit) {
            Janna.sendMessage(channel, lineHeader + message);
        } else {
            ArrayList<String> messageList = new ArrayList<>();
            while (!message.isEmpty()) {
                // String short enough yet?
                if (message.length() < limit) {
                    messageList.add(message);
                    message = "";
                }
                // String still too long
                else {
                    String temp = message.substring(0, limit);
                    int tempIndex = temp.lastIndexOf(" ");
                    if (tempIndex == -1) {
                        // Bruh did you write a thing with like a 500char long word? Get outta here
                        Janna.warn("C'mon, yo");
                        message = "";
                    } else {
                        messageList.add(message.substring(0, tempIndex));
                        message = message.substring(tempIndex + 1).trim();
                    }
                }
            }
            for (int i = 0; i < messageList.size(); i++) {
                queueMessage(channel, lineHeader + "[" + (i + 1) + "/" + messageList.size() + "]: " + messageList.get(i));
            }
        }
    }
}
