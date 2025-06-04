package com.anwang.ui.components;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CustomPanel extends JPanel {
    private GridBagConstraints gbc;

    public CustomPanel() {
        setLayout(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // 设置边距
    }

    public void addRow(List<JComponent> components, int row) {
        for (int i = 0; i < components.size(); i++) {
            if (i == 0) {
                gbc.gridx = i;
                gbc.gridy = row;
                gbc.anchor = GridBagConstraints.WEST;
                gbc.weightx = i;
            } else {
                gbc.gridx = i;
                gbc.gridy = row;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.weightx = i;
            }
            add(components.get(i), gbc);
        }
    }
}