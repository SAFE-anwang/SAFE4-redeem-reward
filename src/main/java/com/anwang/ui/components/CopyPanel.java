package com.anwang.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

public class CopyPanel extends JPanel {

    public static ImageIcon copyIcon;

    public CopyPanel(String text) {
        setLayout(new FlowLayout(FlowLayout.LEFT));

        if (copyIcon == null) {
            copyIcon = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/copy.png")));
        }

        JLabel textLabel = new JLabel();
        if(text.length() >= 80) {
            textLabel.setText("<html>" + text.substring(0, 80) + "<br>" + text.substring(80) + "</html>");
        } else {
            textLabel.setText(text);
        }
        JButton copyButton = new JButton(copyIcon);
        copyButton.setBorderPainted(false);
        copyButton.setContentAreaFilled(false);
        copyButton.setFocusPainted(false);
        int height = textLabel.getPreferredSize().height;
        copyButton.setPreferredSize(new Dimension(height, height));
        copyButton.setToolTipText("复制");
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        copyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                StringSelection stringSelection = new StringSelection(text);
                if (text.contains("(")) {
                    stringSelection = new StringSelection(text.substring(text.indexOf('(') + 1, text.indexOf(')')));
                }
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });

        copyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                copyButton.setContentAreaFilled(true);
                copyButton.setBackground(new Color(200, 200, 200));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                copyButton.setContentAreaFilled(false);
                copyButton.setBackground(null);
            }
        });

        add(textLabel, BorderLayout.CENTER);
        add(copyButton, BorderLayout.EAST);
    }
}
