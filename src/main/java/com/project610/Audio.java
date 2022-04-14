package com.project610;

import com.project610.async.AudioConcat;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.project610.Janna.*;

// WIN: https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.7z
public class Audio {

    public static String ffmpegPath = "ffmpeg-5.0.1-essentials_build\\bin\\ffmpeg ";
    private static Runtime runtime = Runtime.getRuntime();
    public static String desiredExt = ".wav";

    public static String concat(String username, ArrayList<String> filenames) {
        int rand = (int)(Math.random()*100000);
        String out = "temp/se_concat-"+rand+desiredExt;

        String fileListData = "";
        String fileListName = "temp/files"+rand+".txt";
        Path fileList = Paths.get(fileListName);
        ArrayList<String> converted = new ArrayList<>();
        try {
            for (String filename : filenames) {
                String name = "";
                boolean keepFile = false;
                String newName = "";
                if (filename.indexOf("temp/rawtext-") == 0) {
                    name = "temp/"+filename.substring(13);
                    newName = convert(filename, name);
                    converted.add(newName);
                } else if (filename.indexOf("temp/rawsfx-") == 0) {
                    name = "sfx/"+filename.substring(12);
                    keepFile = true;
                    newName = convert(filename, name);
                } else {
                    newName = filename;
                }
                fileListData += "file '../"+newName+"'\n";
            }
            Files.write(fileList, fileListData.getBytes(StandardCharsets.UTF_8));
            // Make sure the files are all written
            for (int i = 0; i < converted.size(); i++) {
                Path path = Paths.get(converted.get(i));
                if (!Files.exists(path)) {
                    i--;
                    Thread.sleep(50);
                }
            }

            User user = users.get(userIds.get(username));
            // Translate google speed/pitch to ffmpeg
            double voicePitch = (user == null ? 1 : (1+user.voicePitch / 25));
            double voiceSpeed = (user == null ? 1 : user.voiceSpeed);
            String pitchSpeed = "-af asetrate=24000*"+voicePitch+",aresample=24000,atempo=1/"+voicePitch+"*"+voiceSpeed;
            String cmd = ffmpegPath + "-f concat -safe 0 -y -i " + fileListName + " " + pitchSpeed + " " + out;

            runCmd(cmd);

            cleanupQueue.queue.addAll(converted);
            cleanupQueue.queue.add(fileList.toString());

            // Wait?
            Path expected = Paths.get(out);
            for (int i = 0; i < 400 && !Files.exists(expected); i++) {
                Thread.sleep(10);
            }

            Janna.speechQueue.sounds.add(new PlaySound(username, out));
        } catch (Exception ex) {
            error("EEEEEEEE", ex);
        }
        return "";
    }

    private static void runCmd(String cmd) {
        try {
            Process p = runtime.exec(cmd);
            p.waitFor(1, TimeUnit.SECONDS);
//            BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
//            String line = "";
//            while ((line = br.readLine()) != null) {
//                System.out.println(line);
//            }
        } catch (Exception ex) {
            error("Error running process: " + cmd, ex);
        }
    }

    public static String convert(String src, String dst) {
        try {
            String newName = dst.substring(0, dst.lastIndexOf('.')) + desiredExt;
            String cmd = ffmpegPath + "-i " + src + " -af \"aformat=sample_fmts=s16:sample_rates=24000\" -ac 1 " +
                    newName;
            runCmd(cmd);
            cleanupQueue.queue.add(src);
            return newName;
        } catch (Exception ex) {
            error("DDDDDDDDDDD", ex);
        }
        return "";
    }

}