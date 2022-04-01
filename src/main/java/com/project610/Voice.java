package com.project610;

// Imports the Google Cloud client library
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.project610.Janna.error;

public class Voice {

    public MediaPlayer mp;
    PlaySound sound;

    public Voice(String message, User user) {
        // Make temp dir if DNE
        File dir = new File("temp");
        System.out.println("dir=temp");
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Made dir");
        }

        String filename = "temp/output-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000) + ".wav";
        filename = filename.replace('/', File.separatorChar);


        String voiceName = Janna.defaultVoice;
        double pitch = 0;
        double speed = 0;
        if (user != null) {
            System.out.println("User: " + user.name + " is OK");
            voiceName = user.voiceName;
            pitch = user.voicePitch;
            speed = user.voiceSpeed;
        } else {
            System.out.println("User is NULL! Using default values");
        }

        // Instantiates a client
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            // Set the text input to be synthesized
            SynthesisInput input = SynthesisInput.newBuilder().setText(message).build();

            // Build the voice request, select the language code ("en-US") and the ssml voice gender
            // ("neutral")
            VoiceSelectionParams voice =
                    VoiceSelectionParams.newBuilder()
                            .setLanguageCode("en-US")
                            //.setSsmlGender(SsmlVoiceGender.NEUTRAL)
                            .setName(voiceName) // To be user-specific later
                            .build();

            // Select the type of audio file you want returned
            AudioConfig audioConfig =
                    AudioConfig.newBuilder()
                            .setAudioEncoding(AudioEncoding.LINEAR16)
                            .setPitch(pitch)
                            .setSpeakingRate(speed)
                            .build();

            // Perform the text-to-speech request on the text input with the selected voice parameters and
            // audio file type
            SynthesizeSpeechResponse response =
                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            try (OutputStream out = new FileOutputStream(filename)) {
                System.out.println("About to write file");
                out.write(audioContents.toByteArray());
                out.flush();
                out.close();
                System.out.println("File written");

                sound = new PlaySound(filename);
                sound.run();
                System.out.println("Sound played");
                Files.deleteIfExists(Paths.get(filename));
                System.out.println("Deleted");
            } catch (Exception ex) {
                error("Voice screwed up: " + ex.toString(), ex);
            }
        } catch (Exception ex) {
            error("TTS screwed up: " + ex.toString(), ex);
        }
    }

    public void stop() {
        //sound.
    }
}