package com.project610;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Dunno if the stuff below does anything, just trying to fix Manjaro text size issues
            JFrame.setDefaultLookAndFeelDecorated(true);
            System.setProperty("awt.useSystemAAFontSettings", "on");
            System.setProperty("swing.aatext", "true");
        } catch (Exception ex) {
            System.err.println("Look and feel broke, probably falling back on the garbo L&F");
            ex.printStackTrace();
        }
        JFrame jf = new JFrame("Janna v0.0.3 (NOW on GitHub?)");
        jf.setMinimumSize(new Dimension(300, 300));
        jf.setSize(500,400);

        Janna janna= new Janna(args, jf);
        janna.setBackground(new Color(230, 230, 230));
        jf.setContentPane(janna);
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setVisible(true);

        try {
            janna.init();
        } catch (Exception ex) {
            System.err.println("Failed to init Janna");
            ex.printStackTrace( );
        }

        //janna.loadSprites();
    }
}
