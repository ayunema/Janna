package com.project610;

// Imports the Google Cloud client library
import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;
import com.google.protobuf.UnknownFieldSet;
import javafx.scene.media.MediaPlayer;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.project610.Janna.*;

public class Voice {

    PlaySound sound;

    public Voice(String message, User user) {
        // Make temp dir if DNE
        File dir = new File("temp");
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("Made dir");
        }

        // Generate a unique-enough filename
        String filename = "temp/output-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000) + ".wav";

        // Initiate default voice (In case of system-messages, or weird bugs), sub in user-voice if exists
        String voiceName = Janna.defaultVoice;
        double pitch = 0;
        double speed = 0;
        if (user != null) {
            if (Janna.voiceNames.contains(user.voiceName)) {
                voiceName = user.voiceName;
            }
            pitch = user.voicePitch;
            speed = user.voiceSpeed;
        } else {
            debug("User is NULL! Using default values");
        }

        // This is a buncha google sample code, with bits and bobs injected afterward
        // Instantiates a client
        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
            // Set the text input to be synthesized
            //SynthesisInput input = SynthesisInput.newBuilder().setText(message).build();
            SynthesisInput input = SynthesisInput.newBuilder().setSsml(message).build();

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
//            SynthesizeSpeechResponse response =
//                    textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

            // Get the audio contents from the response
            ByteString audioContents = response.getAudioContent();

            // Write the response to the output file.
            try (OutputStream out = new FileOutputStream(filename)) {
                out.write(audioContents.toByteArray());
                out.flush();
                out.close();
            } catch (Exception ex) {
                error("Messed up writing audio file: " + ex.toString(), ex);
            }

            // Sound file created, send it to the speechQueue for... Queuing.
            sound = new PlaySound((null == user) ? " " : user.name, filename);
            speechQueue.sounds.add(sound);
        } catch (Exception ex) {
            error("TTS screwed up: " + ex.toString(), ex);
        }
    }

    public void stop() {
        sound.clip.stop();
        sound.busy=false;
    }
}