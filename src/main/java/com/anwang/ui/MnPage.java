package com.anwang.ui;

import com.anwang.contracts.MasterNodeSubsidy;
import com.anwang.types.ContractModel;
import com.anwang.types.MnData;
import com.anwang.types.masternode.MasterNodeInfo;
import com.anwang.ui.components.MnDataInfoDialog;
import com.anwang.ui.components.filters.NumericFilter;
import com.anwang.utils.CommonUtil;
import io.reactivex.disposables.Disposable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigInteger;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class MnPage extends JPanel {
    private final JTable table;
    private final TxDataTableModel tableModel;
    private final FilterPanel filterPanel;
    private final PaginationPanel paginationPanel;
    private final List<MnData> dataList = Collections.synchronizedList(new ArrayList<>());
    private Disposable eventSubscription;
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);
    private ScheduledExecutorService reconnectScheduler;

    public MnPage() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        filterPanel = new FilterPanel();
        add(filterPanel, BorderLayout.NORTH);

        tableModel = new TxDataTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(30);
        table.getColumnModel().getColumn(0).setMinWidth(60);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(1).setMinWidth(300);
        table.getColumnModel().getColumn(1).setMaxWidth(300);
        table.getColumnModel().getColumn(2).setMinWidth(300);
        table.getColumnModel().getColumn(2).setMaxWidth(300);
        table.getColumnModel().getColumn(3).setMinWidth(60);
        table.getColumnModel().getColumn(3).setMaxWidth(60);
        table.getColumnModel().getColumn(4).setMinWidth(60);
        table.getColumnModel().getColumn(4).setMaxWidth(60);
        table.getColumnModel().getColumn(5).setMinWidth(100);
        table.getColumnModel().getColumn(5).setMaxWidth(100);
        table.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));
        table.getColumnModel().getColumn(7).setMinWidth(50);
        table.getColumnModel().getColumn(7).setMaxWidth(50);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == 7) {
                    MnDataInfoDialog.show((MnData) table.getValueAt(row, 6));
                }
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        paginationPanel = new PaginationPanel(tableModel);
        add(paginationPanel, BorderLayout.SOUTH);

        startSubscribeEvent();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopSubscribeEvent));
    }

    private void startSubscribeEvent() {
        new Thread(this::subscribeEvent).start();
    }

    Event RedeemMasterNodeEvent = new Event("RedeemMasterNode", Arrays.asList(
            new TypeReference<Utf8String>() {
            }, new TypeReference<Address>() {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<Address>() {
            }));

    Event MNRegisterEvent = new Event("MNRegister", Arrays.asList(
            new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<Uint256>() {
            }, new TypeReference<Uint256>() {
            }));
    Event MNAddressChangedEvent = new Event("MNAddressChanged", Arrays.asList(
            new TypeReference<Address>() {
            }, new TypeReference<Address>() {
            }));
    Event MNEnodeChangedEvent = new Event("MNEnodeChanged", Arrays.asList(
            new TypeReference<Address>() {
            }, new TypeReference<Utf8String>() {
            }, new TypeReference<Utf8String>() {
            }));

    Event MNSubsidy = new Event("MNSubsidy", Arrays.asList(
            new TypeReference<DynamicArray<Uint256>>() {
            }));

    BigInteger endHeight = BigInteger.valueOf(96364); //  2025/05/30 15:00:00

    boolean isOk = false;

    private void subscribeEvent() {
        paginationPanel.sendButton.setEnabled(false);
        isOk = true;
        mnDataMap.clear();
        totalOnlineHeight = BigInteger.valueOf(0);

        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
        }
        EthFilter ethFilter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.valueOf(48977)), DefaultBlockParameter.valueOf(endHeight), "0x0000000000000000000000000000000000001090").addOptionalTopics(
                EventEncoder.encode(RedeemMasterNodeEvent));
        eventSubscription = ContractModel.getInstance().getWeb3j().ethLogFlowable(ethFilter)
                .subscribe(log -> {
                    switch (CommonUtil.getEventName(log.getTopics().get(0))) {
                        case "RedeemMasterNode":
                            handleRedeemMasterNode(log.getData());
                            break;
                        default:
                            break;
                    }
                }, error -> {
                    isOk = false;
                    System.out.println(error.getMessage());
                    scheduleReconnect();
                });

        if (!isOk) {
            return;
        }


        if (!eventSubscription.isDisposed()) {
            eventSubscription.dispose();
        }
        ethFilter = new EthFilter(DefaultBlockParameter.valueOf(BigInteger.valueOf(48977)), DefaultBlockParameter.valueOf(endHeight), "0x0000000000000000000000000000000000001025").addOptionalTopics(
                EventEncoder.encode(MNRegisterEvent),
                EventEncoder.encode(MNAddressChangedEvent),
                EventEncoder.encode(MNEnodeChangedEvent));
        eventSubscription = ContractModel.getInstance().getWeb3j().ethLogFlowable(ethFilter)
                .subscribe(log -> {
                    switch (CommonUtil.getEventName(log.getTopics().get(0))) {
                        case "MNRegister":
                            handleRegister(log.getBlockNumber(), log.getData());
                            break;
                        case "MNAddressChanged":
                            handleChangeAddress(log.getData());
                            break;
                        case "MNEnodeChanged":
                            handleChangeEnode(log.getBlockNumber(), log.getData());
                            break;
                        default:
                            break;
                    }
                }, error -> {
                    isOk = false;
                    System.out.println(error.getMessage());
                    scheduleReconnect();
                });

        if (!isOk) {
            return;
        }

        System.out.println("迁移主节点数量：" + redeemMap.size() + "，期间主节点数量：" + mnDataMap.size());
        calculate();
        tableModel.updateData();

        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
        }
        ethFilter = new EthFilter(DefaultBlockParameter.valueOf(endHeight), DefaultBlockParameterName.LATEST, MasterNodeSubsidy.contractAddr).addOptionalTopics(
                EventEncoder.encode(MNSubsidy));
        eventSubscription = ContractModel.getInstance().getWeb3j().ethLogFlowable(ethFilter)
                .subscribe(log -> {
                    switch (CommonUtil.getEventName(log.getTopics().get(0))) {
                        case "MNSubsidy":
                            handleMNSubsidy(log.getTransactionHash(), log.getData());
                            break;
                        default:
                            break;
                    }
                }, error -> {
                    isOk = false;
                    System.out.println(error.getMessage());
                    scheduleReconnect();
                });

        paginationPanel.sendButton.setEnabled(true);
    }

    private void scheduleReconnect() {
        if (isReconnecting.get()) {
            return;
        }
        isReconnecting.set(true);
        if (reconnectScheduler == null || reconnectScheduler.isShutdown()) {
            reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
        }
        reconnectScheduler.schedule(() -> {
            try {
                subscribeEvent();
            } finally {
                isReconnecting.set(false);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void stopSubscribeEvent() {
        if (eventSubscription != null && !eventSubscription.isDisposed()) {
            eventSubscription.dispose();
        }
        if (reconnectScheduler == null || reconnectScheduler.isShutdown()) {
            return;
        }
        reconnectScheduler.shutdown();
        try {
            if (!reconnectScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                reconnectScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            reconnectScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    Map<Address, Boolean> redeemMap = new HashMap<>();

    private void handleRedeemMasterNode(String logData) {
        List<Type> noIndexed = FunctionReturnDecoder.decode(logData, RedeemMasterNodeEvent.getNonIndexedParameters());
        Address addr = (Address) noIndexed.get(3);
        redeemMap.put(addr, true);
    }

    long id = 69;

    Map<Address, MnData> mnDataMap = new HashMap<>();

    private void handleRegister(BigInteger height, String logData) {
        List<Type> noIndexed = FunctionReturnDecoder.decode(logData, MNRegisterEvent.getNonIndexedParameters());
        Address addr = (Address) noIndexed.get(0);
        Address creator = (Address) noIndexed.get(1);
        MnData data = new MnData(++id, addr, creator, height);
        if (!redeemMap.containsKey(addr)) { // new MasterNode
            data.type = BigInteger.ONE;
            data.startHeight = height;
            data.onlineDay = (endHeight.doubleValue() - height.doubleValue()) / 2880.0;
            try {
                MasterNodeInfo info = ContractModel.getInstance().getMasterNodeStorage().getInfoByID(data.id);
                data.enode = info.enode;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        data.createHeight = height;
        mnDataMap.put(addr, data);
    }

    private void handleChangeAddress(String logData) {
        List<Type> noIndexed = FunctionReturnDecoder.decode(logData, MNAddressChangedEvent.getNonIndexedParameters());
        Address oldAddr = (Address) noIndexed.get(0);
        Address newAddr = (Address) noIndexed.get(1);
        if (mnDataMap.containsKey(oldAddr)) {
            MnData data = mnDataMap.get(oldAddr);
            data.addr = newAddr;
            mnDataMap.put(newAddr, data);
            mnDataMap.remove(oldAddr);
        }
    }

    private void handleChangeEnode(BigInteger height, String logData) {
        List<Type> noIndexed = FunctionReturnDecoder.decode(logData, MNEnodeChangedEvent.getNonIndexedParameters());
        Address addr = (Address) noIndexed.get(0);
        String enode = (String) noIndexed.get(1).getValue();
        if (mnDataMap.containsKey(addr)) {
            MnData data = mnDataMap.get(addr);
            if (data.enode.isEmpty()) {
                data.startHeight = height;
                data.onlineDay = (endHeight.doubleValue() - height.doubleValue()) / 2880.0;
            }
            data.enode = enode;
            mnDataMap.put(addr, data);
        }
    }

    private void handleMNSubsidy(String txid, String logData) {
        List<Type> noIndexed = FunctionReturnDecoder.decode(logData, MNSubsidy.getNonIndexedParameters());
        List<BigInteger> mnIDs = ((DynamicArray<Uint256>) noIndexed.get(0)).getValue().stream().map(v -> v.getValue()).collect(Collectors.toList());
        for (BigInteger mnID : mnIDs) {
            int pos = find(dataList, mnID);
            if (pos != -1) {
                MnData data = dataList.get(pos);
                if (data.subsidyTxid.isEmpty()) {
                    data.subsidyTxid = txid;
                } else {
                    data.subsidyTxid += ("<br>" + txid);
                }
                // data.state = 1;
                dataList.set(pos, data);
                tableModel.fireTableDataChanged();
            }
        }
    }

    private int find(List<MnData> arr, BigInteger mnID) {
        for (int i = 0; i < arr.size(); i++) {
            if (arr.get(i).id.longValue() == mnID.longValue()) {
                return i;
            }
        }
        return -1;
    }

    BigInteger totalOnlineHeight = BigInteger.valueOf(0);
    BigInteger totalRewardAmount = CommonUtil.COIN.multiply(new BigInteger(StartupPage.amount));

    private void calculate() {
        for (Address addr : mnDataMap.keySet()) {
            if (mnDataMap.get(addr).startHeight.longValue() == 0) {
                continue;
            }
            totalOnlineHeight = totalOnlineHeight.add(endHeight.subtract(mnDataMap.get(addr).startHeight));
        }

        for (Address addr : mnDataMap.keySet()) {
            MnData data = mnDataMap.get(addr);
            if (data.startHeight.longValue() == 0) {
                continue;
            }
            data.rewardAmount = endHeight.subtract(data.startHeight).multiply(totalRewardAmount).divide(totalOnlineHeight);
            dataList.add(data);
        }
        dataList.sort(Comparator.comparingLong(e -> e.id.longValue()));
    }

    private List<MnData> getFilteredData() {
        String searchText = filterPanel.getSearchText();
        if (searchText.isEmpty()) {
            return dataList;
        }
        List<MnData> filteredDataList = new ArrayList<>();
        for (MnData data : dataList) {
            if (data.addr.getValue().equals(searchText) ||
                    data.creator.getValue().equals(searchText)) {
                filteredDataList.add(data);
            }
        }
        return filteredDataList;
    }

    private void updateTable() {
        paginationPanel.currentPage = 0;
        paginationPanel.updatePageInfo();
        tableModel.fireTableDataChanged();
    }

    class TxDataTableModel extends AbstractTableModel {
        private final String[] columnNames = {"主节点ID", "主节点地址", "收益地址", "类型", "运行天数", "奖励金额", "状态", "详情 >>"};
        private final int pageSize = 20;

        @Override
        public int getRowCount() {
            int total = getFilteredData().size();
            int fullPages = total / pageSize;
            int lastPageSize = total % pageSize;

            if (paginationPanel.getCurrentPage() < fullPages) {
                return pageSize;
            } else if (paginationPanel.getCurrentPage() == fullPages) {
                return lastPageSize > 0 ? lastPageSize : (fullPages > 0 ? pageSize : 0);
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            MnData data = getDataAt(paginationPanel.getCurrentPage(), rowIndex);
            if (data == null) return null;

            switch (columnIndex) {
                case 0:
                    return data.id;
                case 1:
                    return data.addr;
                case 2:
                    return data.creator;
                case 3:
                    return data.type.intValue() == 0 ? "旧节点" : "新节点";
                case 4:
                    return String.format("%.2f", data.onlineDay);
                case 5:
                    return String.format("%.4f", data.rewardAmount.doubleValue() / CommonUtil.COIN.doubleValue());
                case 6:
                    return data;
                case 7:
                    return "详情 >>";
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 6;
        }

        public MnData getDataAt(int page, int row) {
            int index = page * pageSize + row;
            List<MnData> tempList = getFilteredData();
            if (index >= 0 && index < tempList.size()) {
                return tempList.get(index);
            }
            return null;
        }

        public void updateData() {
            SwingUtilities.invokeLater(() -> {
                paginationPanel.updatePageInfo();
                paginationPanel.totalLabel.setText("总奖励主节点数：" + dataList.size());
                fireTableDataChanged();
            });
        }

        public int getTotalPages() {
            return (int) Math.ceil((double) getFilteredData().size() / pageSize);
        }
    }

    private int doSubsidy() {
        List<MnData> needs = new ArrayList<>();
        for (MnData data : dataList) {
            if (data.rewardAmount.compareTo(BigInteger.ZERO) == 0 ||
                    data.state == 1) {
                continue;
            }
            needs.add(data);
        }

        if (needs.isEmpty()) {
            return 1;
        }

        int i = 0;
        int batchNum = 100;
        for (; i < needs.size() / batchNum; i++) {
            List<BigInteger> mnIDs = new ArrayList<>();
            List<Address> creators = new ArrayList<>();
            List<BigInteger> amounts = new ArrayList<>();
            BigInteger value = BigInteger.ZERO;
            for (int k = 0; k < batchNum; k++) {
                MnData data = needs.get(i * batchNum + k);
                mnIDs.add(data.id);
                creators.add(data.creator);
                amounts.add(data.rewardAmount);
                value = value.add(data.rewardAmount);
            }
            try {
                String txid = ContractModel.getInstance().getMasterNodeSubsidy().subsidy(StartupPage.privateKey, value, mnIDs, creators, amounts);
                for (int k = 0; k < batchNum; k++) {
                    MnData data = needs.get(i * batchNum + k);
                    data.state = 1;
                    if (data.subsidyTxid.isEmpty()) {
                        data.subsidyTxid = txid;
                    } else {
                        data.subsidyTxid += ("<br>" + txid);
                    }
                    int pos = find(dataList, data.id);
                    if (pos != -1) {
                        dataList.set(pos, data);
                    }
                }
                tableModel.fireTableDataChanged();
            } catch (Exception e) {
                e.printStackTrace();
                return 2;
            }
        }

        if (needs.size() % batchNum != 0) {
            List<MnData> temps = needs.subList(i * batchNum, needs.size());
            List<BigInteger> mnIDs = new ArrayList<>();
            List<Address> creators = new ArrayList<>();
            List<BigInteger> amounts = new ArrayList<>();
            BigInteger value = BigInteger.ZERO;
            for (int k = 0; k < temps.size(); k++) {
                MnData data = temps.get(k);
                mnIDs.add(data.id);
                creators.add(data.creator);
                amounts.add(data.rewardAmount);
                value = value.add(data.rewardAmount);
            }
            try {
                String txid = ContractModel.getInstance().getMasterNodeSubsidy().subsidy(StartupPage.privateKey, value, mnIDs, creators, amounts);
                for (int k = 0; k < temps.size(); k++) {
                    MnData data = temps.get(k);
                    data.state = 1;
                    if (data.subsidyTxid.isEmpty()) {
                        data.subsidyTxid = txid;
                    } else {
                        data.subsidyTxid += ("<br>" + txid);
                    }
                    int pos = find(dataList, data.id);
                    if (pos != -1) {
                        dataList.set(pos, data);
                    }
                }
                tableModel.fireTableDataChanged();
            } catch (Exception e) {
                e.printStackTrace();
                return 2;
            }
        }

        return 0;
    }

    class FilterPanel extends JPanel {
        private final JTextField filterField;

        public FilterPanel() {
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

            JLabel filterLabel = new JLabel("查询地址:");
            filterField = new JTextField(20);
            JButton filterButton = new JButton("查询");

            filterButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateTable();
                }
            });

            add(filterLabel);
            add(filterField);
            add(filterButton);
        }

        public String getSearchText() {
            return filterField.getText().trim();
        }
    }

    class PaginationPanel extends JPanel {
        private final JButton prevButton;
        private final JButton nextButton;

        private final JTextField field;
        private final JLabel pageLabel2;
        private int currentPage = 0;
        private final TxDataTableModel model;

        private final JLabel totalLabel;
        private final JButton sendButton;

        public PaginationPanel(TxDataTableModel model) {
            this.model = model;
            setLayout(new GridBagLayout());

            prevButton = new JButton("上一页");
            nextButton = new JButton("下一页");
            JLabel pageLabel1 = new JLabel("第 ");
            field = new JTextField(3);
            field.setHorizontalAlignment(JTextField.CENTER);
            pageLabel2 = new JLabel();

            totalLabel = new JLabel("统计中...");
            sendButton = new JButton("发送");

            prevButton.addActionListener(e -> {
                if (currentPage > 0) {
                    currentPage--;
                    updatePageInfo();
                    tableModel.fireTableDataChanged();
                }
            });

            nextButton.addActionListener(e -> {
                if (currentPage < model.getTotalPages() - 1) {
                    currentPage++;
                    updatePageInfo();
                    tableModel.fireTableDataChanged();
                }
            });

            ((AbstractDocument) field.getDocument()).setDocumentFilter(new NumericFilter());
            field.addActionListener(e -> {
                int tempPage = Integer.parseInt(field.getText().trim());
                if (tempPage < 1) {
                    tempPage = 1;
                }
                currentPage = Math.min(tempPage - 1, model.getTotalPages());
                updatePageInfo();
                tableModel.fireTableDataChanged();
            });

            sendButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    sendButton.setEnabled(false);
                    int ret = doSubsidy();
                    if (ret == 0) {
                        JOptionPane.showMessageDialog(null, "恭喜，已全部奖励，正在打包交易，请等待确认！！！");
                    } else if (ret == 1) {
                        JOptionPane.showMessageDialog(null, "恭喜，当前已没有需要奖励的记录！！！");
                    } else {
                        JOptionPane.showMessageDialog(null, "抱歉，有部分主节点奖励失败，检查金额是否充足后等待5分钟再试试！！！");
                        sendButton.setEnabled(true);
                    }
                }
            });

            JPanel centerPanel = new JPanel();
            centerPanel.add(prevButton);
            centerPanel.add(pageLabel1);
            centerPanel.add(field);
            centerPanel.add(pageLabel2);
            centerPanel.add(nextButton);

            JPanel rightPanel = new JPanel();
            rightPanel.add(totalLabel);
            rightPanel.add(sendButton);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.anchor = GridBagConstraints.CENTER;
            add(centerPanel, gbc);

            gbc.gridx = 0;
            gbc.weightx = 0.0;
            gbc.anchor = GridBagConstraints.EAST;
            add(rightPanel, gbc);

            updatePageInfo();
        }

        public void updatePageInfo() {
            int totalPages = Math.max(1, model.getTotalPages());
            field.setText(String.valueOf(currentPage + 1));
            pageLabel2.setText(String.format(" 页 / 共 %d 页", totalPages));
            prevButton.setEnabled(currentPage > 0);
            nextButton.setEnabled(currentPage < totalPages - 1);
        }

        public int getCurrentPage() {
            return currentPage;
        }
    }

    static class ButtonRenderer extends DefaultTableCellRenderer {
        private final JPanel panel;
        private final JLabel label;

        public ButtonRenderer() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            label = new JLabel();
            panel.add(label);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof MnData) {
                MnData data = (MnData) value;
                label.setVisible(false);

                switch (data.state) {
                    case 0:
                        label.setText("未奖励");
                        label.setVisible(true);
                        break;
                    case 1:
                        label.setText("已奖励");
                        label.setForeground(Color.GREEN);
                        label.setVisible(true);
                    default:
                        break;
                }
            }
            return panel;
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        private final JPanel panel;
        private final JLabel label;

        private MnData currentData;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            label = new JLabel();
            panel.add(label);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (value instanceof MnData) {
                currentData = (MnData) value;
                label.setVisible(false);

                switch (currentData.state) {
                    case 0:
                        label.setText("未奖励");
                        label.setVisible(true);
                        break;
                    case 1:
                        label.setText("已奖励");
                        label.setForeground(Color.GREEN);
                        label.setVisible(true);
                    default:
                        break;
                }
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return currentData;
        }
    }
}