package com.project610;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import com.github.twitch4j.eventsub.domain.RedemptionStatus;
import com.github.twitch4j.helix.domain.CustomReward;
import com.github.twitch4j.pubsub.domain.ChannelPointsRedemption;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.project610.async.*;
import com.project610.commands.*;
import com.project610.structs.JList2;
import com.project610.utils.Util;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.sqlite.SQLiteDataSource;

import javax.swing.*;

import static javax.swing.BoxLayout.LINE_AXIS;

public class Janna extends JPanel {

    public static Janna instance;
    private static final int LOG_LEVEL = 6;
    static BufferedReader inputReader = null;
    static BufferedReader channelReader = null;
    static BufferedWriter channelWriter = null;

    public Socket socket = null;
    public Connection sqlCon;
    public static ResultSet EMPTY_RESULT_SET;

    public static ArrayList<String> messages = new ArrayList<>();
    public static ArrayList<Voice> voices = new ArrayList<>();
    public static ArrayList<String> voiceNames = new ArrayList<>();
    public static String defaultVoice = "en-US-Standard-B";
    public static SpeechQueue speechQueue;
    public static CleanupQueue cleanupQueue;
    public static MessageQueue messageQueue;
    public static SfxPageUploader sfxPageUploader;
    public static SfxUpdateThread sfxUpdateThread;

    public static HashMap<Integer, User> users = new HashMap<>();
    public static HashMap<String, Integer> userIds = new HashMap<>();

    public static com.github.twitch4j.helix.domain.User mainchannel_user;
    public static String[] extraChannels;
    public static TwitchClient twitch;
    private OAuth2Credential credential;

    public HashMap<String, Long> muteList = new HashMap<>();
    public ArrayList<String> whitelist = new ArrayList<>();
    public boolean whitelistOnly = false;

    public TreeMap<String, JRadioButtonMenuItem> emoteHandleMethods = new TreeMap<>();
    public TreeMap<String, JRadioButtonMenuItem> chatReadingMethods = new TreeMap<>();


    // Config stuff
    Path configPath = Paths.get("config.ini");
    public static HashMap<String, String> appConfig = new HashMap<>();
    String appVersion = "";

    public static HashMap<String, String> filterList = new HashMap<>();
    public static TreeMap<String, Sfx> sfxList = new TreeMap<>();
    public static HashMap<String, String> responseList = new HashMap<>();
    public static HashMap<String, HashMap<String,String>> reactionMods = new HashMap<>();
    public static HashSet<String> usedSfx = new HashSet<>();
    public static TreeMap<Integer, BingoSquare> bingoSquares = new TreeMap<>();
    public static HashMap<Integer, BingoSheet> bingoSheets = new HashMap<>();

    //    static String ttsMode = "google"; // Pitch applies during synthesis, sounds better
    static String ttsMode = "se"; // Way more voices, speed/pitch modify SFX, but *may suddenly crash and burn*

    public static HashMap<String,String> commandAliases;
    public static HashMap<String,String> sfxAliases;

    public static boolean sfxDirty = false;


    public Janna(String[] args, JFrame parent) {
        super(new MigLayout("fill, wrap"));

        Janna.instance = this;

        this.parent = parent;

        // Get any info from resources (Such as version number)
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(".properties");

            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("version=")) {
                    appVersion = line.substring(line.indexOf("=") + 1);
                    if (null == appConfig.get("version") || !appConfig.get("version").equalsIgnoreCase(appVersion)) {
                        appConfig.put("version", appVersion);
                        // TODO: Deal with new app versions somehow
//                        writeSettings();
//                        newVersion = true;
                    }
                    parent.setTitle(parent.getTitle().replace("%VERSION%", "v" + appVersion));
                }
            }
        } catch (Exception ex) {
            // If we can't read from resources, we got problems
        }

        try {
            initUI();
        } catch (Exception ex) {
            error("Failed to initUI, game over man", ex);
        }
    }

    public void saveAuthToken(String token) {
        try {
            PreparedStatement ps = Janna.instance.sqlCon.prepareStatement("INSERT INTO auth (token) VALUES (?);");
            ps.setString(1, token);
            ps.execute();
        } catch (Exception ex) {
            error("Failed to save auth token, that's gon' cause problems", ex);
        }

        setAuthToken(token);
    }

    public void setAuthToken(String token) {
        Creds._helixtoken = token;

        postAuth();
    }


    public static JPanel hbox() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        return panel;
    }

    public static JPanel vbox() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
    }

    // UI stuff
    JFrame parent;

    JMenuBar menuBar;

    JPanel midPane;
    public static JTextArea chatArea;
    JScrollPane chatScroll;
    JList2 userList;

    JPanel inputPane;
    JTextField inputField;


    public void initUI() throws Exception {
        removeAll();

        parent.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {
                appConfig.put("windowpos", parent.getLocationOnScreen().x + "," + parent.getLocationOnScreen().y);
                writeSettings();
            }

            @Override
            public void windowClosed(WindowEvent e) {

            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });

        setBackground(new Color(50, 50, 100));

        menuBar = new JMenuBar();
        //add(menuBar);
        parent.setJMenuBar(menuBar);
        menuBar.setLayout(new BoxLayout(menuBar, LINE_AXIS));

        // FILE MENU
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> {
            parent.dispose();
        });
        fileMenu.add(exitItem);

        JMenuItem test = new JMenuItem("test");
        test.addActionListener(e -> {

        });
        fileMenu.add(test);

        // CHAT MENU
        JMenu chatMenu = new JMenu("Chat");
        menuBar.add(chatMenu);

        JMenuItem silenceCurrentItem = new JMenuItem("Kill current message");
        silenceCurrentItem.addActionListener(e -> silenceCurrentVoices());
        chatMenu.add(silenceCurrentItem);

        JMenuItem silenceAllItem = new JMenuItem("Kill all queued messages");
        silenceAllItem.addActionListener(e -> silenceAllVoices());
        chatMenu.add(silenceAllItem);

        chatMenu.add(new JSeparator());

        JMenuItem clearItem = new JMenuItem("Clear chat log");
        clearItem.addActionListener(e -> {
            chatArea.setText("");
        });
        chatMenu.add(clearItem);

        chatMenu.add(new JSeparator());

        JMenuItem channelsItem = new JMenuItem("Set login / channels");
        channelsItem.addActionListener(e -> loginChannelPrompt());
        chatMenu.add(channelsItem);

        JMenu handleEmoteMenu = new JMenu("Emote handling");
        chatMenu.add(handleEmoteMenu);

        emoteHandleMethods.put("Default", new JRadioButtonMenuItem("Default"));
        if (ttsMode.equalsIgnoreCase("google")) {
            emoteHandleMethods.put("Fast emotes", new JRadioButtonMenuItem("Fast emotes"));
        }
        emoteHandleMethods.put("First only", new JRadioButtonMenuItem("First only"));
        emoteHandleMethods.put("First of each", new JRadioButtonMenuItem("First of each"));
        for (String key : emoteHandleMethods.keySet()) {
            JRadioButtonMenuItem method = emoteHandleMethods.get(key);
            method.addActionListener(e -> setEmoteHandleMethod(method.getText()));
            if (key.equalsIgnoreCase(appConfig.get("emotehandle"))) {
                setEmoteHandleMethod(method.getText());
            }
            handleEmoteMenu.add(method);
        }

        JMenu chatReadingMenu = new JMenu("Chat reading");
        chatMenu.add(chatReadingMenu);

        chatReadingMethods.put("Default", new JRadioButtonMenuItem("Default"));
        chatReadingMethods.put("Whitelist only", new JRadioButtonMenuItem("Whitelist only"));
        chatReadingMethods.put("SFX only", new JRadioButtonMenuItem("SFX only"));
        for (String key : chatReadingMethods.keySet()) {
            JRadioButtonMenuItem method = chatReadingMethods.get(key);
            method.addActionListener(e -> setChatReadingMethod(method.getText()));
            if (key.equalsIgnoreCase(appConfig.get("chatreading"))) {
                setChatReadingMethod(method.getText());
            }
            chatReadingMenu.add(method);
        }

        // CONFIG MENU
        JMenu configMenu = new JMenu("Config");
        menuBar.add(configMenu);

        JMenu ffmpegItem = new JMenu("FFMPEG");
        configMenu.add(ffmpegItem);

        JMenuItem downloadFfmpegItem = new JMenuItem("Download FFMPEG");
        ffmpegItem.add(downloadFfmpegItem);
        downloadFfmpegItem.addActionListener(e -> {
            try {
                // Make ffmpeg dir if DNE
                File dir = new File("ffmpeg");
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String zipPath = "temp/ffmpeg.zip";

                switch (Util.getOS()) {
                    case WINDOWS:
                        String outPath = "ffmpeg/ffmpeg.exe";
                        // TODO: Do this in separate thread so program doesn't hang
                        if (!Files.exists(Paths.get(zipPath))) {
                            FileUtils.copyURLToFile(
                                    new URL("https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip"),
                                    new File(zipPath)
                            );
                        }
                        if (extractFile(zipPath, "ffmpeg.exe", outPath)) {
                            appConfig.put("ffmpegpath", outPath);
                        }
                        break;
                    case LINUX:
                        Desktop.getDesktop().browse(new URI("https://ffmpeg.org/download.html#build-linux"));
                        return;
                    case MAC:
                        Desktop.getDesktop().browse(new URI("https://ffmpeg.org/download.html#build-mac"));
                        return;
                    default:
                        warn("Unrecognized OS: " + System.getProperty("os.name").toLowerCase());
                }
            } catch (URISyntaxException ex) {
                error("Error parsing URI for FFMPEG download", ex);
            } catch (IOException ex) {
                error("Error visiting URL to get FFMPEG", ex);
            }
        });


        JMenuItem locateFfmpegItem = new JMenuItem("Locate FFMPEG Executable");
        ffmpegItem.add(locateFfmpegItem);
        locateFfmpegItem.addActionListener(e -> {
            JDialog ffmpegDialog = new JDialog(parent, "Locate FFMPEG (The file, not directory)", true);
            ffmpegDialog.setLocation(getPopupLocation());
            ffmpegDialog.setSize(700, 400);
            JFileChooser chooser = new JFileChooser(".");
            ffmpegDialog.add(chooser);

            String[] selected = {""}; // This seems silly, but eh
            chooser.addActionListener(f -> {
                if (f.getActionCommand().equals(JFileChooser.APPROVE_SELECTION)) {
                    selected[0] = chooser.getSelectedFile().getAbsolutePath();
                    ffmpegDialog.dispose();
                } else if (f.getActionCommand().equals(JFileChooser.CANCEL_SELECTION)) {
                    ffmpegDialog.dispose();
                }
            });
            ffmpegDialog.setVisible(true);
            if (!selected[0].isEmpty()) {
                appConfig.put("ffmpegpath", selected[0]);
            }
        });

        configMenu.add(new JSeparator());

        JMenuItem clearAuthTokenItem = new JMenuItem("Clear Auth Token Data");
        configMenu.add(clearAuthTokenItem);
        clearAuthTokenItem.addActionListener(e -> {
            try {
                int status = Janna.instance.executeUpdate("DELETE FROM auth");
                if (status > 0) {
                    info("Deleted auth token data; You should be prompted to log in again now.");
                } else {
                    info("Didn't find any auth token data to delete");
                }
                Auth.getToken();

            } catch (Exception ex) {
                error("Error clearing auth token data!", ex);
            }
        });

        // SFX MENU
        JMenu sfxMenu = new JMenu("SFX");
        menuBar.add(sfxMenu);

        JMenuItem sfxListPageItem = new JMenuItem("SFX List Webpage Config");
        sfxListPageItem.addActionListener(e -> sfxPagePrompt());
        sfxMenu.add(sfxListPageItem);

        // MENUBAR DONE
        menuBar.add(Box.createHorizontalGlue());

        // Mid pane to hold chat area, user list, input box, and send button (h-box)
        midPane = new JPanel(new MigLayout("fill"));
        midPane.setBackground(new Color(50, 50, 100));
        add(midPane, "grow");

        // Chat pane holds all but the user list (v-box)
        JPanel chatPane = new JPanel(new MigLayout("fill"));
        chatPane.setBackground(new Color(50, 50, 100));
        midPane.add(chatPane, "grow");

        chatArea = new JTextArea();
        //chatArea.setPreferredSize(new Dimension(300, 100));
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setFont(chatArea.getFont().deriveFont(11f));
        chatScroll = new JScrollPane(chatArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        //chatScroll.setPreferredSize(new Dimension(300,400));
        chatPane.add(new JLabel(" "), "north");
        chatPane.add(chatScroll, "grow");

        JPanel userListPane = new JPanel(new MigLayout("filly"));
        userListPane.setBackground(new Color(50, 50, 100));
        midPane.add(userListPane, "east, grow");

        JLabel chattersLabel = new JLabel("<html><font color=white><b>// Chatters</b></font></html>");
        userListPane.add(chattersLabel, "north");

        userList = new JList2<String>();
        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userList.setPrototypeCellValue("___________________");
        userListPane.add(new JScrollPane(userList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS), "growy");

        inputPane = new JPanel(new MigLayout("fillx"));
        inputPane.setBackground(new Color(50, 50, 100));
        add(inputPane, "south, h 30");

        inputField = new JTextField();
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

        inputPane.add(inputField, "grow"); // This should be in yet another pane??
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendChat());
        inputPane.add(sendButton, "east, w 50");
    }

    private boolean extractFile(String zipPath, String file, String output) {
        try (
                FileSystem fileSystem = FileSystems.newFileSystem(Paths.get(zipPath), null);
                FileInputStream fileIn = new FileInputStream(zipPath);
                ZipInputStream zipIn = new ZipInputStream(fileIn)
        ) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                if (!entry.isDirectory()
                        && entry.getName().substring(entry.getName().lastIndexOf('/') + 1).equalsIgnoreCase(file)) {
                    Files.copy(fileSystem.getPath(entry.getName()), Paths.get(output), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            }
        } catch (Exception ex) {
            error("Failed to unzip file: " + file, ex);
        }
        return false;
    }


