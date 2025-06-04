package com.anwang.ui;

import com.anwang.types.ContractModel;
import com.anwang.ui.components.CustomPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class StartupPage {
    public JDialog dialog;
    public int closeType;
    public static String privateKey;
    public static String amount;

    public StartupPage() {
        dialog = new JDialog((JFrame) null, "设定信息", true);
        dialog.setLayout(new BorderLayout());
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        closeType = 0;
    }

    public void show() {
        JLabel senderLabel = new JLabel("发送者私钥：");
        JTextField senderField = new JTextField(30);

        JLabel amountLabel = new JLabel("发送总金额：");
        JTextField amountField = new JTextField(30);

        CustomPanel panel = new CustomPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        List<JComponent> components = new ArrayList<>();
        components.add(senderLabel);
        components.add(senderField);
        panel.addRow(components, 0);

        components.clear();
        components.add(amountLabel);
        components.add(amountField);
        panel.addRow(components, 1);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JButton okButton = new JButton("确定");
        JButton exitButton = new JButton("退出");
        buttonPanel.add(okButton);
        buttonPanel.add(exitButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String senderText = senderField.getText();
                String amountText = amountField.getText();
                if (senderText == null || senderText.trim().isEmpty() ||
                        amountText == null || amountText.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "请输入相关信息");
                    return;
                }
                privateKey = senderText.trim();
                amount = amountText.trim();
                closeType = 1;
                dialog.dispose();
            }
        });

        exitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(App.app);
        dialog.setVisible(true);
    }
}