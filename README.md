# Janna
Janna is a Twitch TTS bot, designed as a Java-alternative to [Anna](https://www.project610.com/anna/), a now-almost-entirely-abandoned Windows based IRC TTS bot.

If you want something basic, that mostly works on Windows, maybe try Anna.

### Why use Janna?
Janna has cool features, like different voices for different users (Accent/Pitch/Speed), sound effects, and... Probably other stuff. If you want to make things a little more noisy and personal, or just keep up with Twitch chat without having to look away from what you're doing, then maybe you wanna try Janna.

### What's required to use Janna?
* The easy requirement is Java. Gotta have at least Java 8 installed.
* The less-easy requirement is Google Cloud. I'm gonna level with you, it's a bit of a pain to set up. 
  * [Signing up with Google Cloud to use Janna](https://docs.google.com/document/d/1t01Yv0U0TXHnbCyJB92wqj_TOxs0_RK9sVQK8V8sguQ)

**I need to make it clear that Google technically lets you use their TTS api for 'free', but only if you're signed up and have a payment method in case you go over the free quota**

I've never gone over the limit (4M chars/month), but if you've got a big channel with a super-active chat, then Google charges $4/1M chars. It's not a lot, but it could add up if you don't have a ton of money. 

Anyway, if you're cool with all that, once you've got your Google Cloud stuff set up, you should be able to run Janna by downloading and running the JAR file.

___

### How to set up Janna

* When you first start Janna (After setting up your Google Cloud stuff), an empty database and config.ini file will be auto-generated.

* Eventually you'll be able to log in in-app, but for now, close Janna and open up the config.ini file

* Enter a username and oauth token for the bot to use for talking to chat (This can be your own account or not)
  * Also enter the channel you'll be streaming from as the mainchannel (Any additional channels you'd like to listen in on can go into extrachannels)

* Save and close config.ini, and relaunch Janna

* Now you'll be prompted to log in as the broadcaster via Twitch. 
  * Janna will only ask for permissions to make/manage channel point rewards

* Once you've logged in, you should get a "Is google text to speech working and stuff" message. If you hear that, then it is!

___

### How to use Janna (Commands, etc)

* Incoming chat will be read aloud automatically

* Any !commands will not be read, and as a side effect, chatters can silence their messages by starting them with an exclamation point (`!`)
   (eg: ! Here's a super long message that I don't want read out loud)

  #### Mod commands
  * `!no` Silence any messages currently being read
  * `!stfu` Silence every message currently queued up to play (Or playing now)
  * `!janna.addsfx <phrase> <https://__________>` Play the sound in the URL when `phrase` shows up in chat
     (Only the first occurrence of SFX will be read, to avoid mega-spam)
  * `!janna.removesfx <phrase>` Remove the SFX associated with `phrase`
  * `!janna.addfilter <phrase> <replacement>` Janna will read `phrase` as if the chatter wrote `replacement`
  * `!janna.removefilter <phrase>` Remove the filter associated with `phrase`
  * `!janna.addreponse <phrase> <response>` Write a response in chat if `phrase` shows up anywhere in a user's message

  #### Everybody commands
  * (Janna is very mature, and 3% of chat messages will have "but enough about my butt" read after them)
    * `!dontbuttmebro` Your messages will no longer "enough about my butt"
    * `!dobuttmebro` If you change your mind, this will turn "enough about my butt" back on
