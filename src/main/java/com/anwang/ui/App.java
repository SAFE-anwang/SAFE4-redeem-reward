package com.anwang.ui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class App extends JFrame {

    public static App app;

    public App() {
        setTitle("迁移奖励工具 v1.0.0");
        setMinimumSize(new Dimension(1200, 770));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ImageIcon imageIcon = new ImageIcon(Objects.requireNonNull(getClass().getClassLoader().getResource("images/safe.png")));
        setIconImage(imageIcon.getImage());
        setLocationRelativeTo(null);

        MnPage mnPage = new MnPage();
        add(mnPage);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StartupPage startupPage = new StartupPage();
            startupPage.show();
            if (startupPage.closeType == 1) {
                app = new App();
                app.setVisible(true);
                JOptionPane.showMessageDialog(app, "<html>请注意: 当前工具将对 5月13日15:00:00 至 5月30日15:00:00 新/迁移的主节点 分发奖励。<br><br>总奖励金额为: " + startupPage.amount + " SAFE。<br><br>请确保私钥对应地址有足够的金额！！！");
            }
        });
    }
}
