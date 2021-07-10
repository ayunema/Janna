package com.project610;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.kraken.domain.KrakenUserList;
import com.github.twitch4j.pubsub.domain.ChannelPointsRedemption;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import javafx.scene.control.TextArea;

import org.java_websocket.client.WebSocketClient;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.swing.*;

import static javax.swing.BoxLayout.LINE_AXIS;
import static javax.swing.BoxLayout.PAGE_AXIS;

public class Janna extends JPanel {

    private static final int LOG_LEVEL = 6;
    static BufferedReader inputReader = null;
    static BufferedReader channelReader = null;
    static BufferedWriter channelWriter = null;
    //static ListenThread listenThread;
    //public static TextArea chatArea;
    public Socket socket = null;
    public Connection sqlCon;
    //public static TextToSpeech tts = new TextToSpeech();

    public static ArrayList<String> messages = new ArrayList<>();
    public static ArrayList<Voice> voices = new ArrayList<>();
    public static ArrayList<String> voiceNames = new ArrayList<>();
    public static String defaultVoice = "en-US-Standard-B";

    public static HashMap<Integer, User> users = new HashMap<>();
    public static HashMap<String, Integer> userIds = new HashMap<>();

    WebSocketClient client;
    Webserver webserver;

    String oauth = "";
    public static TwitchClient twitch;

    public ArrayList<String> muteList = new ArrayList<>();
    public ArrayList<String> whitelist= new ArrayList<>();
    public boolean whitelistOnly = false;





    JPanel midPane;
    public static JTextArea chatArea;
    JScrollPane chatScroll;
    JTextField inputField;

    JFrame parent;

    Path configPath = Paths.get("config.ini");

    public Janna(String[] args, JFrame jf) {
        parent = jf;
        setLayout(new BoxLayout(this, PAGE_AXIS));

        try {
            initUI();
        } catch (Exception ex) {
            error("Failed to initUI, game over man", ex);
        }
    }



    public void initUI() throws Exception {

        // Mid pane to hold chat area, user list, input box, and send button (h-box)
        midPane = new JPanel();
        midPane.setLayout(new BoxLayout(midPane, LINE_AXIS));
        add(midPane);

        // Chat pane holds all but the user list (v-box)
        JPanel chatPane = new JPanel();
        chatPane.setLayout(new BoxLayout(chatPane, PAGE_AXIS));
        midPane.add(chatPane);

        chatArea = new JTextArea();
        //chatArea.setPreferredSize(new Dimension(300, 100));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(chatArea.getFont().deriveFont(11f));
        chatScroll = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //chatScroll.setPreferredSize(new Dimension(300,400));

        inputField = new JTextField();
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        inputField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendChat();
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });
        chatPane.add(chatScroll);

        JPanel inputPane = new JPanel();
        inputPane.setLayout(new BoxLayout(inputPane, LINE_AXIS));

        chatPane.add(inputPane);

        inputPane.add(inputField); // This should be in yet another pane??
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendChat());
        inputPane.add(sendButton);

        midPane.add (new JButton("<Userlist>"));

        add (new JButton("poop"));
        add (new JButton("farts"));
        add (new JButton("and butts"));

