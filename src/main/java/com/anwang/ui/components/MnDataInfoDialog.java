package com.anwang.ui.components;

import com.anwang.types.MnData;
import com.anwang.ui.App;
import com.anwang.utils.CommonUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MnDataInfoDialog {
    public static void show(MnData data) {
        CustomPanel panel = new CustomPanel();
        List<JComponent> components = new ArrayList<>();

        int index = 0;
        components.add(new JLabel("主节点ID："));
        components.add(new CopyPanel(data.id.toString()));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("主节点地址："));
        components.add(new CopyPanel(data.addr.getValue()));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("收益地址："));
        components.add(new CopyPanel(data.creator.getValue()));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("Enode："));
        components.add(new CopyPanel(data.enode));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("创建高度："));
        components.add(new CopyPanel(data.createHeight.toString()));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("启动高度："));
        components.add(new CopyPanel(data.startHeight.toString()));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("运行天数："));
        components.add(new CopyPanel(String.format("%.2f", data.onlineDay)));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("奖励金额："));
        components.add(new CopyPanel(String.format("%.4f", data.rewardAmount.doubleValue() / CommonUtil.COIN.doubleValue())));
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("奖励状态："));
        JLabel stateLabel;
        if (data.state == 0) {
            stateLabel = new JLabel("未奖励");
            stateLabel.setForeground(Color.RED);
        } else {
            stateLabel = new JLabel("已奖励");
            stateLabel.setForeground(Color.GREEN);
        }
        components.add(stateLabel);
        panel.addRow(components, index++);

        components.clear();
        components.add(new JLabel("奖励交易ID："));
        components.add(new CopyPanel(data.subsidyTxid));
        panel.addRow(components, index++);

        JOptionPane.showConfirmDialog(
                App.app,
                panel,
                "详情",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );
    }
}