//    private void setLogin(String username, String oauth) {
//        try {
//            main = main.trim().toLowerCase();
//            extra = extra.trim().replaceAll(" ", "");
//            if (!appConfig.get("extrachannels").equalsIgnoreCase(extra.trim())) {
//                String[] extras = extra.toLowerCase().split(",");
//                for (String channel : twitch.getChat().getChannels()) {
//                    if (!channel.equalsIgnoreCase(main) && !Arrays.asList(extras).contains(channel.toLowerCase())) {
//                        System.out.println("Leaving " + channel);
//                        twitch.getChat().leaveChannel(channel);
//                    }
//                }
//                for (String channel : extras) {
//                    try {
//                        twitch.getChat().joinChannel(channel);
//                    } catch (Exception ex) {
//                        error("Failed to join channel: " + channel, ex);
//                    }
//                }
//                appConfig.put("extrachannels", extra);
//            }
//            if (!appConfig.get("mainchannel").equalsIgnoreCase(main.trim())) {
//                appConfig.put("mainchannel", main);
//                writeSettings();
//                if (null != appConfig.get("oauth")) { // Sign in if credentials are in place
//                    twitch.close();
//                    init();
//                }
//            }
//        } catch (Exception ex) {
//            error("Error setting channels", ex);
//        }
//    }

    public Point getPopupLocation() {
        return new Point((int) parent.getLocation().getX() + 80, (int) parent.getLocation().getY() + 80);
    }

    private void loginChannelPrompt() {
        JDialog channelDialog = new JDialog(parent, "Set Login/Channels", true);
        channelDialog.setLocation(getPopupLocation());
        channelDialog.setLayout(new MigLayout("fillx, w 600"));

        JPanel inputPanel = new JPanel(new MigLayout("fillx"));
        channelDialog.add(inputPanel, "growx");

        inputPanel.add(new JLabel("Bot username"));
        JTextField usernameField = new JTextField(appConfig.get("username"));
        inputPanel.add(usernameField, "growx, pushx, wrap");

        inputPanel.add(new JLabel("Bot oauth token"));
        JPasswordField oauthField = new JPasswordField(appConfig.get("oauth"));
        inputPanel.add(oauthField, "growx, pushx, wrap");

        inputPanel.add(Box.createRigidArea(new Dimension(25, 25)), "wrap");

        inputPanel.add(new JLabel("Main channel"));
        JTextField mainField = new JTextField(appConfig.get("mainchannel"));
        inputPanel.add(mainField, "growx, pushx, wrap");

        inputPanel.add(new JLabel("Extra channels"));
        JTextField extraField = new JTextField(appConfig.get("extrachannels"));
        inputPanel.add(extraField, "growx, pushx, wrap");

        JPanel buttonPanel = new JPanel(new MigLayout());
        channelDialog.add(buttonPanel, "south");
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            channelDialog.dispose();
            setLoginAndChannels(usernameField.getText(), oauthField.getPassword(), mainField.getText(), extraField.getText());
        });
        buttonPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> channelDialog.dispose());
        buttonPanel.add(cancelButton);

        usernameField.addActionListener(okButton.getActionListeners()[0]);
        oauthField.addActionListener(okButton.getActionListeners()[0]);
        mainField.addActionListener(okButton.getActionListeners()[0]);
        extraField.addActionListener(okButton.getActionListeners()[0]);

        channelDialog.pack();
        channelDialog.setVisible(true);
    }

    private void setLoginAndChannels(String username, char[] password, String main, String extra) {
        boolean reinit = false;
        String oauth = new String(password);
        // Update login
        try {
            if (!username.equalsIgnoreCase(appConfig.get("username")) || !oauth.equalsIgnoreCase(appConfig.get("oauth"))) {
                appConfig.put("username", username);
                appConfig.put("oauth", oauth);
                reinit = true;
            }
        } catch (Exception ex) {
            error("Failed to store new login credentials", ex);
        }

        // Update channels
        try {
            main = main.trim().toLowerCase();
            extra = extra.trim().replaceAll(" ", "");
            if (null == twitch) {
                appConfig.put("mainchannel", main);
                appConfig.put("extrachannels", extra);
                reinit = true;
            } else {
                if (!extra.trim().equalsIgnoreCase(appConfig.get("extrachannels"))) {
                    String[] extras = extra.toLowerCase().split(",");
                    for (String channel : twitch.getChat().getChannels()) {
                        if (!channel.equalsIgnoreCase(main) && !Arrays.asList(extras).contains(channel.toLowerCase())) {
                            System.out.println("Leaving " + channel);
                            twitch.getChat().leaveChannel(channel);
                        }
                    }
                    for (String channel : extras) {
                        try {
                            twitch.getChat().joinChannel(channel);
                        } catch (Exception ex) {
                            error("Failed to join channel: " + channel, ex);
                        }
                    }
                    appConfig.put("extrachannels", extra);
                }
                if (!main.trim().equalsIgnoreCase(appConfig.get("mainchannel"))) {
                    appConfig.put("mainchannel", main);
                    reinit = true;
                }
            }
        } catch (Exception ex) {
            error("Error setting channels", ex);
        }

        try {
            if (reinit) { // Sign in if credentials are in place
                writeSettings();
                if (null != twitch) twitch.close();
                init();
            }
        } catch (Exception ex) {
            error("Failed to re-init after setting login/channels", ex);
        }
    }

    private void sfxPagePrompt() {
        JDialog sfxPageDialog = new JDialog(parent, "SFX List Webpage Config", true);
        sfxPageDialog.setLocation(getPopupLocation());
        sfxPageDialog.setLayout(new MigLayout("fillx, w 600"));

        JPanel inputPanel = new JPanel(new MigLayout("fillx"));
        sfxPageDialog.add(inputPanel, "growx");

        inputPanel.add(new JLabel("In order to display SFX without flooding chat, you can have a simple webpage "
                + "uploaded to a webserver for users to look at whenever they want"), "span, wrap");

        inputPanel.add(Box.createRigidArea(new Dimension(25, 25)), "wrap");

        inputPanel.add(new JLabel("FTP Hostname"));
        JTextField hostnameField = new JTextField(appConfig.get("sfx_ftp_hostname"));
        inputPanel.add(hostnameField, "growx, pushx, wrap");

        inputPanel.add(new JLabel("FTP Username"));
        JTextField usernameField = new JTextField(appConfig.get("sfx_ftp_username"));
        inputPanel.add(usernameField, "growx, pushx, wrap");

        inputPanel.add(new JLabel("FTP Password"));
        JPasswordField passwordField = new JPasswordField(appConfig.get("sfx_ftp_password"));
        inputPanel.add(passwordField, "growx, pushx, wrap");

        inputPanel.add(new JLabel("FTP Path"));
        JTextField pathField = new JTextField(appConfig.get("sfx_ftp_path"));
        inputPanel.add(pathField, "growx, pushx, wrap");

        inputPanel.add(Box.createRigidArea(new Dimension(25, 25)), "wrap");

        inputPanel.add(new JLabel("Webpage URL"));
        JTextField urlField = new JTextField(appConfig.get("sfx_page_url"));
        inputPanel.add(urlField, "growx, pushx, wrap");

        inputPanel.add(new JLabel("Webpage Enabled"));
        JCheckBox enabledBox = new JCheckBox();
        inputPanel.add(enabledBox, "growx, pushx, wrap");
        if (appConfig.get("sfx_page_enabled").equals("1")) {
            enabledBox.setSelected(true);
        }

        JPanel buttonPanel = new JPanel(new MigLayout());
        sfxPageDialog.add(buttonPanel, "south");
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            sfxPageDialog.dispose();
            setSfxPageStuff(hostnameField.getText(), usernameField.getText(), new String(passwordField.getPassword()),
                    pathField.getText(), urlField.getText(), enabledBox.isSelected());
        });
        buttonPanel.add(okButton);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> sfxPageDialog.dispose());
        buttonPanel.add(cancelButton);

        usernameField.addActionListener(okButton.getActionListeners()[0]);
        passwordField.addActionListener(okButton.getActionListeners()[0]);
        pathField.addActionListener(okButton.getActionListeners()[0]);
        urlField.addActionListener(okButton.getActionListeners()[0]);

        sfxPageDialog.pack();
        sfxPageDialog.setVisible(true);
    }

    private void setSfxPageStuff(String hostname, String username, String password, String path, String url, boolean enabled) {
        appConfig.put("sfx_ftp_hostname", hostname);
        appConfig.put("sfx_ftp_username", username);
        appConfig.put("sfx_ftp_password", password);
        appConfig.put("sfx_ftp_path", path);
        appConfig.put("sfx_page_url", url);
        appConfig.put("sfx_page_enabled", enabled ? "1" : "0");
        writeSettings();

        if (enabled) {
            uploadSfxListPage();
        }
    }

    public void uploadSfxListPage() {
        FTPClient ftp = new FTPClient();
        try {
            File f = generateSfxPage();

            ftp.connect(appConfig.get("sfx_ftp_hostname"));
            boolean login = ftp.login(appConfig.get("sfx_ftp_username"), appConfig.get("sfx_ftp_password"));
            ftp.setControlEncoding("UTF-8");
            ftp.setFileType(FTPClient.BINARY_FILE_TYPE);
            FileInputStream f_in = new FileInputStream(f);
            ftp.storeFile(appConfig.get("sfx_ftp_path") + "/sfxlist.html", f_in);
            f_in.close();
            trace(ftp.getReplyString());
            ftp.disconnect();
            info("Uploaded SFX List Page to FTP; Link: " + appConfig.get("sfx_page_url"));
        } catch (Exception ex) {
            error("Failed to upload SFX List page", ex);
        }
    }

    private File generateSfxPage() throws IOException {
        Path p = Paths.get("temp/sfxlist.html");
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        sb.append("<head>")
                .append("<meta charset='utf-8'>")
                .append("<meta http-equiv='Cache-Control' content='no-cache, no-store, must-revalidate' />")
                .append("<meta http-equiv='Pragma' content='no-cache' />")
                .append("<meta http-equiv='Expires' content='0' />")
                .append("<title>Janna SFX List</title>")
                .append("<style type='text/css'>")
                .append("body { margin: 0px; padding: 0px; background: #222; color: #ddd; font-family: Tahoma; }")
                .append("a { color: #ddd; }")
                .append("a:visited { color: #ccc; }")
                .append(".row { margin-bottom: 5px; }")
                .append(".cell { padding: 3px 10px; overflow-wrap: break-word; }")
                .append(".cellheader { font-weight: bold; background: #66d; color: #fff; }")
                .append(".copy { border:none; background: none; cursor: pointer; }")
                .append(".alias { border: 1px dashed #888; border-radius: 4px; margin: 0px 3px; padding: 1px 2px; }")
                .append("</style>")
                .append("<script src='https://www.project610.com/janna/libs/sorttable.js'></script>")
                .append("</head>");

        sb.append("<body>");

        sb.append("<table class='sortable'>")
                .append("<thead>")
                .append("<tr>")
                .append("<th>&nbsp;</th>")
                .append("<th>Phrase</th>")
                .append("<th>Mods</th>")
                .append("<th>Aliases</th>")
                .append("<th>Uses</th>")
                .append("<th>Created</th>")
                .append("</tr>")
                .append("</thead>");

        sb.append("<tbody>");
        int rows = 0;
        for (String key : sfxList.keySet()) {
            Sfx sfx = sfxList.get(key);
            String background = rows%2==0?"background:#003":"background:#001";

            // Row
            sb.append("<tr class='row' style='")
                    .append(background)
                    .append("'>");

            sb.append("<td class='cell' style='width: 40px;'>")
                    .append("<input type='button' class='copy' value='📋' onclick='navigator.clipboard.writeText(\"")
                    .append(key)
                    .append("\");'/>")
                    .append("</td>");

            sb.append("<td class='cell' style='width: 150px;'>");

            String url = sfx.url;
            if (!url.equalsIgnoreCase("multi")) {
                sb.append("<a href='")
                        .append(url)
                        .append("'>");
            }
            sb.append(key);
            if (!url.equalsIgnoreCase("multi")) {
                sb.append("</a>");
            }
            sb.append("</td>");

            sb.append("<td class='cell' style='width: 200px;'>");
            HashMap<String,String> mods = sfx.mods;
            for (String mod : mods.keySet()) {
                sb.append(mod)
                        .append(" = ")
                        .append(mods.get(mod))
                        .append("<br/>");
            }
            sb.append("</td>");

            sb.append("<td class='cell' style='width: 150px;'>");
            for (String alias : sfx.aliases) {
                sb.append("<span class='alias'>")
                        .append(alias)
                        .append("</span>");
            }
            sb.append("</td>");

            sb.append("<td class='cell' style='width: 50px;'>")
                    .append(sfx.uses)
                    .append("</td>");

            sb.append("<td class='cell' style='width: 200px;'>")
                    .append(sfx.created)
                    .append("</td>");

            // End row
            sb.append("</tr>");
            rows++;
        }
        sb.append("</tbody></table></body></html>");
        Files.write(p, sb.toString().getBytes());

        File f = new File("temp/sfxlist.html");
        return f;
    }

    private void writeSettings() {
        for (String key : appConfig.keySet()) {
            try {
                PreparedStatement prep = sqlCon.prepareStatement("INSERT OR REPLACE INTO appconfig (param, value) VALUES (?, ?);");
                prep.setString(1, key);
                prep.setString(2, appConfig.get(key));
                prep.executeUpdate();
            } catch (SQLException ex) {
                error("Error writing app config to DB", ex);
            }
        }
    }

    private void setEmoteHandleMethod(String newMethod) {
        if (null == newMethod) {
            newMethod = "Default";
        }
        for (String key : emoteHandleMethods.keySet()) {
            if (key.equalsIgnoreCase(newMethod)) {
                appConfig.put("emotehandle", newMethod);
                emoteHandleMethods.get(key).setSelected(true);
            } else {
                emoteHandleMethods.get(key).setSelected(false);
            }
        }
    }

    private void setChatReadingMethod(String newMethod) {
        if (null == newMethod) {
            newMethod = "Default";
        }
        for (String key : chatReadingMethods.keySet()) {
            if (key.equalsIgnoreCase(newMethod)) {
                appConfig.put("chatreading", newMethod);
                chatReadingMethods.get(key).setSelected(true);
            } else {
                chatReadingMethods.get(key).setSelected(false);
            }
        }
    }

    // Lazy UI stuff, will eventually obsolete this crap
    public static Component prefSize(Component component, int w, int h) {
        component.setPreferredSize(new Dimension(w, h));
        return component;
    }

    public static Component maxSize(Component component, int w, int h) {
        component.setMaximumSize(new Dimension((w == -1 ? Integer.MAX_VALUE : w), (h == -1 ? Integer.MAX_VALUE : h)));
        return component;
    }

    public static Component rigidSize(Component component, int w, int h) {
        component.setMinimumSize(new Dimension(w, h));
        component.setPreferredSize(new Dimension(w, h));
        component.setMaximumSize(new Dimension(w, h));
        component.setSize(new Dimension(w, h));
        return component;
    }

    public void init() throws Exception {

        initDB();

        appConfig = new HashMap<>();

        // Get app settings
//        readAppConfig();
        loadAppConfig();
        loadCommands();

        // SpeechQueue started; Will ready up and play voices as they come
        //  if allowConsecutive is true, different people can 'talk' at the same time
        speechQueue = new SpeechQueue(true);
        new Thread(speechQueue).start();

        // Start a thread to clean up old temp files
        cleanupQueue = new CleanupQueue();
        new Thread(cleanupQueue).start();

        // Start a thread to write potentially spammy messages
        messageQueue = new MessageQueue();
        new Thread(messageQueue).start();

        // Start a thread to update SFX usages periodically
        sfxUpdateThread = new SfxUpdateThread();
        new Thread(sfxUpdateThread).start();

        commandAliases = new HashMap<>();
        sfxAliases = new HashMap<>();


        // Load stuff from DB
        loadReactions();
        loadMuteList();
        loadAliases();
        loadBingoStuff();

        sfxPageUploader = new SfxPageUploader();
        new Thread(sfxPageUploader).start();

        // Get credentials for logging into chat
        try {
            credential = new OAuth2Credential("twitch", Creds._password);
        } catch (Exception ex) {
            warn("Failed to log into chat. Enter username/oauth for chat, and specify a main channel");
            loginChannelPrompt();
            return;
        }

        twitch = TwitchClientBuilder.builder()
                .withClientId(Creds._clientid)
                .withEnableChat(true)
                .withChatAccount(credential)
                .withEnablePubSub(true)
                .withEnableKraken(true)
                .withEnableTMI(true)
                .withEnableHelix(true)
                .build();

        twitch.getPubSub().connect();

        // Join channels
        joinChannel(appConfig.get("mainchannel"));
        if (null != appConfig.get("extrachannels")) {
            for (String extraChannel : appConfig.get("extrachannels").split(",")) {
                joinChannel(extraChannel.toLowerCase().trim());
            }
        }

        twitch.getEventManager().onEvent(ChannelMessageEvent.class, this::readMessage);

        // Get Helix auth token (With Kraken api dead, this is really critical)
        Auth.getToken();

        if (ttsMode.equalsIgnoreCase("google")) {
            voiceNames.addAll(Arrays.asList
                    ("en-AU-Standard-A", "en-AU-Standard-C", "en-AU-Standard-B", "en-AU-Standard-D", "en-GB-Standard-A",
                            "en-GB-Standard-C", "en-GB-Standard-F", "en-GB-Standard-B", "en-GB-Standard-D",
                            "en-IN-Standard-A", "en-IN-Standard-D", "en-IN-Standard-B", "en-IN-Standard-C",
                            "en-US-Standard-C", "en-US-Standard-E", "en-US-Standard-G", "en-US-Standard-H",
                            "en-US-Standard-B", "en-US-Standard-D", "en-US-Standard-I", "en-US-Standard-J"));
        } else if (ttsMode.equalsIgnoreCase("se")) {
            voiceNames.addAll(Arrays.asList
                    ("Filiz", "Astrid", "Tatyana", "Maxim", "Carmen", "Ines", "Cristiano", "Vitoria", "Ricardo",
                            "Maja", "Jan", "Jacek", "Ewa", "Ruben", "Lotte", "Liv", "Seoyeon", "Takumi", "Mizuki",
                            "Giorgio", "Carla", "Bianca", "Karl", "Dora", "Mathieu", "Celine", "Chantal", "Penelope",
                            "Miguel", "Mia", "Enrique", "Conchita", "Geraint", "Salli", "Matthew", "Kimberly", "Kendra",
                            "Justin", "Joey", "Joanna", "Ivy", "Raveena", "Aditi", "Emma", "Brian", "Amy", "Russell",
                            "Nicole", "Vicki", "Marlene", "Hans", "Naja", "Mads", "Gwyneth", "Zhiyu",
                            /*"es-LA_SofiaVoice", "pt-BR_IsabelaVoice", "en-US_MichaelVoice", "ja-JP_EmiVoice",
                            "en-US_AllisonVoice", "fr-FR_ReneeVoice", "it-IT_FrancescaVoice", "es-ES_LauraVoice",
                            "de-DE_BirgitVoice", "es-ES_EnriqueVoice", "de-DE_DieterVoice", "en-US_LisaVoice",
                            "en-GB_KateVoice", "es-US_SofiaVoice",*/
                            "es-ES-Standard-A", "it-IT-Standard-A", "it-IT-Wavenet-A", "ja-JP-Standard-A", "ja-JP-Wavenet-A",
                            "ko-KR-Standard-A", "ko-KR-Wavenet-A", "pt-BR-Standard-A", "tr-TR-Standard-A", "sv-SE-Standard-A",
                            "nl-NL-Standard-A", "nl-NL-Wavenet-A", "en-US-Wavenet-A", "en-US-Wavenet-B", "en-US-Wavenet-C",
                            "en-US-Wavenet-D", "en-US-Wavenet-E", "en-US-Wavenet-F", "en-GB-Standard-A", "en-GB-Standard-B",
                            "en-GB-Standard-C", "en-GB-Standard-D", "en-GB-Wavenet-A", "en-GB-Wavenet-B", "en-GB-Wavenet-C",
                            "en-GB-Wavenet-D", "en-US-Standard-B", "en-US-Standard-C", "en-US-Standard-D", "en-US-Standard-E",
                            "de-DE-Standard-A", "de-DE-Standard-B", "de-DE-Wavenet-A", "de-DE-Wavenet-B", "de-DE-Wavenet-C",
                            "de-DE-Wavenet-D", "en-AU-Standard-A", "en-AU-Standard-B", "en-AU-Wavenet-A", "en-AU-Wavenet-B",
                            "en-AU-Wavenet-C", "en-AU-Wavenet-D", "en-AU-Standard-C", "en-AU-Standard-D", "fr-CA-Standard-A",
                            "fr-CA-Standard-B", "fr-CA-Standard-C", "fr-CA-Standard-D", "fr-FR-Standard-C", "fr-FR-Standard-D",
                            "fr-FR-Wavenet-A", "fr-FR-Wavenet-B", "fr-FR-Wavenet-C", "fr-FR-Wavenet-D", "da-DK-Wavenet-A",
                            "pl-PL-Wavenet-A", "pl-PL-Wavenet-B", "pl-PL-Wavenet-C", "pl-PL-Wavenet-D", "pt-PT-Wavenet-A",
                            "pt-PT-Wavenet-B", "pt-PT-Wavenet-C", "pt-PT-Wavenet-D", "ru-RU-Wavenet-A", "ru-RU-Wavenet-B",
                            "ru-RU-Wavenet-C", "ru-RU-Wavenet-D", "sk-SK-Wavenet-A", "tr-TR-Wavenet-A", "tr-TR-Wavenet-B",
                            "tr-TR-Wavenet-C", "tr-TR-Wavenet-D", "tr-TR-Wavenet-E", "uk-UA-Wavenet-A", "ar-XA-Wavenet-A",
                            "ar-XA-Wavenet-B", "ar-XA-Wavenet-C", "cs-CZ-Wavenet-A", "nl-NL-Wavenet-B", "nl-NL-Wavenet-C",
                            "nl-NL-Wavenet-D", "nl-NL-Wavenet-E", "en-IN-Wavenet-A", "en-IN-Wavenet-B", "en-IN-Wavenet-C",
                            "fil-PH-Wavenet-A", "fi-FI-Wavenet-A", "el-GR-Wavenet-A", "hi-IN-Wavenet-A", "hi-IN-Wavenet-B",
                            "hi-IN-Wavenet-C", "hu-HU-Wavenet-A", "id-ID-Wavenet-A", "id-ID-Wavenet-B", "id-ID-Wavenet-C",
                            "it-IT-Wavenet-B", "it-IT-Wavenet-C", "it-IT-Wavenet-D", "ja-JP-Wavenet-B", "ja-JP-Wavenet-C",
                            "ja-JP-Wavenet-D", "cmn-CN-Wavenet-A", "cmn-CN-Wavenet-B", "cmn-CN-Wavenet-C", "cmn-CN-Wavenet-D",
                            "nb-no-Wavenet-E", "nb-no-Wavenet-A", "nb-no-Wavenet-B", "nb-no-Wavenet-C", "nb-no-Wavenet-D",
                            "vi-VN-Wavenet-A", "vi-VN-Wavenet-B", "vi-VN-Wavenet-C", "vi-VN-Wavenet-D", "sr-rs-Standard-A",
                            "lv-lv-Standard-A", "is-is-Standard-A", "bg-bg-Standard-A", "af-ZA-Standard-A",
                            "Tracy", "Danny", "Huihui", "Yaoyao", "Kangkang", "HanHan", "Zhiwei", "Asaf", "An",
                            "Stefanos", "Filip", "Ivan", "Heidi", "Herena", "Kalpana", "Hemant", "Matej", "Andika",
                            "Rizwan", "Lado", "Valluvar", "Linda", "Heather", "Sean", "Michael", "Karsten", "Guillaume",
                            "Pattara", "Jakub", "Szabolcs", "Hoda", "Naayf"));
        }

        // Speech/text to verify that Janna is alive and well (Hopefully!)
        // TODO: Make 'initialized' phrase configurable
        if (ttsMode.equalsIgnoreCase("google")) {
            new Voice("Is Google text to speech working and stuff?", null); // Breaks on first-time auth?
        } else if (ttsMode.equalsIgnoreCase("se")) {
            new SeVoice(null, new TTSMessage("message", "Is StreamElements text to speech"), new TTSMessage("message", "working and stuff?"));
        }
        info(Util.currentTime() + " - Beware I live");
    }

    private void initDB() throws Exception {
        if (sqlCon != null) return; // Don't re-init if attempted

        // DB stuff; Maybe dumb to try creating tables every time, but eh. Think of this as a lazy 'liquibase' thing
        SQLiteDataSource sqlDataSource = new SQLiteDataSource();
        sqlDataSource.setUrl("jdbc:sqlite:janna.sqlite");

        sqlCon = sqlDataSource.getConnection();
        PreparedStatement emptyStatement = sqlCon.prepareStatement("SELECT 1 WHERE false");
        emptyStatement.execute();
        EMPTY_RESULT_SET = emptyStatement.getResultSet();

        ResultSet result = executeQuery("PRAGMA user_version;");
        long dbVersion = result.getLong(1);
        debug("dbVersion: " + dbVersion);

        if (dbVersion < 1) {
            executeUpdate("CREATE TABLE IF NOT EXISTS user ( "
                    + " id INTEGER PRIMARY KEY AUTOINCREMENT "
                    + ", username VARCHAR(128) UNIQUE"
                    + ", voicename VARCHAR(128) DEFAULT 'en-US-Standard-B'"
                    + ", voicespeed DOUBLE DEFAULT 1"
                    + ", voicepitch DOUBLE DEFAULT 0"
                    + ", voicevolume DOUBLE DEFAULT 1"
                    + ", freevoice INTEGER DEFAULT 1"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS pref ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", name VARCHAR(128) UNIQUE"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS user_pref ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", user_id INTEGER"
                    + ", pref_id INTEGER"
                    + ", data VARCHAR(1024)"
                    + ");"
            );
            addPref("butt_stuff");
            executeUpdate("CREATE TABLE IF NOT EXISTS auth ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", token VARCHAR(1024)"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS reaction ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", type VARCHAR(128)"
                    + ", phrase VARCHAR(128) UNIQUE"
                    + ", result VARCHAR(1024)"
                    + ", extra VARCHAR(1024)"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS muted_username ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", username VARCHAR(128) UNIQUE"
                    + ", expiry INTEGER"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS appconfig ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", param VARCHAR(128) UNIQUE"
                    + ", value VARCHAR(128)"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS command_alias ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", command VARCHAR(128)"
                    + ", alias VARCHAR(128) UNIQUE"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS sfx_alias ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", sfx VARCHAR(128)"
                    + ", alias VARCHAR(128) UNIQUE"
                    + ");"
            );

            executeUpdate("CREATE TABLE IF NOT EXISTS reaction_mod ( "
                    + "reaction_id INTEGER"
                    + ", mod VARCHAR(128)"
                    + ", data VARCHAR(1024)"
                    + ", UNIQUE(reaction_id, mod)"
                    + " );"
            );

            // Don't @ me, tired of appconfig being the table that shows up first on stream when browsing the DB
            executeUpdate("CREATE TABLE IF NOT EXISTS _ ( "
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + " );"
            );

            executeUpdate("ALTER TABLE reaction ADD COLUMN created_timestamp TEXT;");
            executeUpdate("PRAGMA user_version=1");
        }
        if (dbVersion < 2) {
            executeUpdate("ALTER TABLE reaction ADD COLUMN uses INTEGER DEFAULT 0;");
            executeUpdate("PRAGMA user_version=2");
        }
        if (dbVersion < 3) {
            executeUpdate("CREATE TABLE IF NOT EXISTS bingo_sheet ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", user_id"
                    + ", square_ids VARCHAR(1024)"
                    + ");");

            executeUpdate("CREATE TABLE bingo_square ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT"
                    + ", name VARCHAR(1024) UNIQUE"
                    + ", description VARCHAR(1024)"
                    + ", difficulty INTEGER"
                    + ", state INTEGER"
                    + ");");

            executeUpdate("PRAGMA user_version=3");
        }
    }

    private void loadReactions() {
        try {
            ResultSet result = executeQuery("SELECT * FROM reaction");
            if (result.isClosed()) return;

            do {
                int id = result.getInt("id");
                String type = result.getString("type");
                String key = result.getString("phrase");
                String value = result.getString("result");
                String extra = result.getString("extra");
                String created = result.getString("created_timestamp");
                long uses = result.getLong("uses");

                HashMap<String, String> mods = new HashMap<>();
                reactionMods.put(key, mods);
                ResultSet mod_result = executeQuery("SELECT * FROM reaction_mod WHERE reaction_id=" + id); // TODO: Prep
                if (!mod_result.isClosed()) {
                    do {
                        String mod = mod_result.getString("mod");
                        String data = mod_result.getString("data");
                        mods.put(mod, data);
                    } while (mod_result.next());
                    reactionMods.get(key).putAll(mods); // TODO: Actually test this for multi-type reactions
                                                        // TODO: (Actually test multi-type reactions at all)
                }

                if (type.equalsIgnoreCase("filter")) {
                    filterList.put(key, value);
                } else if (type.equalsIgnoreCase("sfx")) {
                    newSfx(key, new Sfx(value, reactionMods.get(key), created, uses));
                } else if (type.equalsIgnoreCase("response")) {
                    responseList.put(key, value);
                }
            } while (result.next());
        } catch (Exception ex) {
            error("Failed to load reactions from DB", ex);
        }
    }

    public void newSfx(String key, Sfx sfx) {
        sfxList.put(key, sfx);
        sfxDirty = true;
    }

    public void loadBingoStuff() {
        try {
            ResultSet squareResult = executeQuery("SELECT * FROM bingo_square");

            while (squareResult.next()) {
                String squareName = squareResult.getString("name"),
                        squareDescription = squareResult.getString("description");
                int squareId = squareResult.getInt("id"),
                        squareDifficulty = squareResult.getInt("difficulty"),
                        squareState = squareResult.getInt("state");
                bingoSquares.put(squareId, new BingoSquare(squareId, squareName, squareDescription, squareDifficulty, squareState));
            }

            ResultSet sheetResult = executeQuery("SELECT * FROM bingo_sheet");
            while (sheetResult.next()) {
                bingoSheets.put(sheetResult.getInt("user_id"), new BingoSheet(sheetResult.getString("square_ids")));
            }
        } catch (Exception ex) {
            error("Failed to load bingo stuff", ex);
        }
    }

    private void loadAliases() {
        try {
            ResultSet result = executeQuery("SELECT * FROM command_alias");
            if (result.isClosed()) return;

            do {
                String command = result.getString("command");
                String alias = result.getString("alias");

                commandAliases.put(alias, command);
            } while (result.next());
        } catch (Exception ex) {
            error("Failed to load reactions from DB", ex);
        }

        try {
            ResultSet result = executeQuery("SELECT * FROM sfx_alias");
            if (result.isClosed()) return;

            do {
                String sfx = result.getString("sfx");
                String alias = result.getString("alias");

                newSfxAlias(alias, sfx);
            } while (result.next());
        } catch (Exception ex) {
            error("Failed to load reactions from DB", ex);
        }
    }

    private void newSfxAlias(String alias, String sfx) {
        try {
            sfxAliases.put(alias, sfx);
            sfxList.get(sfx).aliases.add(alias);
        } catch (NullPointerException ex) {
            warn("NPE while adding SFX Alias, probably an orphaned record. SFX: " + sfx + ", Alias: " + alias);
        }
    }

    private void loadMuteList() {
        try {
            ResultSet result = executeQuery("SELECT * FROM muted_username;");
            if (result.isClosed()) return;

            do {
                String username = result.getString("username");
                long expiry = result.getLong("expiry");

                muteList.put(username, expiry);
            } while (result.next());
        } catch (Exception ex) {
            error("Failed to load reactions from DB", ex);
        }
    }

    private void loadAppConfig() {

        try {
            ResultSet result = executeQuery("SELECT * FROM appconfig;");
            if (result.isClosed()) return;

            do {
                String param = result.getString("param");
                String value = result.getString("value");
                appConfig.put(param, value);
            } while (result.next());

            Creds._username = appConfig.get("username");
            Creds._password = appConfig.get("oauth");

            setEmoteHandleMethod(appConfig.get("emotehandle"));
            setChatReadingMethod(appConfig.get("chatreading"));
            setWindowPos(appConfig.get("windowpos"));

        } catch (Exception ex) {
            error("Failed to load appConfig from DB", ex);
        }
    }

    private void setWindowPos(String windowpos) {
        if (null == windowpos) return;
        try {
            String[] split = windowpos.split(",");
            parent.setLocation(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        } catch (NumberFormatException ex) {
            // Shouldn't be possible, user input not involved
            error("Error setting window position", ex);
        }
    }

    // Yeah
    protected void joinChannel(String channel) {
        try {
            twitch.getChat().joinChannel(channel);
        } catch (Exception ex) {
            error("Couldn't connect to channel: " + channel, ex);
        }
    }

    // Runs after authentication is completed; Can't do Helix API stuff until that's been done
    public void postAuth() {
        try {
            mainchannel_user = twitch.getHelix().getUsers(Creds._helixtoken, null, Arrays.asList(appConfig.get("mainchannel"))).execute().getUsers().get(0);

            twitch.getPubSub().listenForChannelPointsRedemptionEvents(credential, mainchannel_user.getId());
            twitch.getEventManager().onEvent(RewardRedeemedEvent.class, this::rewardRedeemed);

            setupCustomRewards();
        } catch (Exception ex) {
            error("Error doing postAuth stuff", ex);
        }
    }

    // Twitch won't let a bot mark rewards as Redeemed, Canceled, etc. unless the bot made the rewards.
    // TODO: Store titles (Which are used for redemption-handling code) in a HashMap for consistency's sake
    // TODO-maybe: Make this configurable outside the code
    private void setupCustomRewards() {

        generateTTSReward(
                "TTS: Slow down my voice",
                "Make your TTS voice slightly slower",
                256,
                "#3664A1",
                false,
                true
        );

        generateTTSReward(
                "TTS: Speed up my voice",
                "Make your TTS voice slightly faster",
                256,
                "#FF6905",
                false,
                true
        );

        generateTTSReward(
                "TTS: Lower my voice pitch",
                "Make your TTS voice a little deeper",
                256,
                "#FA2929",
                false,
                true
        );

        generateTTSReward(
                "TTS: Raise my voice pitch",
                "Make your TTS voice a little higher",
                256,
                "#58FF0D",
                false,
                true
        );

        generateTTSReward(
                "TTS: Set my voice accent",
                "Pick a base TTS voice from this list (eg: en-GB-Standard-A): https://docs.google.com/spreadsheets/d/1hrhoy3yoLjKE_N_XgHwG8qFAWWxMch6CerqV2xX-XOs",
                4096,
                "#29FAEA",
                true,
                true
        );

        generateTTSReward(
                "TTS: Set voice accent (Free)",
                "You can only do this if you're new here! Pick a base TTS voice from this list (eg: en-GB-Standard-A): https://docs.google.com/spreadsheets/d/1hrhoy3yoLjKE_N_XgHwG8qFAWWxMch6CerqV2xX-XOs",
                300,
                "#94FA3A",
                true,
                true
        );

        generateTTSReward(
                "TTS: Reset my voice (Speed/Pitch)",
                "Instead of paying a bunch of times, pay a bit, one time, to go back to base pitch/speed",
                1024,
                "#350035",
                false,
                true
        );
    }

    // Upload the reward to Twitch
    // TODO: More elegant exception handling, and probably check if it exists before uploading (Sorta the same thing)
    private void uploadCustomReward(CustomReward reward) {
        try {
            twitch.getHelix().createCustomReward(Creds._helixtoken, mainchannel_user.getId(), reward).execute();
        } catch (HystrixRuntimeException ex) {
            debug("Most likely a duplicate, because I was too lazy to check for pre-existing: " + reward.getTitle());
        }
    }

    // Generate a TTS-specific reward. If no modification is desired (eg: From the generateSimplifiedCustomReward root)
    //  then set uploadImmediately=true to just fire and forget
    private CustomReward generateTTSReward(String title, String prompt, int cost, String backgroundColor, boolean inputRequired, boolean uploadImmediately) {
        CustomReward reward = generateSimplifiedCustomReward()
                .title(title)
                .prompt(prompt)
                .cost(cost)
                .backgroundColor(backgroundColor)
                .isUserInputRequired(inputRequired)
                .build();

        if (uploadImmediately) {
            uploadCustomReward(reward);
        }

        return reward;
    }

    // The CustomRewardBuilder has a lot of params that I don't much care about, so this auto-sets some stuff
    private CustomReward.CustomRewardBuilder generateSimplifiedCustomReward() {
        return CustomReward.builder()

                .globalCooldownSetting(CustomReward.GlobalCooldownSetting.builder().globalCooldownSeconds(0).isEnabled(false).build())
                .maxPerStreamSetting(CustomReward.MaxPerStreamSetting.builder().isEnabled(false).maxPerStream(0).build())
                .maxPerUserPerStreamSetting(CustomReward.MaxPerUserPerStreamSetting.builder().isEnabled(false).maxPerUserPerStream(0).build())
                .shouldRedemptionsSkipRequestQueue(false)

                .isEnabled(true)
                .isPaused(false)
                .isInStock(true)

                .broadcasterId(mainchannel_user.getId())
                .broadcasterLogin(mainchannel_user.getLogin())
                .broadcasterName(mainchannel_user.getDisplayName())
                .id(UUID.randomUUID().toString());
    }

    // Handle messages/commands typed into the bot UI - Currently not read out loud, but this could be configurable
    private void sendChat() {
        String message = inputField.getText();
        inputField.setText("");

        if (message.trim().isEmpty()) {
            return;
        }

        if (message.charAt(0) == '/') {
            String[] split = message.split(" ");
            String cmd = split[0].substring(1).trim();

            if (cmd.equalsIgnoreCase("j") || cmd.equalsIgnoreCase("join")) {
                String channel = split[1].replace("#", "");
                twitch.getChat().joinChannel(channel);
            }

        } else {
            sendMessage(appConfig.get("mainchannel"), message);
            chat(Creds._username + ": " + message);
        }
    }

    // Handle redemptions
    void rewardRedeemed(RewardRedeemedEvent event) {
        ChannelPointsRedemption redemption = event.getRedemption();
        String username = redemption.getUser().getDisplayName();
        String reward = redemption.getReward().getTitle();
        String channel = twitch.getChat().getChannelIdToChannelName().get(event.getRedemption().getChannelId());

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
        } else if (reward.equalsIgnoreCase("TTS: Lower my voice pitch")) {
            if (currentUser.voicePitch > -20) {
                currentUser.voicePitch -= 1;
                currentUser.save();
                redeemed = 1;
            } else {
                sendMessage(channel, username + ": Your voice is as low as it gets! (@" + username + ")");
            }
        } else if (reward.equalsIgnoreCase("TTS: Raise my voice pitch")) {
            if (currentUser.voicePitch < 20) {
                currentUser.voicePitch += 1;
                currentUser.save();
                redeemed = 1;
            } else {
                sendMessage(channel, username + ": I can't raise your voice any higher than this (@" + username + ")");
            }
        } else if (reward.equalsIgnoreCase("TTS: Slow down my voice")) {
            if (currentUser.voiceSpeed > 0.75) {
                currentUser.voiceSpeed -= 0.25;
                currentUser.save();
                redeemed = 1;
            } else {
                sendMessage(channel, "Your voice is already minimum speed (@" + username + "");
            }
        } else if (reward.equalsIgnoreCase("TTS: Speed up my voice")) {
            if (currentUser.voiceSpeed < 4.0) {
                currentUser.voiceSpeed += 0.25;
                currentUser.save();
                redeemed = 1;
            } else {
                sendMessage(channel, "Your voice is already max speed (@" + username + "");
            }
        } else if (reward.equalsIgnoreCase("TTS: Set my voice accent")) {
            redeemed = changeUserVoice(currentUser, event.getRedemption().getUserInput().trim(), channel);
        }
        // The way this is intended to work is: New people get 1 'free' accent change, subsequent changes are
        //  considerably more expensive, to keep people from confusing the streamer with constant significant changes
        else if (reward.equalsIgnoreCase("TTS: Set voice accent (Free)")) {
            if (currentUser.freeVoice > 0) {
                redeemed = changeUserVoice(currentUser, event.getRedemption().getUserInput().trim(), channel);
            } else {
                sendMessage(channel, "Scam detected. I'm keeping those channel points.  (@" + currentUser.name + ")");
                redeemed = 1;
            }
        } else if (reward.equalsIgnoreCase("TTS: Reset my voice (Speed/Pitch)")) {
            if (currentUser.voicePitch == 0 && currentUser.voiceSpeed == 1) {
                sendMessage(channel, "@" + username + ", your voice is already at default speed/pitch");
                redeemed = 0;
            } else {
                sendMessage(channel, "@" + username + ", your voice speed/pitch have been reset!");
                currentUser.voicePitch = 0;
                currentUser.voiceSpeed = 1;
                currentUser.save();
                redeemed = 1;
            }
        }
        // If not handled by this bot, don't deal with redemption
        else {
            redeemed = -1;
        }

        // Auto-mark redeemed/canceled for stuff that's been handled
        if (redeemed != -1) {
            Collection<String> redemption_ids = new ArrayList<>();
            redemption_ids.add(redemption.getId());
            if (redeemed == 1) {
                redemption.setStatus("FULFILLED");
                twitch.getHelix().updateRedemptionStatus(Creds._helixtoken, mainchannel_user.getId(),
                        redemption.getReward().getId(), redemption_ids, RedemptionStatus.FULFILLED).execute();
            } else {
                redemption.setStatus("CANCELED");
                twitch.getHelix().updateRedemptionStatus(Creds._helixtoken, mainchannel_user.getId(),
                        redemption.getReward().getId(), redemption_ids, RedemptionStatus.CANCELED).execute();
            }
        }
    }

    // Change accent - Could probably tie the speed/pitch into this later as well. TODO
    public int changeUserVoice(User currentUser, String input, String channel) {
        if (voiceNames.contains(input)) {
            if (!currentUser.voiceName.equalsIgnoreCase(input)) {
                boolean freebieUsed = false;
                currentUser.voiceName = input;
                if (currentUser.freeVoice > 0) {
                    currentUser.freeVoice--;
                    freebieUsed = true;
                }
                currentUser.save();
                // Don't redeem it if a freebie was used, just require the 300 points to motivate follows
                return (freebieUsed ? 0 : 1);
            } else {
                sendMessage(channel, "You're already using that voice (@" + currentUser.name + ")");
            }
        } else {
            sendMessage(channel, "That voice isn't supported (@" + currentUser.name + ")");
        }
        return 0;
    }

    // Write updated user settings to DB
    public void saveUser(User user) {
        try {
            PreparedStatement update = sqlCon.prepareStatement("UPDATE user SET"
                    + " voicename=?"
                    + ", voicespeed=?"
                    + ", voicepitch=?"
                    + ", voicevolume=?"
                    + ", freevoice=?"
                    + " WHERE id=?"
            );
            update.setString(1, user.voiceName);
            update.setDouble(2, user.voiceSpeed);
            update.setDouble(3, user.voicePitch);
            update.setDouble(4, user.voiceVolume);
            update.setDouble(5, user.freeVoice);
            update.setInt(6, user.id);
            update.executeUpdate();
        } catch (Exception ex) {
            error("Failed to save user: " + user.name, ex);
        }
    }

    // Send a message to Twitch TODO: Retry rejected messages if possible
    public static void sendMessage(String s) {
        for (String channel : twitch.getChat().getChannels()) {
            sendMessage(channel, s);
        }
    }

    // Send a message to a specific channel on Twitch
    public static boolean sendMessage(String channel, String s) {
        return twitch.getChat().sendMessage(channel, s);
    }

    // Screw around with the text of a message before having it read aloud (Anti-spam measures will go here)
    public static String butcher(String s, User user) {
        String result = "";
        //s = s.toLowerCase();

        String exclude = "(?!";
        for (String key : sfxList.keySet()) {
            exclude += (exclude == "(?!" ? "" : "|") + key;
        }
        exclude += ")";
        for (String find : filterList.keySet()) {
            String replace = filterList.get(find);
            s = s.replaceAll("\\b(?i)" + exclude + find + "\\b", replace);
        }

        // Sanitize for the API's sake
        s = s.replace("&", "and");
        s = s.replace("%", "percent");
        s = s.replace("#", "");

        int tempWordCount = 0;
        String tempWord = "";
        String[] words = s.split(" ");

        // Anti-spam and anti-annoyance measures
        for (String word : words) {
            // Limit repeated words to 3 in a row
            if (tempWord.equalsIgnoreCase(word)) {
                if (++tempWordCount > 2) {
                    continue;
                }
            } else {
                tempWord = word;
                tempWordCount = 0;
            }

            // Translate sfx aliases
            String actualSfx = instance.getActualSfx(word.toLowerCase());
            if (!word.equalsIgnoreCase(actualSfx)) {
                word = actualSfx;
            }

            // Limit repeated characters to 3 in a row
            try {
                if (word.matches(".*?(?i)(.)(\\1){2,}.*?") && !sfxList.keySet().contains(word.toLowerCase())) {
                    StringBuilder sb = new StringBuilder(word);
                    char tempChar = ' ';
                    int tempCharCount = 0;
                    for (int i = 0; i < sb.length(); i++) {
                        if (Character.toLowerCase(tempChar) == Character.toLowerCase(sb.charAt(i))) {
                            tempCharCount++;
                            if (tempCharCount > 2) {
                                sb.deleteCharAt(i);
                                i--;
                            }
                        } else {
                            tempChar = sb.charAt(i);
                            tempCharCount = 0;
                        }
                        word = sb.toString();
                    }
                }
            } catch (IndexOutOfBoundsException ex) {
                // Didn't realize this could happen on String.matches
                warn("Skipping word for some reason: " + ex);
            }
            result += word + " ";
        }

        // This is super immature. 3% of messages will end with "But enough about my butt", because it's funny.
        // Chatters can opt out of this with the command !dontbuttmebro
        // TODO: Make configurable for people who aren't as childish as me
        if (Math.random() > 0.97 && !"0".equals(user.prefs.get("butt_stuff")) && !sfxList.keySet().contains(instance.getActualSfx(s.trim().toLowerCase()))) {
            result += " But enough about my butt.";
        }
        return result;
    }

    private ResultSet db_getUser(String username) {
        try {
            PreparedStatement select = sqlCon.prepareStatement("SELECT * FROM user WHERE username LIKE ? LIMIT 1");
            select.setString(1, username);
            select.execute();

            return select.getResultSet();

        } catch (Exception ex) {
            return EMPTY_RESULT_SET;
        }
    }

    private User generateNewUser(ResultSet result) {
        try {
            return new User(
                    this
                    , result.getInt("id")
                    , result.getString("username")
                    , result.getString("voicename")
                    , result.getDouble("voicespeed")
                    , result.getDouble("voicepitch")
                    , result.getDouble("voicevolume")
                    , result.getInt("freevoice")
            );
        } catch (Exception ex) {
            return null;
        }
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

                ResultSet result = db_getUser(username);
                // If user DNE in DB
                if (!result.next()) {
                    System.out.println("Adding user to DB: " + username);
                    PreparedStatement insert = sqlCon.prepareStatement("INSERT INTO user (username) VALUES (?)");
                    insert.setString(1, username);
                    insert.executeUpdate();
                    insert.close();

                    ResultSet result2 = db_getUser(username);
                    if (result2.next()) {
                        currentUser = generateNewUser(result2);
                    } else {
                        // This probably isn't an issue anymore
                        warn("How can result2 be empty? I literally just inserted the thing I was looking for: " + username);
                    }
                } else {
                    currentUser = generateNewUser(result);
                }
                users.put(currentUser.id, currentUser);
                userIds.put(currentUser.name, currentUser.id);

                if (!userList.getItems().contains(username)) {
                    userList.add(username);
                }
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
            PreparedStatement userPrefsQuery = sqlCon.prepareStatement("SELECT * FROM user_pref up JOIN pref p ON p.id = up.pref_id WHERE up.user_id = " + user.id + ";");
            ResultSet result = userPrefsQuery.executeQuery();
            System.out.println("User prefs for " + user.name + ":");
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
                PreparedStatement query = sqlCon.prepareStatement("UPDATE user_pref SET data='" + data + "' "
                        + " WHERE user_id=" + user.id + " AND pref_id = (SELECT id FROM pref WHERE name='" + name + "'"
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
            error("Error setting user pref `" + name + "=" + data + "` for `" + user.name + "`", ex);
            return false;
        }
        return true;
    }

    public ResultSet executeQuery(String query) {
        try {
            PreparedStatement getReactions = sqlCon.prepareStatement(query);
            return getReactions.executeQuery();
        } catch (SQLException ex) {
            error("SQL Exception while getting reactions", ex);
        }
        return EMPTY_RESULT_SET;
    }

    public int executeUpdate(String query) throws SQLException {
        PreparedStatement prep = sqlCon.prepareStatement(query);
        return prep.executeUpdate();
    }

    // Logging stuff, should probably move this into its own class
    public static void console(String s, int level) {
        if (level <= Janna.LOG_LEVEL) {
            chatArea.append("\n" + s);
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
    }

    public static void trace(String s) {
        console("[TRACE] " + s, 8);
        System.out.println("[TRACE] " + s);
    }

    public static void debug(String s) {
        console("[DEBUG] " + s, 7);
    }

    public static void info(String s) {
        console("[INFO]  " + s, 6);
    }

    public static void chat(String s) {
        console("" + s, 5);
    }

    public static void warn(String s) {
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

    // Incoming messages handled here
    private void readMessage(ChannelMessageEvent e) {

        String name = e.getUser().getName();
        String channel = e.getChannel().getName();
        User user = getUser(name);

        for (String key : e.getMessageEvent().getBadges().keySet()) {
            String value = e.getMessageEvent().getBadges().get(key);
            user.roles.put(key, value);
        }
        String message = e.getMessage();
        chat(name + ": " + message);

        HashMap<String, String> emotes = getEmotes(e);

        if (message.charAt(0) == '!') {
            parseCommand(message, user, channel);
            return;
        }

        for (String phrase : responseList.keySet()) {
            if (message.toLowerCase().contains(phrase.toLowerCase())) {
                sendMessage(channel, responseList.get(phrase));
            }
        }

        boolean canSpeak = true;

        if (muteList.keySet().contains(user.name.toLowerCase())) {
            // TODO: Check if mute has expired
            debug("Not speaking, user is muted");
            return;
        }

        if (whitelistOnly) {
            if (false/*isOp*/) {

            } else if (!whitelist.contains(user.name.toLowerCase())) {
                return;
            }
        }

        if (user.voiceVolume <= 0) {
            return;
        }

        if (canSpeak) {
            if (false) { // TODO: Read names
                message = user.name + ": " + message;
            }
            if (ttsMode.equalsIgnoreCase("google")) {
                new Voice(ssmlify(butcher(message, user), emotes), user);
            } else if (ttsMode.equalsIgnoreCase("se")) {
                new SeVoice(
                        user,
                        makeTTSMessage(
                                emotes,
                                butcher(message, user)
                        )
                );
            }
        }
        //new Speaker(message).start();
    }

    private TTSMessage[] makeTTSMessage(HashMap<String, String> emotes, String message) {

        ArrayList<TTSMessage> messages = new ArrayList<>();

        int emoteCount = 0;
        for (String emote : emotes.keySet()) {
            emoteCount += emotes.get(emote).split(",").length;
        }

        String emoteHandleMethod = appConfig.get("emotehandle");
        if (!emoteHandleMethod.equalsIgnoreCase("Default") && emotes.keySet().size() > 0) {
            // The more times an emote shows up in a message, the faster it'll be read, to discourage spam, maybe.
            // * Likely not supported using StreamElements TTS
            if (emoteHandleMethod.equalsIgnoreCase("Fast emotes")) {
                for (String emote : emotes.keySet()) {
                    message = message.replace(emote, "<prosody rate=\"" + (150 + 25 * emoteCount) + "%\" volume=\"" + (-2 - 1 * emoteCount) + "dB\">" + emote + "</prosody>");
                }
            } else if (emoteHandleMethod.equalsIgnoreCase("First only")) {
                int firstEmotePos = message.length();
                String firstEmote = "";
                for (String emote : emotes.keySet()) {
                    String[] split = emotes.get(emote).split("(-|,)", 3);
                    try {
                        // Grab the end position of the emote for substring purposes
                        int pos = Integer.parseInt(split[1]);
                        if (pos < firstEmotePos) {
                            firstEmotePos = pos;
                            firstEmote = emote;
                        }
                    } catch (NumberFormatException ex) {
                        // Shouldn't be possible, but eh
                        error("Number format exception finding index of first emote", ex);
                    }
                }
                for (String emote : emotes.keySet()) {
                    if (emote.equalsIgnoreCase(firstEmote)) {
                        String half1 = message.substring(0, firstEmotePos);
                        String half2 = message.substring(firstEmotePos).replaceAll("\\b" + emote + "\\b", "");
                        message = half1 + half2;
                    } else {
                        message = message.replaceAll("\\b" + emote + "\\b", "");
                    }
                }
            } else if (emoteHandleMethod.equalsIgnoreCase("First of each")) {
                for (String emote : emotes.keySet()) {
                    try {
                        Matcher matcher = Pattern.compile(emote).matcher(message);
                        matcher.find();
                        int pos = matcher.start() + emote.length();
                        String half1 = message.substring(0, pos);
                        String half2 = message.substring(pos).replaceAll("\\b" + emote + "\\b", "");
                        message = half1 + half2;
                    } catch (Exception ex) {
                        warn(ex.toString());
                    }
                }
            }
        }


        // Handle sfx - Only play the first to avoid unholy noise spam
        int soundPos = message.length(), cursor = 0;
        String find = "";
        String replace = "";

        for (String key : sfxList.keySet()) {
            cursor = 0;
            boolean matches = message.toLowerCase().contains(key.toLowerCase());
            int index = message.toLowerCase().indexOf(key.toLowerCase());
            if (matches && index < soundPos) {
                String subMessage = message.substring(0, Math.min(message.length(), soundPos)).toLowerCase();
                for (String word : subMessage.split(" ")) {
                    // Only match full words
                    if (word.equalsIgnoreCase(key)) {
                        soundPos = cursor + subMessage.substring(cursor).indexOf(key);
                        find = key;
                        replace = "";
                        break;
                    } else {
                        cursor += 1 + word.length();
                    }
                }
            }
        }

        // Increase usage of SFX, if necessary
        if (!find.isEmpty()) {
            sfxList.get(find).uses++;
            usedSfx.add(find);
        }

        // TODO: Fix more than just `?` on the end of an sfx phrase
        String half1 = message.substring(0, soundPos).trim(),
                half2 = message.substring(soundPos).replaceFirst("(?i)" + find.replace("?", "\\?"), replace).trim();
        if (!half1.isEmpty()) messages.add(new TTSMessage("message", half1));
        if (!find.isEmpty()) {
            messages.add(new TTSMessage("sfx", getSfx(find), find));
        }
        if (!half2.isEmpty()) messages.add(new TTSMessage("message", half2));

        // sfx only mode
        String chatReadingMethod = appConfig.get("chatreading");
        if (chatReadingMethod.equalsIgnoreCase("SFX only")) {
            ArrayList<TTSMessage> messages2 = new ArrayList<>();
            for (TTSMessage ttsMessage : messages) {
                if (ttsMessage.type.equals("sfx")) {
                    messages2.add(ttsMessage);
                }
            }
            return messages2.toArray(new TTSMessage[0]);
        }

        return messages.toArray(new TTSMessage[0]);
    }

    public Sfx getSfx(String phrase) {
        Sfx sfx = sfxList.get(phrase);
        if (sfx.url.equalsIgnoreCase("multi")) {
            ArrayList<Sfx> multiSfxList = new ArrayList<>();
            for (Sfx multiSfx : sfxList.values()) {
                try {
                    // This is hackasaurus rex. If the phrase appears in an SFX's mods with an int value, take that and
                    //  use it as a weight for randomizing. If it's null, it'll barf an error, and frankly I don't care
                    //  what that error is, just move on
                    int weight = Integer.parseInt(multiSfx.mods.get(phrase));
                    for (int i = 0; i < weight; i++) {
                        multiSfxList.add(multiSfx);
                    }
                } catch (Exception ex) {
                    // Ignore stuff that doesn't use number, because I guess that's a weight or something
                }
            }
            sfx = multiSfxList.get((int)(Math.random()*multiSfxList.size()));
        }

        return sfx;
    }

    // Mess with the text to do stuff like read emotes faster, or play sound effects
    //  * Google cloud stuff - Keeping duplicated code for now, but will probably be scrapped
    private String ssmlify(String message, HashMap<String, String> emotes) {
        // Sanitize so peeps don't do bad custom SSML
        message = message.replace("<", "less than").replace(">", "greater than");

        int emoteCount = 0;
        for (String emote : emotes.keySet()) {
            emoteCount += emotes.get(emote).split(",").length;
        }

        String emoteHandleMethod = appConfig.get("emotehandle");
        if (!emoteHandleMethod.equalsIgnoreCase("Default") && emotes.keySet().size() > 0) {
            // The more times an emote shows up in a message, the faster it'll be read, to discourage spam, maybe.
            if (emoteHandleMethod.equalsIgnoreCase("Fast emotes")) {
                for (String emote : emotes.keySet()) {
                    message = message.replace(emote, "<prosody rate=\"" + (150 + 25 * emoteCount) + "%\" volume=\"" + (-2 - 1 * emoteCount) + "dB\">" + emote + "</prosody>");
                }
            } else if (emoteHandleMethod.equalsIgnoreCase("First only")) {
                int firstEmotePos = message.length();
                String firstEmote = "";
                for (String emote : emotes.keySet()) {
                    String[] split = emotes.get(emote).split("(-|,)", 3);
                    try {
                        // Grab the end position of the emote for substring purposes
                        int pos = Integer.parseInt(split[1]);
                        if (pos < firstEmotePos) {
                            firstEmotePos = pos;
                            firstEmote = emote;
                        }
                    } catch (NumberFormatException ex) {
                        // Shouldn't be possible, but eh
                        error("Number format exception finding index of first emote", ex);
                    }
                }
                for (String emote : emotes.keySet()) {
                    if (emote.equalsIgnoreCase(firstEmote)) {
                        String half1 = message.substring(0, firstEmotePos);
                        String half2 = message.substring(firstEmotePos).replaceAll("\\b" + emote + "\\b", "");
                        message = half1 + half2;
                    } else {
                        message = message.replaceAll("\\b" + emote + "\\b", "");
                    }
                }
            } else if (emoteHandleMethod.equalsIgnoreCase("First of each")) {
                for (String emote : emotes.keySet()) {
                    try {
                        Matcher matcher = Pattern.compile(emote).matcher(message);
                        matcher.find();
                        int pos = matcher.start() + emote.length();
                        String half1 = message.substring(0, pos);
                        String half2 = message.substring(pos).replaceAll("\\b" + emote + "\\b", "");
                        message = half1 + half2;
                    } catch (Exception ex) {
                        warn(ex.toString());
                    }
                }
            }
        }

        // Handle sfx - Only play the first to avoid unholy noise spam
        int soundPos = message.length(), cursor = 0;
        String find = "";
        String replace = "";

        for (String key : sfxList.keySet()) {
            cursor = 0;
            boolean matches = message.toLowerCase().contains(key.toLowerCase());
            int index = message.toLowerCase().indexOf(key.toLowerCase());
            if (matches && index < soundPos) {
                String subMessage = message.substring(0, Math.min(message.length(), soundPos)).toLowerCase();
                for (String word : subMessage.split(" ")) {
                    // Only match full words
                    if (word.equalsIgnoreCase(key)) {
                        soundPos = cursor + subMessage.substring(cursor).indexOf(key);
                        find = key;
                        replace = "<audio src=\"" + sfxList.get(find).url + "\" " + sfxList.get(find).extra + ">" + find + "</audio>";
                        break;
                    } else {
                        cursor += 1 + word.length();
                    }
                }
            }
        }

        String half1 = message.substring(0, soundPos), half2 = message.substring(soundPos).replaceFirst("(?i)" + find, replace);
        message = half1 + half2;

        return "<speak>" + message + "</speak>";
    }

    // Gracefully borrowed from the Twitch4J discord server
    private HashMap<String, String> getEmotes(ChannelMessageEvent e) {
        HashMap<String, String> emoteList = new HashMap<>();
        final String msg = e.getMessage();
        final int msgLength = msg.length();
        e.getMessageEvent().getTagValue("emotes")
                .map(emotes -> StringUtils.split(emotes, '/'))
                .ifPresent(emotes -> {
                    for (String emoteStr : emotes) {
                        final int indexDelim = emoteStr.indexOf(':');
                        final String emoteId = emoteStr.substring(0, indexDelim);
                        final String indices = emoteStr.substring(indexDelim + 1);
                        final String[] indicesArr = StringUtils.split(indices, ',');
                        for (String specificIndex : indicesArr) {
                            final int specificDelim = specificIndex.indexOf('-');
                            final int startIndex = Math.max(Integer.parseInt(specificIndex.substring(0, specificDelim)), 0);
                            final int endIndex = Math.min(Integer.parseInt(specificIndex.substring(specificDelim + 1)) + 1, msgLength);
                            final String emoteName = msg.substring(startIndex, endIndex);
                            if (null == emoteList.get(emoteName)) {
                                emoteList.put(emoteName, startIndex + "-" + endIndex);
                            } else {
                                emoteList.put(emoteName, emoteList.get(emoteName) + "," + startIndex + "-" + endIndex);
                            }
                        }
                    }
                });
        return emoteList;
    }

    public HashMap<String, Function> commandMap = new HashMap<>();

    private void loadCommands() {
        commandMap.put("no", new No());
        commandMap.put("stfu", new Stfu());
        commandMap.put("mute", new Mute());
        commandMap.put("unmute", new Unmute());
        commandMap.put("dontbuttmebro", new DontButt());
        commandMap.put("dobuttmebro", new DoButt());
        commandMap.put("voice", new GetVoice());
        commandMap.put("sfx", new SfxList());
        commandMap.put("newsfx", new ListNewSfx());

        commandMap.put("newbingo", new NewBingo());
        commandMap.put("joinbingo", new JoinBingo());
        commandMap.put("bingocheck", new BingoCheck());
        commandMap.put("addbingosquare", new AddBingoSquare());
        commandMap.put("getbingosquare", new GetBingoSquare());
        commandMap.put("bingotoggle", new BingoToggle());
        commandMap.put("removebingosquare", new RemoveBingoSquare());

        commandMap.put("janna.addfilter", new AddFilter());
        commandMap.put("janna.getfilter", new GetFilter());
        commandMap.put("janna.removefilter", new RemoveFilter());

        commandMap.put("janna.addresponse", new AddResponse());
        commandMap.put("janna.getresponse", new GetResponse());
        commandMap.put("janna.removeresponse", new RemoveResponse());

        commandMap.put("janna.addsfx", new AddSfx());
        commandMap.put("janna.getsfx", new GetSfx());
        commandMap.put("janna.modsfx", new ModSfx());
        commandMap.put("janna.replacesfx", new ReplaceSfx());
        commandMap.put("janna.removesfx", new RemoveSfx());

        commandMap.put("janna.addsfxalias", new AddSfxAlias());
        commandMap.put("janna.removesfxalias", new RemoveSfxAlias());

        commandMap.put("janna.addalias", new AddAlias());
        commandMap.put("janna.removealias", new RemoveAlias());

        commandMap.put("janna.voiceusers", new GetVoiceUsers());
    }

    // Incoming messages starting with `!` handled here
    private void parseCommand(String message, User user, String channel) {
        message = message.substring(1);
        String[] split = message.split(" ");
        String cmd = split[0];

        HashMap<String, Object> params = new HashMap<>();
        params.put("message", message);
        params.put("user", user);
        params.put("channel", channel);

        String actualCommand = getCommand(cmd.toLowerCase());

        if (commandMap.get(actualCommand) != null) {
            commandMap.get(actualCommand).apply(params);
        }
    }

    public void addAlias(String message, String channel) {
        try {
            String[] split = message.split(" ");
            String command = split[1].toLowerCase();
            String alias = split[2].toLowerCase();
            if (commandAliases.keySet().contains(alias)) {
                sendMessage("That alias is already in use");
                return;
            }
            if (command.charAt(0) == '!') command = command.replaceFirst("!", "");
            if (alias.charAt(0) == '!') alias = alias.replaceFirst("!", "");
            PreparedStatement prep = sqlCon.prepareStatement("INSERT OR REPLACE INTO command_alias (command, alias) VALUES (?, ?);");
            prep.setString(1, command);
            prep.setString(2, alias);
            if (prep.executeUpdate() > 0) {
                commandAliases.put(alias, command);
                sendMessage(channel, "Added alias; You can now use the command !"+command +" by typing !"+ alias);
            } else {
                sendMessage(channel, "Can't tell ya why, but that addalias command didn't work");
            }
        }
        catch (SQLException ex) {
            if (ex.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]")) {
                sendMessage(channel, "Alias already exists");
            } else {
                sendMessage(channel, "Failed to create alias: " + ex);
            }
        }
        catch (IndexOutOfBoundsException ex) {
            sendMessage(channel, "Malformed command; Usage: !janna.addalias <command> <alias>");
        }
    }

    public void removeAlias(String message, String channel) {
        try {
            String[] split = message.split(" ");
            String alias = split[1].toLowerCase();
            if (alias.charAt(0) == '!') alias = alias.replaceFirst("!", "");
            PreparedStatement prep = sqlCon.prepareStatement("DELETE FROM command_alias WHERE alias=?;");
            prep.setString(1, alias);
            if (prep.executeUpdate() > 0) {
                commandAliases.remove(alias);
                sendMessage(channel, "Removed alias: " + alias);
            }
        }
        catch (SQLException ex) {
            sendMessage(channel, "Failed to remove alias: " + ex);
        }
        catch (IndexOutOfBoundsException ex) {
            sendMessage(channel, "Malformed command; Usage: !janna.removealias <alias>");
        }
    }

    public void addSfxAlias(String message, String channel) {
        try {
            String[] split = message.split(" ");
            String sfx = split[1].toLowerCase();
            String alias = split[2].toLowerCase();
            if (!sfxList.keySet().contains(sfx)) {
                sendMessage("That SFX don't exist, yo");
                return;
            }
            if (sfxAliases.keySet().contains(alias)) {
                sendMessage("That alias is already in use");
                return;
            }
            PreparedStatement prep = sqlCon.prepareStatement("INSERT OR REPLACE INTO sfx_alias (sfx, alias) VALUES (?, ?);");
            prep.setString(1, sfx);
            prep.setString(2, alias);
            if (prep.executeUpdate() > 0) {
                sfxAliases.put(alias, sfx);
                sfxList.get(sfx).aliases.add(alias);
                sfxDirty = true;
                sendMessage(channel, "Added sfx alias; You can now use the sfx `"+sfx +"` by typing `"+ alias + "`");
            } else {
                sendMessage(channel, "Can't tell ya why, but that addSfxAlias command didn't work");
            }
        }
        catch (SQLException ex) {
            if (ex.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]")) {
                sendMessage(channel, "SFX alias already exists");
            } else {
                sendMessage(channel, "Failed to create alias: " + ex);
            }
        }
        catch (IndexOutOfBoundsException ex) {
            sendMessage(channel, "Malformed command; Usage: !janna.addSfxAlias <sfx> <alias>");
        }
    }

    public void removeSfxAlias(String message, String channel) {
        try {
            String[] split = message.split(" ");
            String alias = split[1].toLowerCase();
            String sfx = sfxAliases.get(alias);
            PreparedStatement prep = sqlCon.prepareStatement("DELETE FROM sfx_alias WHERE alias=?;");
            prep.setString(1, alias);
            if (prep.executeUpdate() > 0) {
                sfxAliases.remove(alias);
                sfxList.get(sfx).aliases.remove(alias);
                sfxDirty = true;
                sendMessage(channel, "Removed sfx alias: " + alias);
            }
        }
        catch (SQLException ex) {
            sendMessage(channel, "Failed to remove sfx alias: " + ex);
        }
        catch (IndexOutOfBoundsException ex) {
            sendMessage(channel, "Malformed command; Usage: !janna.removeSfxAlias <alias>");
        }
    }

    // Return the 'root' command, in the event that a user specified an alias
    public String getCommand(String alias) {
        String command = commandAliases.get(alias);
        if (command == null) return alias;
        else return command;
    }

    // Return the 'root' command, in the event that a user specified an alias
    public String getActualSfx(String alias) {
        String sfx = sfxAliases.get(alias);
        if (sfx == null) return alias;
        else return sfx;
    }

    public void getReaction(String type, String message, String channel) {
        try {
            String[] split = message.split(" ");
            String phrase = split[1].toLowerCase();
            PreparedStatement prep = sqlCon.prepareStatement("SELECT * FROM reaction WHERE type=? AND phrase=?;");
            prep.setString(1, type);
            prep.setString(2, phrase);
            ResultSet result = prep.executeQuery();
            String mods = "mods=[";
            prep = sqlCon.prepareStatement("SELECT * FROM reaction_mod WHERE reaction_id = "
                    + " (SELECT id FROM reaction WHERE phrase=?);");
            prep.setString(1, phrase);
            ResultSet modResult = prep.executeQuery();
            if (!modResult.isClosed()) {
                while (modResult.next()) {
                    mods+=modResult.getString("mod") + "=" + modResult.getString("data") + "; ";
                }
            }
            mods += "]";

            switch (type) {
                case "sfx":
                    sendMessage(channel, "[" + phrase + "] url=" + result.getString("result") + ", " + mods);
                    break;
                case "filter":
                    sendMessage(channel, "[" + phrase + "] replacement=" + result.getString("result") + ", " + mods);
                    break;
                case "response":
                    sendMessage(channel, "[" + phrase + "] response=" + result.getString("result") + ", " + mods);
                    break;
                default:
            }
        } catch (SQLException ex) {
            // Meh for now
        } catch (IndexOutOfBoundsException ex) {
            String usage = "!janna.get" + type + " <phrase>";
            sendMessage(channel, "Malformed command; Usage: " + usage);
        }
    }

    public String muteUser(String username, String expiry) {
        username = username.toLowerCase();
        long expiryDate = Long.MAX_VALUE;
        try {
            if (!expiry.equalsIgnoreCase("-1")) {
                expiryDate = System.currentTimeMillis() + (1000 * Long.parseLong(expiry));
                expiry = " for " + expiry + " seconds";
            } else {
                expiry = " indefinitely";
            }
            PreparedStatement prep = sqlCon.prepareStatement("INSERT INTO muted_username ( username, expiry ) VALUES (?, ?)");
            prep.setString(1, username);
            prep.setLong(2, expiryDate);
            if (prep.executeUpdate() > 0) {
                muteList.put(username, expiryDate);
                return username + " muted " + expiry;
            } else {
                return "Well that didn't work";
            }
        } catch (SQLException ex) {
            if (ex.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]")) {
                try {
                    PreparedStatement prep2 = sqlCon.prepareStatement("UPDATE muted_username SET expiry = ? WHERE username = ?");
                    prep2.setLong(1, expiryDate);
                    prep2.setString(2, username);
                    if (prep2.executeUpdate() > 0) {
                        muteList.put(username, expiryDate);
                        return username + " muted " + expiry;
                    } else {
                        return "Well that didn't work";
                    }
                } catch (SQLException ex2) {
                    // Sheesh
                    error("Failed to mute " + username, ex);
                    return "Failed to mute " + username + ": " + ex;
                }
            } else {
                error("Failed to mute " + username, ex);
                return "Failed to mute " + username + ": " + ex;
            }
        } catch (NumberFormatException ex) {
            return "Malformed expiry date, please specify number of seconds, or leave empty for indefinite";
        }
    }

    public String unmuteUser(String username) {
        username = username.toLowerCase();
        try {
            PreparedStatement prep = sqlCon.prepareStatement("DELETE FROM muted_username WHERE username=?");
            prep.setString(1, username);
            if (prep.executeUpdate() > 0) {
                muteList.remove(username);
                return "Unmuted " + username;
            } else {
                return "Well that didn't work";
            }
        } catch (SQLException ex) {
            warn("Failed to unmute " + username + ", probably not muted");
            return "";
        }
    }

    public void silenceCurrentVoices() {
        for (int i = speechQueue.currentlyPlaying.size() - 1; i >= 0; i--) {
            try {
                if (speechQueue.currentlyPlaying.get(i).started) {
                    speechQueue.currentlyPlaying.get(i).clip.stop();
                    speechQueue.currentlyPlaying.get(i).busy = false;
                }
            } catch (Exception ex) {

            }
        }
    }

    public void silenceCurrentVoices(String username) {
        for (int i = speechQueue.currentlyPlaying.size() - 1; i >= 0; i--) {
            try {
                PlaySound current = speechQueue.currentlyPlaying.get(i);
                if (current.started && current.username.equalsIgnoreCase(username)) {
                    current.clip.stop();
                    current.busy = false;
                }
            } catch (Exception ex) {

            }
        }
    }

    public void silenceAllVoices() {
        try {
            for (int i = speechQueue.currentlyPlaying.size() - 1; i >= 0; i--) {
                speechQueue.currentlyPlaying.get(i).clip.stop();
                speechQueue.currentlyPlaying.get(i).busy = false;
            }
        } catch (Exception ex) {

        }
    }

    public void silenceAllVoices(String username) {
        try {
            for (int i = speechQueue.currentlyPlaying.size() - 1; i >= 0; i--) {
                if (speechQueue.currentlyPlaying.get(i).username.equalsIgnoreCase(username)) {
                    speechQueue.currentlyPlaying.get(i).clip.stop();
                    speechQueue.currentlyPlaying.get(i).busy = false;
                }
            }
        } catch (Exception ex) {

        }
    }

    public void addReaction(String type, String message, String channel) {
        String[] split = message.split(" ", 3);
        String phrase = "", result = "";
        try {
            phrase = split[1].toLowerCase();
            result = split[2];
            PreparedStatement prep = sqlCon.prepareStatement("INSERT INTO reaction (type, phrase, result, created_timestamp) VALUES (?, ?, ?, ?);");
            prep.setString(1, type);
            prep.setString(2, phrase);
            prep.setString(3, result);
            prep.setString(4, Util.currentTime());
            prep.executeUpdate();
            switch (type) {
                case "sfx":
                    newSfx(phrase, new Sfx(result, null, Util.currentTime(), 0));
                    sendMessage(channel, "Added SFX for phrase: " + phrase);
                    break;
                case "filter":
                    filterList.put(phrase, result);
                    sendMessage(channel, "Added filter for phrase: " + phrase);
                    break;
                case "response":
                    responseList.put(phrase, result);
                    sendMessage(channel, "Added response to phrase: " + phrase);
                    break;
            }
        } catch (SQLException ex) {
            if (ex.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]")) {
                sendMessage(channel, "A reaction for: `" + split[1] + "` already exists");
            } else {
                error("Failed to insert " + type + ": " + message.split(" ")[1], ex);
                sendMessage(channel, "Failed to add " + type + ": " + ex);
            }
        } catch (IndexOutOfBoundsException ex) {
            warn("add " + type + " command malformatted");
            switch (type) {
                case "sfx":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.addSfx <phrase> <https://__________>` (wav, mp3, ogg)");
                    break;
                case "filter":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.addFilter <phrase> <filtered phrase>`");
                    break;
                case "response":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.addResponse <phrase> <response>`");
                    break;
            }
        }
    }

    public void editReaction(String type, String message, String channel) {
        String[] split = message.split(" ", 3);
        String phrase = "", result = "";
        try {
            phrase = split[1].toLowerCase();
            result = split[2];
            PreparedStatement prep = sqlCon.prepareStatement("UPDATE reaction SET result = ? WHERE type = ? AND phrase = ?;");
            prep.setString(1, result);
            prep.setString(2, type);
            prep.setString(3, phrase);
            prep.executeUpdate();
            switch (type) {
                case "sfx":
                    cleanupQueue.queue.add(Sfx.getFileLocation(sfxList.get(phrase)));
                    newSfx(phrase, new Sfx(result, reactionMods.get(phrase), sfxList.get(phrase).created, sfxList.get(phrase).uses));
                    // TODO: Clear SFX cache method - Maybe works now?
                    sendMessage(channel, "Edited SFX for phrase: " + phrase);
                    break;
                case "filter":
                    /*filterList.put(phrase, result);
                    sendMessage(channel, "Edited filter for phrase: " + phrase);*/
                    break;
                case "response":
                    /*responseList.put(phrase, result);
                    sendMessage(channel, "Edited response to phrase: " + phrase);*/
                    break;
            }
        } catch (SQLException ex) {
            sendMessage (channel, "SQLException; TODO: Real error message");
            /*if (ex.getMessage().contains("[SQLITE_CONSTRAINT_UNIQUE]")) {
                sendMessage(channel, "A reaction for: `" + split[1] + "` already exists");
            } else {
                error("Failed to insert " + type + ": " + message.split(" ")[1], ex);
                sendMessage(channel, "Failed to add " + type + ": " + ex);
            }*/
        } catch (IndexOutOfBoundsException ex) {
            sendMessage (channel, "IndexOutOfBounds; TODO: Real error message");
            /*warn("add " + type + " command malformatted");
            switch (type) {
                case "sfx":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.addSfx <phrase> <https://__________>` (wav, mp3, ogg)");
                    break;
                case "filter":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.addFilter <phrase> <filtered phrase>`");
                    break;
                case "response":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.addResponse <phrase> <response>`");
                    break;
            }*/
        }
    }

    public void removeReaction(String type, String message, String channel) {
        String[] split = message.split(" ", 3);
        String phrase = "";
        try {
            phrase = split[1].toLowerCase();
            PreparedStatement prep = sqlCon.prepareStatement("DELETE FROM reaction WHERE type=? AND phrase=?;");
            prep.setString(1, type);
            prep.setString(2, phrase);
            if (prep.executeUpdate() > 0) {
                switch (type) {
                    case "sfx":
                        sfxList.remove(phrase);
                        sendMessage(channel, "Removed SFX for phrase: " + phrase);
                        break;
                    case "filter":
                        filterList.remove(phrase);
                        sendMessage(channel, "Removed filter for phrase: " + phrase);
                        break;
                    case "response":
                        responseList.remove(phrase);
                        sendMessage(channel, "Removed response to phrase: " + phrase);
                        break;
                }
            } else {
                sendMessage(channel, "No " + type + " found " + (type.equals("response") ? "to" : "for") + " phrase: " + phrase);
            }
        } catch (SQLException ex) {
            error("Failed to remove " + type + ": " + message.split(" ")[1], ex);
            sendMessage(channel, "Failed to remove " + type + ": " + ex);
        } catch (IndexOutOfBoundsException ex) {
            warn("remove " + type + " command malformatted");
            switch (type) {
                case "sfx":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.removeSfx <phrase>");
                    break;
                case "filter":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.removeFilter <phrase>`");
                    break;
                case "response":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.removeResponse <phrase> `");
                    break;
            }
        }
    }

    public void modReaction(String type, String message, String channel) {
        String[] split = message.split(" ", 3);
        String phrase = "";
        try {
            phrase = split[1].toLowerCase();
            String extra = (split.length > 2 ? split[2] : "");
            String output = "", cleanupUrl = "";
            String[] newMod = extra.split("=", 2);
            HashMap<String, String> mods = new HashMap<>();
            switch (type) {
                case "sfx":
                    Sfx currentSfx = sfxList.get(phrase);
                    String url = currentSfx.url;
                    cleanupUrl = url;
                    mods = currentSfx.mods;
                    mods.put(newMod[0], newMod[1]);

                    // Override newMod stuff if needed
                    if (newMod[0].toLowerCase().startsWith("volume")) {
                        try {
                            // This could honestly be outside of the switch
                            if (newMod.length == 1 && newMod[0].equalsIgnoreCase("volume")) {
                                mods.remove("volume");
                                reactionMods.put(phrase, mods);
                                newSfx(phrase, new Sfx(cleanupUrl, reactionMods.get(phrase), sfxList.get(phrase).created, sfxList.get(phrase).uses));
                                // Delete cached thing, since mods are applied on initial convert
                                cleanupQueue.queue.add(Sfx.getFileLocation(cleanupUrl));

                                PreparedStatement prep = sqlCon.prepareStatement(
                                        "DELETE FROM reaction_mod WHERE reaction_id = (SELECT id FROM reaction WHERE phrase = ?) AND mod = ?;");
                                prep.setString(1, phrase);
                                prep.setString(2, newMod[0]);
                                if (prep.executeUpdate() > 0) {
                                    sendMessage(channel, "Removed " + newMod[0] + " mod for " + type + ": " + phrase);
                                }
                                return;
                            }

                            int currentVolume = 0;
                            if (null != mods.get("volume")) {
                                currentVolume = Integer.parseInt(mods.get("volume"));
                            }
                            int volume = Integer.parseInt(newMod[1]);

                            if (newMod[0].equalsIgnoreCase("volume")) {
                                mods.put("volume", "" + volume);
                            } else if (newMod[0].endsWith("-")) {
                                mods.put("volume", "" + (currentVolume - volume));
                                newMod[0] = "volume";
                            } else if (newMod[0].endsWith("+")) {
                                mods.put("volume", "" + (currentVolume + volume));
                                newMod[0] = "volume";
                            }
                        } catch (Exception ex) {
                            error("Failed to mod sfx volume", ex);
                        }
                    }
                    reactionMods.put(phrase, mods);
                    sfxDirty = true;
                    output = "Modified " + type + " for phrase: " + phrase;
                    break;
                case "filter":
                    break;
                case "response":
                    break;
            }


            PreparedStatement prep = sqlCon.prepareStatement(
                    "REPLACE INTO reaction_mod (reaction_id, mod, data) "
                    + "VALUES ((SELECT id FROM reaction WHERE phrase = ?), ?, ?);");
            prep.setString(1, phrase);
            prep.setString(2, newMod[0]);
            prep.setString(3, mods.get(newMod[0]));
            if (prep.executeUpdate() > 0) {
                switch (type) {
                    case "sfx":
                        newSfx(phrase, new Sfx(cleanupUrl, reactionMods.get(phrase), sfxList.get(phrase).created, sfxList.get(phrase).uses));
                        // Delete cached thing, since mods are applied on initial convert
                        cleanupQueue.queue.add(Sfx.getFileLocation(cleanupUrl));
                        sendMessage(channel, output);
                        break;
                    case "filter":
                        break;
                    case "reponse":
                        break;
                }
            } else {
                sendMessage(channel, "No " + type + " found " + (type.equals("response") ? "to" : "for") + " phrase: " + phrase);
            }
        } catch (SQLException ex) {
            error("Failed to modify " + type + ": " + message.split(" ")[1], ex);
            sendMessage(channel, "Failed to modify " + type + ": " + ex);
        } catch (IndexOutOfBoundsException ex) {
            warn("modify " + type + " command malformatted");
            switch (type) {
                case "sfx":
                    sendMessage(channel, "Malformatted command; Usage: `!janna.modSfx <phrase> [param=value]");
                    break;
                case "filter":
                    break;
                case "response":
                    break;
            }
        }
    }

    public boolean isSuperMod(String username) {
        return ("1".equals(getUser(username).roles.get("broadcaster")));
    }

    public boolean isMod(String username) {
        return (isSuperMod(username) || "1".equals(getUser(username).roles.get("moderator")));
    }

    public boolean isSub(String username) {
        return (isSuperMod(username)
                || "1".equals(getUser(username).roles.get("subscriber"))
                || "1".equals(getUser(username).roles.get("founder"))
        );
    }

    public boolean isVIP(String username) {
        return (isSuperMod(username) || isMod(username) || "1".equals(getUser(username).roles.get("vip")));
    }

    public List<String> getMods() {
        return twitch.getMessagingInterface().getChatters(appConfig.get("mainchannel")).execute().getModerators();
    }
}
