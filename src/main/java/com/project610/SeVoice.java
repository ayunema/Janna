package com.project610;

import com.project610.async.AudioConcat;
import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import static com.project610.Janna.*;

public class SeVoice {
    public PlaySound sound;

    public SeVoice(User user, TTSMessage... messages) {
        if (messages.length == 0) return;
        if (null == appConfig.get("ffmpegpath")) {
            warn("Unable to process TTS audio without FFMPEG.\nClick Config > FFMPEG > Download, or\n" +
                    "Click Config > FFMPEG > Locate FFMPEG if already installed");
            return;
        }

        // Make temp/sfx dirs if DNE
        File dir = new File("temp");
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Made temp dir");
        }
        dir = new File("sfx");
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Made sfx dir");
        }

        String username = (user == null ? "SYSTEM" : user.name);
        String voiceName = Janna.defaultVoice;
        if (user != null && Janna.voiceNames.contains(user.voiceName)) {
            voiceName = user.voiceName;
        }
        String baseUrl = "https://api.streamelements.com/kappa/v2/speech?voice="
                + voiceName + "&text=";
        LinkedHashMap<String, String> outFiles = new LinkedHashMap<>();
        for (TTSMessage message : messages) {
            if ("sfx".equalsIgnoreCase(message.type) && !message.text.isEmpty()) {
                String expectedName = "sfx/" + message.text.substring(message.text.lastIndexOf("/") + 1);
                expectedName = expectedName.substring(0, expectedName.lastIndexOf('.'))+Audio.desiredExt;
                String filename = "temp/rawsfx-" + message.text.substring(message.text.lastIndexOf("/") + 1);
                if (!Files.exists(Paths.get(expectedName))) {
                    try {
                        downloadFile(new URL(message.text), new File(filename));
                        outFiles.put(filename, message.extra);
                    } catch (Exception ex) {
                        error("HHHHHHHHHHHH", ex);
                    }
                } else {
                    outFiles.put(expectedName, message.extra);
                }
            } else if ("message".equalsIgnoreCase(message.type) && !message.text.isEmpty()) {
                String filename = "temp/rawtext-se-output-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 1000) + ".mp3";

                try {
//                    FileUtils.copyURLToFile(new URL(baseUrl + message.text), new File(filename));
//                    FileUtils.copyURLToFile(urlEncode(baseUrl + message.text), new File(filename));
                    /////////// FileUtils.copyURLToFile(urlEncode(EmojiParser.parseToAliases(baseUrl + message.text)), new File(filename));
                    downloadFile(new URL(baseUrl + message.text), new File(filename));
//                    FileUtils.copyURLToFile(new URL (EmojiParser.parseToAliases(baseUrl + message.text)), new File(filename));
                    outFiles.put(filename, "");
                } catch (Exception ex) {
                    error("GGGGGGGGGGG", ex);
                }
            }
        }
        if (!outFiles.isEmpty()) {
            new Thread(new AudioConcat(username, outFiles)).start();
        }
    }

    private void downloadFile(URL url, File file) {
        HttpGet httpGet = new HttpGet(uriEncode(url.toString()));
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.11 Safari/537.36");
        httpGet.addHeader("Referer", "https://www.google.com");
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {

            HttpEntity imageEntity = httpResponse.getEntity();

            if (imageEntity != null) {
                FileUtils.copyInputStreamToFile(imageEntity.getContent(), file);
            }
        } catch (IOException ex) {
            error("Failed to download SFX/TTS file", ex);
        }
    }

    public URL urlEncode (String string){
        try {
            // I don't know why
            String decoded = URLDecoder.decode(string, "UTF-8");
            URL url = new URL(decoded);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            return uri.toURL();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public URI uriEncode (String string){
        try {
            // I don't know why
            String decoded = URLDecoder.decode(string, "UTF-8");
            URL url = new URL(decoded);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            return uri;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
