# Janna
Janna is a Twitch TTS bot, designed as a Java-alternative to [Anna](https://www.project610.com/anna/), a now-almost-entirely-abandoned Windows based IRC TTS bot.

If you want something basic, that mostly works on Windows, maybe try Anna.

### Why use Janna?
Janna has cool features, like different voices for different users (Accent/Pitch/Speed), sound effects, and... Probably other stuff. If you want to make things a little more noisy and personal, or just keep up with Twitch chat without having to look away from what you're doing, then maybe you wanna try Janna.

### What's required to use Janna?
* The easy requirement is **Java**. Gotta have at least Java 8 installed.
* The still-pretty-easy requirement is **FFMPEG**
  * If you've got it already, you can locate the executable/binary on your system with Config>Locate FFMPEG Executable.
  * If not, you can Config > Download FFMPEG (On Windows, this'll automatically download. Haven't got that going for Mac/Linux right now)

So basically, just download the .jar over in [Releases](https://github.com/Virus610/Janna/releases) and run it. The app will take you through the rest of the setup.

___

### How to set up Janna

* When you first start Janna, an empty database will be auto-generated (janna.sqlite).

* You should automatically get prompted to do some login stuff
  * Enter a username and oauth token for the bot to use for talking to chat (This can be your own account or some alt account)
    * If you need an oauth token, you can generate one here: https://twitchapps.com/tmi/
    * Also enter the channel you'll be streaming from as the mainchannel (Any additional channels you'd like to listen in on can go into extrachannels)

* Now you'll be prompted to log in as the broadcaster via Twitch. 
  * Without this, Janna can't make/manage channel point rewards, or really do much of anything at all.

* Once you've logged in, you should get a "Is StreamElements text to speech working and stuff?" message. If you hear that, then it is!
  * One day, this'll be configurable

___

### How to use Janna (Commands, etc)

* Incoming chat will be read aloud automatically

* Any !commands will not be read, and as a side effect, chatters can silence their messages by starting them with an exclamation point (`!`)
   (eg: ! Here's a super long message that I don't want read out loud)

* Dealing with emote spam
  * In the menu `Chat > Emote Handling`, you have the following options:
    * `Default` Read all emotes
    * `First of each` Only read the first occurrence of each emote once
    * `First only` Only read the first emote

  #### Mod commands
  * `!no` Silence any messages currently being read
  * `!stfu` Silence every message currently queued up to play (Or playing now)
  * `!janna.addsfx <phrase> <https://__________>` Play the sound in the URL when `phrase` shows up in chat
     (Only the first occurrence of SFX will be read, to avoid mega-spam)
  * `!janna.modsfx <phrase> <effect>
    * Currently only supports volume, eg: volume=+4
  * `!janna.removesfx <phrase>` Remove the SFX associated with `phrase`
  * `!janna.addfilter <phrase> <replacement>` Janna will read `phrase` as if the chatter wrote `replacement`
    * Useful example: !janna.addresponse !voices https://docs.google.com/spreadsheets/d/1hrhoy3yoLjKE_N_XgHwG8qFAWWxMch6CerqV2xX-XOs
  * `!janna.removefilter <phrase>` Remove the filter associated with `phrase`
  * `!janna.addreponse <phrase> <response>` Write a response in chat if `phrase` shows up anywhere in a user's message
  * `!janna.addalias <oldCommand> <newCommand>` Allows users to use `newCommand` instead of `oldCommand`
    * Example: !janna.addalias janna.addsfx addsfx (Note: You can write !addsfx or addsfx, it doesn't matter)
  * `!janna.removealias <newCommand>` Remove the alias associated with `newCommand`

  #### Everybody commands
  * (Janna is very mature, and 3% of chat messages will have "but enough about my butt" read after them)
    * `!dontbuttmebro` Your messages will no longer "enough about my butt"
    * `!dobuttmebro` If you change your mind, this will turn "enough about my butt" back on
  * `!voice` Show in chat what voice, speed, and pitch you are currently using
  * `!janna.getsfx <sfxCode>` Outputs the URL and any extra modifications (eg: Volume) for `sfxCode`
  * `!janna.voiceusers <voiceCode>` Tells you how many different chatters are currently using `voiceCode` (In case you want to be unique)
