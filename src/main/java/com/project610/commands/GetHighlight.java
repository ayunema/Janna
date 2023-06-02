package com.project610.commands;

import com.github.twitch4j.helix.domain.*;
import com.project610.Creds;
import com.project610.Janna;
import com.project610.utils.Util;

import java.util.*;

import static com.project610.Janna.*;

public class GetHighlight extends Command {
    boolean fallback = false;

    @Override
    public Object apply(Object o) {
        super.apply(o);
        if (params.get("fallback") != null) fallback = true;

        // Handle args
        if (split.length == 1) {
            messageQueue.queueMessage(channel, "Usage: !video <searchTerm> [channel:channelName] [by:user] [game:\"game title\"]");
            return 1;
        }


        String[] split = Util.parseArgs(message);


        ArrayList<String> searchTerms = new ArrayList<>();
        String fullSearch = "";
        String channelId = getChannelId(channel), videoedBy = "", game = "";
        for (int i = 1; i < split.length; i++) {
            String temp = split[i].toLowerCase();
            if (temp.startsWith("channel:")) {
                try {
                    channelId = getChannelId(temp.split(":")[1]);
                } catch (Exception ex) {
                    messageQueue.queueMessage(channel, "Couldn't get videos for " + temp);
                    error("Failed to get channelID for " + temp, ex);
                    return 1;
                }
            } else if (temp.startsWith("by:")) {
                videoedBy = temp.split(":")[1];
            } else if (temp.startsWith("game:")) {
                game = temp.split(":")[1];
            } else {
                searchTerms.add(temp);
                fullSearch += temp + " ";
            }
        }
        fullSearch = fullSearch.trim();

        // Gather all videos
        LinkedHashSet<Video> videos = new LinkedHashSet<>();
        int limit = 100, results = -1;
        String cursor = null;

        while (results != videos.size()) {
            try {
                VideoList videoList = twitch.getHelix().getVideos(Creds._helixtoken, null, channelId, null, null, null, null, null, cursor, null, limit).execute();
                if (cursor != null && cursor.equalsIgnoreCase(videoList.getPagination().getCursor())) break;
                cursor = videoList.getPagination().getCursor();
                results = videos.size();
                videos.addAll(videoList.getVideos());
            } catch (Exception ex) {
                results = videos.size();
            }
        }

        // Filter out stuff
        // Try to find video with increasing lenience

        // Starting with if all non-channel-name search terms strung together are found as one string
        ArrayList<Video> foundVideos = new ArrayList<>();
        if (!fullSearch.isEmpty()) {
            for (Video video : videos) {
                if (video.getTitle().toLowerCase().contains(fullSearch)) {
                    foundVideos.add(video);
                }
            }

            if (foundVideos.size() == 0) {
                // No luck? Let's try if all terms show up in any order
                for (Video video : videos) {
                    for (String term : searchTerms) {
                        if (!video.getTitle().toLowerCase().contains(term)) {
                            continue;
                        }
                        foundVideos.add(video);
                    }
                }
            }

            if (foundVideos.size() == 0) {
                // STILL No luck? Let's try if individual terms show up
                // This is hacky, but adding weight to videos with more matching terms by adding them to the list multiple times
                for (Video video : videos) {
                    for (String term : searchTerms) {
                        if (video.getTitle().toLowerCase().contains(term)) {
                            foundVideos.add(video);
                        }
                    }
                }
            }

            // If after all this, we've got nothing, maybe just give up
            if (foundVideos.size() == 0) {
                if (params.get("fallback") == null) {
                    messageQueue.queueMessage(channel, "Couldn't find any highlights, sorry! Will see if any similar clips exist");
                    params.put("fallback", true);
                    return Janna.instance.commandMap.get("janna.clip").apply(params);
                }
                else {
                    messageQueue.queueMessage(channel, "Couldn't find any highlights, either");
                    return 1;
                }
            }
        } else {
            foundVideos = new ArrayList<>(videos);
        }

        // TODO: Highlights with game filtering
        /*if (!game.isEmpty()) {
            trace("Checking for game match");
            HashSet<Video> videoSet = new HashSet<>(foundVideos);

            LinkedHashSet<Video> gameVideos = new LinkedHashSet<>();
            int temp = 0;
            for (Video video : videoSet) {
                System.out.print ("Check video #" + temp++ + "... ");
                String gameName = "";
                try {
                    Stream stream = twitch.getHelix().getStreams(Creds._helixtoken, null, null, 1, null, null)
                    if (!video.getGameId().isEmpty()) {
                        gameName = Util.getGameById(video.getGameId()).toLowerCase();
                    }
                } catch (Exception ex) {
                    warn("Failed to get game name for some reason???" + ex.toString());
                }

                if (gameName.contains(game)) {
                    gameVideos.add(video);
                }
            }
            if (gameVideos.size() == 0) {
                messageQueue.queueMessage(channel, "No videos matching game:"+game);
                return 1;
            }
            foundVideos.retainAll(gameVideos);
        }*/

        messageQueue.queueMessage(channel, getRandomVideo(foundVideos).getUrl());
        return 0;
    }

    private Video getRandomVideo(ArrayList<Video> foundVideos) {
        return foundVideos.get((int)(Math.random() * foundVideos.size()));
    }

    private String getChannelId(String channelName) {
        return twitch.getHelix().getUsers(Creds._helixtoken, null, Arrays.asList(channelName)).execute().getUsers().get(0).getId();
    }
}
