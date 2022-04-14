package com.project610.async;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import static com.project610.Janna.debug;
import static com.project610.Janna.error;

public class CleanupQueue implements Runnable {
    public ArrayList<String> queue = new ArrayList<>();
    public boolean stop = false;

    public void run() {
        while (!stop) {
            String current = queue.size() > 0 ? queue.get(0) : "";
            if (!current.isEmpty()) {
                try {
                    if (Files.deleteIfExists(Paths.get(current))) {
                        debug("Cleaned up file: " + current);
                    }
                    queue.remove(0);
                } catch (Exception ex) {
                    error("Error cleaning up file: " + current, ex);
                }
            }
            try {
                Thread.sleep(100);
            } catch (Exception ex) { }
        }
    }
}
