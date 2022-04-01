package com.project610;

import com.github.twitch4j.helix.domain.Emote;
import com.github.twitch4j.kraken.domain.Emoticon;
import com.github.twitch4j.kraken.domain.EmoticonImages;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;

import static com.project610.Janna.error;
import static com.project610.Janna.mainchannel;

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
                        Regex rx_url = new Regex("([a-zA-Z]+:\\/\\/)?[a-zA-Z0-9]+(\\.[a-zA-Z]{2,})*\\.[a-zA-Z]{2,}(:\\d+)?(\\/[^\\s]+)*");
                        Regex rx_emote = new Regex("[a-z0-9]+([A-Z0-9][a-zA-Z0-9]+)+");

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
                            voices.add(new Voice(Janna.butcher(user.name + ": " + message, user), user));
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
                janna.twitch.getChat().sendMessage(mainchannel, "Okay, I won't butt you, bro.");
            }
        } else if (cmd.equalsIgnoreCase("dobuttmebro")) {
            if (janna.setUserPref(user, "butt_stuff", "1")) {
                janna.twitch.getChat().sendMessage(mainchannel, "Can't get enough of that butt.");
            }
        } else if (cmd.equalsIgnoreCase("mute")) {
            // TODO
        }
    }
}