/*
        VBox vbox = new VBox();

        // Big chat box
        HBox hbox1 = new HBox();
        chatArea = new TextArea("poo poo poo \npoo poo <br/>poop");
        chatArea.setPrefWidth(500);
        chatArea.setPrefHeight(400);
        chatArea.setWrapText(true);
        chatArea.setEditable(false);

        hbox1.getChildren().add(chatArea);

        Button butt = new Button("Ma butt");
        hbox1.getChildren().add(butt);

        Circle c = new Circle(0, 0, 25, Color.GREEN);
        c.setFill(Color.BLUE);


        HBox hbox2 = new HBox();

        TextField inputField = new TextField("doop");
        inputField.setPrefWidth(500);

        hbox2.getChildren().addAll(inputField, c);

        vbox.getChildren().addAll(hbox1,hbox2);


        //primaryStage.initStyle(StageStyle.TRANSPARENT);
        //scene.setFill(Color.TRANSPARENT);

*/
    }

    public void init() throws Exception {

        // Get login info
        try {
            List<String> config = Files.readAllLines(configPath);
            for (String s : config) {
                s = s.trim();
                if (s.charAt(0) == '#') continue;
                if (s.indexOf("username=") == 0) {
                    Creds._username = s.split("=", 2)[1];
                } else if (s.indexOf("oauth=") == 0) {
                    Creds._password = s.split("=", 2)[1];
                }
            }
            if (Creds._username.isEmpty() || Creds._password.isEmpty()) {
                throw new Exception();
            }
        } catch (Exception ex) {
            warn("Failed to load username or oauth token, please update `config.ini` with your chat credentials");
            if (!Files.exists(configPath)) {
                Files.write(configPath, "# Login credentials go here. You don't want to use your password, but an OAUTH token,\n# which you can get from here: https://twitchapps.com/tmi/\nusername=\noauth=\n".getBytes());
            }
        }



        SQLiteConfig sqlConfig = new SQLiteConfig();
        SQLiteDataSource sqlDataSource = new SQLiteDataSource();
        sqlDataSource.setUrl("jdbc:sqlite:janna.sqlite");

        sqlCon = sqlDataSource.getConnection();

        PreparedStatement createUserTable = sqlCon.prepareStatement("CREATE TABLE IF NOT EXISTS user ( "
                + " id INTEGER PRIMARY KEY AUTOINCREMENT "
                + ", username VARCHAR(128) UNIQUE"
                + ", voicename VARCHAR(128) DEFAULT 'en-US-Standard-B'"
                + ", voicespeed DOUBLE DEFAULT 1"
                + ", voicepitch DOUBLE DEFAULT 0"
                + ", voicevolume DOUBLE DEFAULT 1"
                + ");"
        );
        createUserTable.execute();

        /*try {
            blindlyExecuteQuery("ALTER TABLE user ADD COLUMN voicevolume DOUBLE DEFAULT 1");
        } catch (Exception ex) {
            debug("Meh: " + ex.toString());
            // Probably already been altered
        }*/

        PreparedStatement createPrefTable = sqlCon.prepareStatement("CREATE TABLE IF NOT EXISTS pref ( "
                + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", name VARCHAR(128) UNIQUE"
                + ");"
        );
        createPrefTable.execute();

        PreparedStatement createUserPrefTable = sqlCon.prepareStatement("CREATE TABLE IF NOT EXISTS user_pref ( "
                + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                + ", user_id INTEGER"
                + ", pref_id INTEGER"
                + ", data VARCHAR(1024)"
                + ");"
        );
        createUserPrefTable.execute();

        addPref("butt_stuff");

        OAuth2Credential credential = new OAuth2Credential("twitch", Creds._password);

        twitch = TwitchClientBuilder.builder()
                .withEnableChat(true)
                .withChatAccount(credential)
                .withEnablePubSub(true)
                .withEnableKraken(true)
                .withEnableTMI(true)
                .withEnableHelix(true)
                .build();

        twitch.getPubSub().connect();

        twitch.getChat().joinChannel("virus610");
        twitch.getChat().joinChannel("notvirus610");

        twitch.getEventManager().onEvent(ChannelMessageEvent.class, this::readMessage);
        //twitch.getChat().sendMessage("virus610", "Butt.");

        KrakenUserList resultList = twitch.getKraken().getUsersByLogin(Arrays.asList("virus610")).execute(); // 28491996
        System.out.println(resultList.getUsers().get(0).getId());

        twitch.getPubSub().listenForChannelPointsRedemptionEvents(credential, "28491996");
        twitch.getEventManager().onEvent(RewardRedeemedEvent.class, this::rewardRedeemed);

        voiceNames.add("en-AU-Standard-A");
        voiceNames.add("en-AU-Standard-C");
        voiceNames.add("en-AU-Standard-B");
        voiceNames.add("en-AU-Standard-D");
        voiceNames.add("en-GB-Standard-A");
        voiceNames.add("en-GB-Standard-C");
        voiceNames.add("en-GB-Standard-F");
        voiceNames.add("en-GB-Standard-B");
        voiceNames.add("en-GB-Standard-D");
        voiceNames.add("en-IN-Standard-A");
        voiceNames.add("en-IN-Standard-D");
        voiceNames.add("en-IN-Standard-B");
        voiceNames.add("en-IN-Standard-C");
        voiceNames.add("en-US-Standard-C");
        voiceNames.add("en-US-Standard-E");
        voiceNames.add("en-US-Standard-G");
        voiceNames.add("en-US-Standard-H");
        voiceNames.add("en-US-Standard-B");
        voiceNames.add("en-US-Standard-D");
        voiceNames.add("en-US-Standard-I");
        voiceNames.add("en-US-Standard-J");

        muteList.add("ircbot610");
        muteList.add("buttsbot");
        muteList.add("saltlogic");

        long ticks = 0;

        try {
            inputReader = new BufferedReader(new InputStreamReader(System.in));

            //Socket socket = new Socket("irc.chat.twitch.tv", 6667);
            socket = new Socket("irc.chat.twitch.tv", 6667);
            socket.setKeepAlive(true);


            channelReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            channelWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            //listenThread = new ListenThread(channelReader, channelWriter, this);
            //listenThread.start();

            // Bad programming practice
            Thread.sleep(1000);

            new Voice("Is Google text to speech working and stuff?", null);
            chat("Beware I live");

        } catch (Exception ex) {
            error("Something broke initializing chat thread stuff", ex);
        }
        finally {
        }

        /*primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                listenThread.alive = false;
                try {
                    socket.shutdownInput();
                } catch (Exception ex) {  }

                try { inputReader.close(); } catch (Exception ex) {  }
                try { channelReader.close(); } catch (Exception ex) {  }
                try { channelWriter.close(); } catch (Exception ex) {  }

                primaryStage.close();
                Platform.exit();
                System.exit(0);
            }
        });*/
    }

    private void sendChat() {
        /*if (!socket.isConnected()) {
            System.out.println("Not connected, apparently!");
        }*/

        String message = inputField.getText();
        inputField.setText("");

        if (message.trim().isEmpty()) {
            return;
        }

        if (message.charAt(0) == '/') {
            String[] split = message.split(" ");
            String cmd = split[0].substring(1).trim();

            if (cmd.equalsIgnoreCase("j") || cmd.equalsIgnoreCase("join")) {
                String channel = split[1].replace("#","");
                twitch.getChat().joinChannel(channel);
            }
        } else {
            sendMessage(message);
            chat(Creds._username + ": " + message);
        }
    }

    void rewardRedeemed(RewardRedeemedEvent event) {
        ChannelPointsRedemption redemption = event.getRedemption();
        String username = redemption.getUser().getDisplayName();
        String reward = redemption.getReward().getTitle();

        User currentUser = getUser(username);
        int redeemed = 0;

        if (reward.equalsIgnoreCase("Change the default TTS voice (random)")) {
            String actuallyDefaultVoice = defaultVoice;
            while (defaultVoice == actuallyDefaultVoice) {
                int rand = new Random().nextInt(voiceNames.size());
                defaultVoice = voiceNames.get(rand);
                System.out.println("New current voice: " + rand + " -> " + defaultVoice);
                redeemed = 1;
            }
        }
        else if (reward.equalsIgnoreCase("TTS: Lower my voice")) {
            if (currentUser.voicePitch > -20) {
                currentUser.voicePitch -= 1;
                currentUser.save();
                redeemed = 1;
            } else {
                sendMessage(username + ": Your voice is as low as it gets!");
            }
        }
        else if (reward.equalsIgnoreCase("TTS: Raise my voice")) {
            if ( currentUser.voicePitch < 20) {
                currentUser.voicePitch += 1;
                currentUser.save();
                redeemed = 1;
            }
            else {
                sendMessage(username + ": I can't raise your voice any higher than this");
            }
        }
        else if (reward.equalsIgnoreCase("TTS: Slow down my voice")) {
            if ( currentUser.voiceSpeed > 0.50) {
                currentUser.voiceSpeed -= 0.50;
                currentUser.save();
                redeemed = 1;
            }
            else {
                sendMessage("nnnnoooooooooooo");
            }
        }
        else if (reward.equalsIgnoreCase("TTS: Speed up my voice")) {
            if ( currentUser.voiceSpeed < 4.0) {
                currentUser.voiceSpeed += 0.25;
                currentUser.save();
                redeemed = 1;
            }
            else {
                sendMessage("NO");
            }
        }
        else if (reward.equalsIgnoreCase("TTS: Set my voice type")) {
            String input = event.getRedemption().getUserInput().trim();
            if (voiceNames.contains(input)) {
                if (!currentUser.voiceName.equalsIgnoreCase(input)) {
                    currentUser.voiceName = input;
                    currentUser.save();
                    redeemed = 1;
                } else {
                    sendMessage("Bruh you're already usin' that voice ("+username+")");
                }
            } else {
                sendMessage("That voice ain't real, yo ("+username+")");
            }
        }
        // If not handled by this bot, don't deal with redemption
        else {
            redeemed = -1;
        }

        if (redeemed != -1) {
            if (redeemed == 1) {
                redemption.setStatus("FULFILLED");
            } else {
                redemption.setStatus("CANCELED");
            }

            /*try {
                twitch.getHelix().cha
                String urlString = "https://api.twitch.tv/helix/channel_points/custom_rewards/redemptions"
                        + "?broadcaster_id=" + oauth // "0123456789"
                        + "&reward_id=" + redemption.getReward().getId() // "<UUID>"
                        + "&id=" + ""; // "<UUID>";
                URL url = new URL(urlString);
            } catch (Exception ex) {
                error("Failed to update redemption status!", ex);
            }*/
        }



        //System.out.println(event.toString());
    }

    public void saveUser(User user) {
        try {
            PreparedStatement update = sqlCon.prepareStatement("UPDATE user SET"
                    + " voicename=?"
                    + ", voicespeed=?"
                    + ", voicepitch=?"
                    + ", voicevolume=?"
                    + " WHERE id=?"
            );
            update.setString(1, user.voiceName);
            update.setDouble(2, user.voiceSpeed);
            update.setDouble(3, user.voicePitch);
            update.setDouble(4,user.voiceVolume);
            update.setInt(5,user.id);
            update.executeUpdate();
        } catch (Exception ex) {
            error("Failed to save user: " + user.name, ex);
        }
    }

    public static void send(String s) {
        try {
            channelWriter.write(s);
            channelWriter.newLine();
            channelWriter.flush();
        } catch (Exception ex) {
            if (s.indexOf("PASS ") != 0) {
                error("Failed to send: " + s, ex);
            } else {
                error("???", ex);
            }
        }
    }

    public static void sendMessage(String s) {
        for (String channel : twitch.getChat().getChannels()) {
            sendMessage(channel, s);
        }
    }

    public static void sendMessage(String channel, String s) {
        twitch.getChat().sendMessage(channel, s);
    }

    public static void writeMessage(String name, String message) {

    }

    public static String butcher(String s, User user) {
        String result = "";
        String[] words = s.split(" ");
        for (String word : words) {
            String lower = word.toLowerCase();
            if (lower.contains("://") || lower.contains("http") || lower.contains("www.") || (lower.matches(".*?[a-z0-9]+\\.[a-z0-9]+\\.[a-z0-9]+.*?") && lower.matches(".*?[a-z].*?"))) {
                word = "link,";
            }
            result += word + " ";
        }


        if (Math.random() > 0.97 && !"0".equals(user.prefs.get("butt_stuff"))) {
            result += ". But enough about my butt.";
        }
        return result;
    }

    public User getUser(String username) {
        username = username.toLowerCase();
        User currentUser = users.get(userIds.get(username));

        // User not in memory yet
        if (null == currentUser) {
            System.out.println("Adding user to memory: " + username);
            try {
                if (null == sqlCon) {
                    return null;
                }
                PreparedStatement select = sqlCon.prepareStatement("SELECT * FROM user WHERE username LIKE ? LIMIT 1");
                select.setString(1, username);
                select.execute();

                ResultSet result = select.getResultSet();
                // If user DNE in DB
                if (!result.next()) {
                    System.out.println("Adding user to DB: " + username);
                    PreparedStatement insert = sqlCon.prepareStatement("INSERT INTO user (username) VALUES (?)");
                    insert.setString(1, username);
                    insert.executeUpdate();
                    insert.close();

                    PreparedStatement select2 = sqlCon.prepareStatement("SELECT * FROM user WHERE username LIKE ? LIMIT 1");
                    select2.setString(1, username);
                    select2.execute();
                    ResultSet result2 = select2.getResultSet();
                    if (result2.next()) {
                        currentUser = new User(
                                this
                                , result2.getInt("id")
                                , result2.getString("username")
                                , result2.getString("voicename")
                                , result2.getDouble("voicespeed")
                                , result2.getDouble("voicepitch")
                                , result2.getDouble("voicevolume")
                        );
                    } else {
                        System.out.println("How the hell can result2 be empty, I literally just inserted the thing I was looking for: " + username);
                    }
                } else {
                    currentUser = new User(
                            this
                            , result.getInt("id")
                            , result.getString("username")
                            , result.getString("voicename")
                            , result.getDouble("voicespeed")
                            , result.getDouble("voicepitch")
                            , result.getDouble("voicevolume")
                    );
                }
                users.put(currentUser.id, currentUser);
                userIds.put(currentUser.name, currentUser.id);
            } catch (Exception ex) {
                error("SQL broke", ex);
            }
        }
        return currentUser;
    }

    private void addPref(String name) {
        try {
            PreparedStatement prefQuery = sqlCon.prepareStatement("INSERT INTO pref (name) SELECT '" + name + "' "
                    + " WHERE NOT EXISTS (SELECT 1 FROM pref WHERE name = '" + name + "');");
            prefQuery.execute();
        } catch (Exception ex) {
            error("Failed to add pref `" + name + "` to DB", ex);
        }

    }

    public HashMap<String, String> getUserPrefs(User user) {
        try {
            HashMap<String, String> prefs = new HashMap<>();
            PreparedStatement userPrefsQuery = sqlCon.prepareStatement("SELECT * FROM user_pref up JOIN pref p ON p.id = up.pref_id WHERE up.user_id = " + user.id +";");
            ResultSet result = userPrefsQuery.executeQuery();
            System.out.println("User prefs for "+user.name+":");
            while (result.next()) {
                System.out.println(result.getString("name") + "=" + result.getString("data"));
                prefs.put(result.getString("name"), result.getString("data"));
            }
            return prefs;
        } catch (Exception ex) {
            error("Messed up getting user prefs", ex);
        }
        return null;
    }

    public boolean setUserPref(User user, String name, String data) {
        try {
            if (data.equals(user.prefs.get(name))) { //
                System.out.println("Pref already set: " + user.name + "." + name + "=" + data);
                return false;
            } else if (null != user.prefs.get(name)) {
                PreparedStatement query = sqlCon.prepareStatement("UPDATE user_pref SET data='"+data+"' "
                        + " WHERE user_id="+user.id+" AND pref_id = (SELECT id FROM pref WHERE name='" + name + "'"
                        + ");");
                query.execute();
            } else {
                PreparedStatement query = sqlCon.prepareStatement("INSERT INTO user_pref (user_id, pref_id, data) VALUES ( "
                        + user.id
                        + ", (SELECT id FROM pref WHERE name='" + name + "')"
                        + ", '" + data + "'"
                        + ");");
                query.execute();
            }
            user.prefs.put(name, data);

        } catch (Exception ex) {
            error("Error setting user pref `"+name+"="+data+"` for `"+user.name+"`", ex);
            return false;
        }
        return true;
    }

    public void getMods() {
        twitch.getChat().sendMessage("virus610", "/mods");
    }

    public void blindlyExecuteQuery(String query) throws Exception {
        PreparedStatement prep = sqlCon.prepareStatement(query);
        prep.execute();
    }

    public static void console(String s, int level) {
        if (level <= 99) {
            chatArea.append("\n" + s);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    public static void debug (String s) {
        console("[DEBUG] " + s, 7);
    }

    public static void info (String s) {
        console("[INFO]  " + s, 6);
    }

    public static void chat (String s) {
        console("" + s, 5);
    }

    public static void warn (String s) {
        console("[WARN]  " + s, 4);
    }

    public static void error(String s, Exception ex) {
        System.err.println(s);
        console("[ERROR] " + s, 3);

        if (null != ex) {
            ex.printStackTrace();

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            console(sw.toString() + "\n", 3);
        }
    }

    private void readMessage(ChannelMessageEvent e) {

        //info("#" + e.getChannel().getName() + ": " + e.getUser().getName() + "> " + e.getMessage());
        String name = e.getUser().getName();
        String channel = e.getChannel().getName();
        User user = getUser(name);
        String message = e.getMessage();
        chat(name + ": " + message);

        if (message.charAt(0) == '!') {
            parseCommand(message, user);
            return;
        }

        String translated = message;

        boolean canSpeak = true;

        if (muteList.contains(user.name.toLowerCase())) {
            debug("Not speaking, user is muted");
            return;
        }

        if (whitelistOnly) {
            if (false/*isOp*/) {

            }
            else if (!whitelist.contains(user.name.toLowerCase())) {
                return;
            }
        }

        if (user.voiceVolume <= 0) {
            return;
        }

        if (canSpeak) {
            voices.add(new Voice(butcher(message, user), user));
        }
        //new Speaker(message).start();
    }

    private void parseCommand(String message, User user) {
        message = message.substring(1);
        String[] split = message.split(" ");
        String cmd = split[0];
        if (cmd.equalsIgnoreCase("no")) {
            //voices.get(0).
        } else if (cmd.equalsIgnoreCase("test"/*stfu*/)) {
            for (String mod : twitch.getMessagingInterface().getChatters("virus610").execute().getModerators()) {
                info("Moderator: " + mod);
            }
            // TODO
        } else if (cmd.equalsIgnoreCase("dontbuttmebro")) {
            if (setUserPref(user, "butt_stuff", "0")) {
                twitch.getChat().sendMessage("virus610", "Okay, I won't butt you, bro.");
            }
        } else if (cmd.equalsIgnoreCase("dobuttmebro")) {
            if (setUserPref(user, "butt_stuff", "1")) {
                twitch.getChat().sendMessage("virus610", "Can't get enough of that butt.");
            }
        } else if (cmd.equalsIgnoreCase("mute")) {
            // TODO
        }
    }
}

