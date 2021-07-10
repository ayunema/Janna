package com.project610;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;

import static com.project610.Janna.error;

public class ListenThread extends Thread {
    BufferedReader br;
    BufferedWriter bw;
    boolean alive = true;
    long ticks = 0;
    Janna janna;
    ArrayList<Voice> voices;

    public ListenThread(BufferedReader br, BufferedWriter bw, Janna janna) {
        this.br = br;
        this.bw = bw;
        this.janna = janna;
        voices = new ArrayList<>();
    }

    public void run() {
        String line;
        while (alive) {
            ticks++;

            try {
                Thread.sleep(50);
                while (br.ready()) {
                    line = br.readLine();
                    String[] split = line.split(" ", 4);
                    if (split[0].equals("PING")) {
                        Janna.send("PONG " + split[1]);
                    }
                    else if (split[1].equals("PRIVMSG")) {
                        User user = janna.getUser(getName(split[0]));
                        String message = split[3].substring(1).trim();
                        Janna.chatArea.append("\n"+getName(split[0]) + ": " + message);
                        if (message.charAt(0) == '!') {
                            parseCommand(message, user);



                            continue;
                        }

                        String translated = message;

                        boolean canSpeak = true;

                        if (janna.muteList.contains(user.name.toLowerCase())) {
                            System.out.println("Not speaking, user is muted");
                            continue;
                        }

                        if (janna.whitelistOnly) {
                            if (false/*isOp*/) {

                            }
                            else if (!janna.whitelist.contains(user.name.toLowerCase())) {
                                continue;
                            }
                        }

                        if (user.voiceVolume <= 0) {
                            continue;
                        }

                        if (canSpeak) {
                            voices.add(new Voice(Janna.butcher(message, user), user));
                        }
                        //new Speaker(message).start();
                    }
                }
            } catch (Exception ex) {
                error("Listen thread broke", ex);
                break;
            }
        }
    }
    public String getName(String s) {
        s = s.substring(1, s.indexOf("!"));
        return s;
    }

    private void parseCommand(String message, User user) {
        message = message.substring(1);
        String[] split = message.split(" ");
        String cmd = split[0];
        if (cmd.equalsIgnoreCase("no")) {
            //voices.get(0).
        } else if (cmd.equalsIgnoreCase("stfu")) {
            // TODO
        } else if (cmd.equalsIgnoreCase("dontbuttmebro")) {
            if (janna.setUserPref(user, "butt_stuff", "0")) {
                janna.twitch.getChat().sendMessage("virus610", "Okay, I won't butt you, bro.");
            }
        } else if (cmd.equalsIgnoreCase("dobuttmebro")) {
            if (janna.setUserPref(user, "butt_stuff", "1")) {
                janna.twitch.getChat().sendMessage("virus610", "Can't get enough of that butt.");
            }
        } else if (cmd.equalsIgnoreCase("mute")) {
            // TODO
        }
    }
}
