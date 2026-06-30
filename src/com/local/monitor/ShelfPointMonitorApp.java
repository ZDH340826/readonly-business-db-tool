package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

public final class ShelfPointMonitorApp extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String PAGE_CONNECTIONS = "连接管理";
    private static final String PAGE_BROWSER = "数据库浏览器";
    private static final String PAGE_ALERTS = "点位缺料报警";

    private final ConfigStore configStore = new ConfigStore(Paths.get("data", "config.properties"));
    private final ConnectionProfileStore profileStore = new ConnectionProfileStore(
            Paths.get("data", "connections.properties"));
    private final PointRepository pointRepository = new PointRepository();
    private final DbMetadataRepository metadataRepository = new DbMetadataRepository();
    private final AlertState alertState = new AlertState();
    private final PointSchedule pointSchedule = new PointSchedule();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Path logPath = Paths.get("logs", "monitor.log");

    private final JList<String> navigationList = new JList<>(new String[] {
            PAGE_CONNECTIONS, PAGE_BROWSER, PAGE_ALERTS});
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JLabel currentConnectionLabel = new JLabel("当前连接：未连接");

    private final DefaultListModel<String> profileListModel = new DefaultListModel<>();
    private final JList<String> profileList = new JList<>(profileListModel);
    private final JTextField profileIdField = new JTextField(12);
    private final JTextField profileNameField = new JTextField(14);
    private final JComboBox<String> profileDbTypeBox = new JComboBox<>(new String[] {"postgres", "h2"});
    private final JTextField profileHostField = new JTextField(16);
    private final JSpinner profilePortSpinner = new JSpinner(new SpinnerNumberModel(5432, 1, 65535, 1));
    private final JTextField profileDatabaseField = new JTextField(12);
    private final JTextField profileSchemaField = new JTextField(10);
    private final JTextField profileUserField = new JTextField(14);
    private final JComboBox<String> profileSslModeBox = new JComboBox<>(new String[] {"disable", "prefer", "require"});
    private final JTextField profileLocalPathField = new JTextField(18);
    private final JPasswordField profilePasswordField = new JPasswordField(14);

    private final DefaultComboBoxModel<String> schemaModel = new DefaultComboBoxModel<>();
    private final JComboBox<String> schemaBox = new JComboBox<>(schemaModel);
    private final DefaultTableModel browserTableModel = new DefaultTableModel(
            new Object[] {"Schema", "Name", "Type"}, 0);
    private final JTable browserTable = new JTable(browserTableModel);
    private final DefaultTableModel columnModel = new DefaultTableModel(
            new Object[] {"Column", "Type", "Size", "Nullable"}, 0);
    private final JTable columnTable = new JTable(columnModel);
    private final DefaultTableModel previewModel = new DefaultTableModel();
    private final JTable previewTable = new JTable(previewModel);

    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(10, 10, 86400, 10));
    private final DefaultTableModel pointModel = new DefaultTableModel(
            new Object[] {"别名", "点位编码", "监测周期(分钟)"}, 0) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 2 ? Integer.class : String.class;
        }
    };
    private final JTable pointTable = new JTable(pointModel);
    private final JButton startButton = new JButton("开始监控");
    private final JButton stopButton = new JButton("停止");
    private final JButton checkButton = new JButton("立即检测");

    private final JTextArea statusArea = new JTextArea();

    private List<ConnectionProfile> profiles = new ArrayList<>();
    private ConnectionProfile currentProfile;
    private char[] currentPassword = new char[0];
    private ScheduledFuture<?> scheduledTask;
    private JDialog activeDialog;

    public static void main(String[] args) {
        if (args.length > 0 && "--self-test".equals(args[0])) {
            System.out.println("ShelfPointMonitor SELF_TEST_OK");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            ShelfPointMonitorApp app = new ShelfPointMonitorApp();
            app.setVisible(true);
        });
    }

    public ShelfPointMonitorApp() {
        super("只读业务数据库工具 - 第一阶段");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 720));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                executor.shutdownNow();
            }
        });
        buildUi();
        loadProfiles();
        loadPointConfig();
        appendStatus("程序已启动。密码只保存在本次运行内，不写入配置文件。");
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setContentPane(root);

        currentConnectionLabel.setFont(currentConnectionLabel.getFont().deriveFont(Font.BOLD));
        root.add(currentConnectionLabel, BorderLayout.NORTH);

        navigationList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navigationList.setSelectedIndex(0);
        navigationList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cardLayout.show(cardPanel, navigationList.getSelectedValue());
            }
        });

        cardPanel.add(buildConnectionPage(), PAGE_CONNECTIONS);
        cardPanel.add(buildBrowserPage(), PAGE_BROWSER);
        cardPanel.add(buildAlertPage(), PAGE_ALERTS);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(navigationList), cardPanel);
        split.setDividerLocation(160);
        split.setResizeWeight(0);
        root.add(split, BorderLayout.CENTER);
        root.add(buildStatusPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildConnectionPage() {
        JPanel page = new JPanel(new BorderLayout(10, 10));
        page.setBorder(javax.swing.BorderFactory.createTitledBorder("连接管理"));

        profileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        profileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int index = profileList.getSelectedIndex();
                if (index >= 0 && index < profiles.size()) {
                    populateProfileForm(profiles.get(index));
                }
            }
        });
        JScrollPane listScroll = new JScrollPane(profileList);
        listScroll.setPreferredSize(new Dimension(220, 260));
        page.add(listScroll, BorderLayout.WEST);

        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        addField(form, row, 0, "ID", profileIdField);
        addField(form, row, 2, "名称", profileNameField);
        addField(form, row, 4, "类型", profileDbTypeBox);
        row++;
        addField(form, row, 0, "Host", profileHostField);
        addField(form, row, 2, "Port", profilePortSpinner);
        addField(form, row, 4, "Database", profileDatabaseField);
        row++;
        addField(form, row, 0, "Schema", profileSchemaField);
        addField(form, row, 2, "User", profileUserField);
        addField(form, row, 4, "sslmode", profileSslModeBox);
        row++;
        addField(form, row, 0, "本地库路径", profileLocalPathField);
        addField(form, row, 2, "Password", profilePasswordField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton newButton = new JButton("新建");
        JButton saveButton = new JButton("保存连接");
        JButton deleteButton = new JButton("删除连接");
        JButton testButton = new JButton("测试并使用连接");
        buttons.add(newButton);
        buttons.add(saveButton);
        buttons.add(deleteButton);
        buttons.add(testButton);
        row++;
        GridBagConstraints buttonsConstraints = gbc(0, row);
        buttonsConstraints.gridwidth = 6;
        buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
        buttonsConstraints.weightx = 1.0;
        form.add(buttons, buttonsConstraints);

        newButton.addActionListener(e -> newProfileForm());
        saveButton.addActionListener(e -> saveProfile());
        deleteButton.addActionListener(e -> deleteProfile());
        testButton.addActionListener(e -> testSelectedProfile());
        profileDbTypeBox.addActionListener(e -> updateProfileTypeEnabled());

        page.add(form, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildBrowserPage() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.setBorder(javax.swing.BorderFactory.createTitledBorder("数据库浏览器（只读）"));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshSchemasButton = new JButton("刷新 Schema");
        JButton loadTablesButton = new JButton("加载表/视图");
        JButton previewButton = new JButton("预览前100行");
        top.add(new JLabel("Schema："));
        top.add(schemaBox);
        top.add(refreshSchemasButton);
        top.add(loadTablesButton);
        top.add(previewButton);
        page.add(top, BorderLayout.NORTH);

        browserTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        browserTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedTableColumns();
            }
        });
        columnTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JSplitPane upper = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(browserTable), new JScrollPane(columnTable));
        upper.setDividerLocation(460);
        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, new JScrollPane(previewTable));
        main.setDividerLocation(300);
        page.add(main, BorderLayout.CENTER);

        refreshSchemasButton.addActionListener(e -> runOnceInBackground(this::refreshSchemas));
        loadTablesButton.addActionListener(e -> runOnceInBackground(this::loadTablesForSelectedSchema));
        previewButton.addActionListener(e -> runOnceInBackground(this::previewSelectedTable));
        return page;
    }

    private JPanel buildAlertPage() {
        JPanel page = new JPanel(new BorderLayout(8, 8));
        page.setBorder(javax.swing.BorderFactory.createTitledBorder("点位缺料报警"));

        JPanel top = new JPanel(new GridBagLayout());
        addField(top, 0, 0, "全局扫描(秒)", intervalSpinner);
        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton saveButton = new JButton("保存点位配置");
        topButtons.add(saveButton);
        topButtons.add(startButton);
        topButtons.add(stopButton);
        topButtons.add(checkButton);
        GridBagConstraints c = gbc(2, 0);
        c.gridwidth = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        top.add(topButtons, c);
        page.add(top, BorderLayout.NORTH);

        pointTable.setRowHeight(26);
        pointTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        pointTable.getColumnModel().getColumn(1).setPreferredWidth(360);
        pointTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        page.add(new JScrollPane(pointTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("添加点位");
        JButton removeButton = new JButton("删除选中");
        bottom.add(addButton);
        bottom.add(removeButton);
        page.add(bottom, BorderLayout.SOUTH);

        addButton.addActionListener(e -> pointModel.addRow(new Object[] {
                "新点位", "", PointDefinition.DEFAULT_INTERVAL_MINUTES}));
        removeButton.addActionListener(e -> removeSelectedPointRows());
        saveButton.addActionListener(e -> savePointConfig());
        startButton.addActionListener(e -> startMonitoring());
        stopButton.addActionListener(e -> stopMonitoring());
        checkButton.addActionListener(e -> runOnceInBackground(this::checkNow));
        stopButton.setEnabled(false);
        return page;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("运行日志"));
        statusArea.setEditable(false);
        statusArea.setRows(8);
        statusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        panel.add(new JScrollPane(statusArea), BorderLayout.CENTER);

        JButton openLogButton = new JButton("打开日志目录");
        openLogButton.addActionListener(e -> openLogs());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(openLogButton);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void addField(JPanel panel, int row, int col, String label, java.awt.Component field) {
        GridBagConstraints labelConstraints = gbc(col, row);
        labelConstraints.anchor = GridBagConstraints.EAST;
        panel.add(new JLabel(label + "："), labelConstraints);

        GridBagConstraints fieldConstraints = gbc(col + 1, row);
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1.0;
        panel.add(field, fieldConstraints);
    }

    private GridBagConstraints gbc(int col, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = col;
        c.gridy = row;
        c.insets = new Insets(4, 4, 4, 4);
        return c;
    }

    private void loadProfiles() {
        ConnectionProfileStore.StoredProfiles stored = profileStore.load();
        profiles = new ArrayList<>(stored.profiles());
        refreshProfileList(stored.currentId());
    }

    private void refreshProfileList(String selectedId) {
        profileListModel.clear();
        int selectedIndex = 0;
        for (int i = 0; i < profiles.size(); i++) {
            ConnectionProfile profile = profiles.get(i);
            profileListModel.addElement(profile.name() + " [" + profile.id() + "]");
            if (profile.id().equals(selectedId)) {
                selectedIndex = i;
            }
        }
        if (!profiles.isEmpty()) {
            profileList.setSelectedIndex(selectedIndex);
        }
    }

    private void populateProfileForm(ConnectionProfile profile) {
        profileIdField.setText(profile.id());
        profileNameField.setText(profile.name());
        profileDbTypeBox.setSelectedItem(profile.dbType());
        profileHostField.setText(profile.host());
        profilePortSpinner.setValue(profile.port());
        profileDatabaseField.setText(profile.database());
        profileSchemaField.setText(profile.schema());
        profileUserField.setText(profile.user());
        profileSslModeBox.setSelectedItem(profile.sslMode());
        profileLocalPathField.setText(profile.localPath());
        updateProfileTypeEnabled();
    }

    private void newProfileForm() {
        profileIdField.setText("profile" + System.currentTimeMillis());
        profileNameField.setText("新连接");
        profileDbTypeBox.setSelectedItem("postgres");
        profileHostField.setText("127.0.0.1");
        profilePortSpinner.setValue(5432);
        profileDatabaseField.setText("example_db");
        profileSchemaField.setText("public");
        profileUserField.setText("readonly_user");
        profileSslModeBox.setSelectedItem("disable");
        profileLocalPathField.setText("data/local-test-db");
        profilePasswordField.setText("");
        profileList.clearSelection();
        updateProfileTypeEnabled();
    }

    private void updateProfileTypeEnabled() {
        boolean local = "h2".equals(profileDbTypeBox.getSelectedItem());
        profileHostField.setEnabled(!local);
        profilePortSpinner.setEnabled(!local);
        profileDatabaseField.setEnabled(!local);
        profileSchemaField.setEnabled(true);
        profileUserField.setEnabled(!local);
        profileSslModeBox.setEnabled(!local);
        profileLocalPathField.setEnabled(local);
        if (local) {
            profileHostField.setText("local");
            profilePortSpinner.setValue(1);
            profileDatabaseField.setText("local-test");
            profileSchemaField.setText("public");
            profileUserField.setText("sa");
            profileSslModeBox.setSelectedItem("disable");
        }
    }

    private ConnectionProfile readProfileForm() {
        String id = profileIdField.getText().trim();
        if (id.isEmpty()) {
            id = "profile" + System.currentTimeMillis();
            profileIdField.setText(id);
        }
        return new ConnectionProfile(
                id,
                profileNameField.getText(),
                String.valueOf(profileDbTypeBox.getSelectedItem()),
                profileHostField.getText(),
                (Integer) profilePortSpinner.getValue(),
                profileDatabaseField.getText(),
                profileSchemaField.getText(),
                profileUserField.getText(),
                String.valueOf(profileSslModeBox.getSelectedItem()),
                profileLocalPathField.getText());
    }

    private void saveProfile() {
        try {
            ConnectionProfile profile = readProfileForm();
            int index = findProfileIndex(profile.id());
            if (index >= 0) {
                profiles.set(index, profile);
            } else {
                profiles.add(profile);
            }
            profileStore.save(profile.id(), profiles);
            refreshProfileList(profile.id());
            appendStatus("连接配置已保存：" + profile.name());
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void deleteProfile() {
        int index = profileList.getSelectedIndex();
        if (index < 0 || index >= profiles.size()) {
            showError(new IllegalArgumentException("请先选择要删除的连接"));
            return;
        }
        if (profiles.size() == 1) {
            showError(new IllegalArgumentException("至少保留一个连接配置"));
            return;
        }
        ConnectionProfile removed = profiles.remove(index);
        try {
            String selectedId = profiles.get(0).id();
            profileStore.save(selectedId, profiles);
            if (currentProfile != null && currentProfile.id().equals(removed.id())) {
                currentProfile = null;
                Arrays.fill(currentPassword, '\0');
                currentPassword = new char[0];
                updateCurrentConnectionLabel();
            }
            refreshProfileList(selectedId);
            appendStatus("连接配置已删除：" + removed.name());
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void testSelectedProfile() {
        try {
            ConnectionProfile profile = readProfileForm();
            char[] password = profilePasswordField.getPassword();
            runOnceInBackground(() -> {
                String result = pointRepository.testConnection(profile.toDbConfig(10), password);
                currentProfile = profile;
                Arrays.fill(currentPassword, '\0');
                currentPassword = password.clone();
                SwingUtilities.invokeLater(() -> {
                    updateCurrentConnectionLabel();
                    appendStatus("测试连接成功并设为当前连接：" + result);
                });
            });
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private int findProfileIndex(String id) {
        for (int i = 0; i < profiles.size(); i++) {
            if (profiles.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void updateCurrentConnectionLabel() {
        if (currentProfile == null) {
            currentConnectionLabel.setText("当前连接：未连接");
            return;
        }
        currentConnectionLabel.setText("当前连接：" + currentProfile.name() + " / "
                + currentProfile.dbType() + " / " + currentProfile.schema());
    }

    private void refreshSchemas() throws Exception {
        DbConfig config = requireCurrentConfig(10);
        List<SchemaInfo> schemas = metadataRepository.listSchemas(config, currentPassword);
        SwingUtilities.invokeLater(() -> {
            schemaModel.removeAllElements();
            for (SchemaInfo schema : schemas) {
                schemaModel.addElement(schema.name());
            }
            selectSchema(currentProfile.schema());
            appendStatus("已刷新 Schema：" + schemas.size() + " 个");
        });
    }

    private void selectSchema(String preferred) {
        for (int i = 0; i < schemaModel.getSize(); i++) {
            if (schemaModel.getElementAt(i).equalsIgnoreCase(preferred)) {
                schemaBox.setSelectedIndex(i);
                return;
            }
        }
        if (schemaModel.getSize() > 0) {
            schemaBox.setSelectedIndex(0);
        }
    }

    private void loadTablesForSelectedSchema() throws Exception {
        DbConfig config = requireCurrentConfig(10);
        String schema = requireSelectedSchema();
        List<TableInfo> tables = metadataRepository.listTables(config, currentPassword, schema);
        SwingUtilities.invokeLater(() -> {
            browserTableModel.setRowCount(0);
            columnModel.setRowCount(0);
            previewModel.setRowCount(0);
            previewModel.setColumnCount(0);
            for (TableInfo table : tables) {
                browserTableModel.addRow(new Object[] {table.schema(), table.name(), table.type()});
            }
            appendStatus("已加载 " + schema + " 下表/视图：" + tables.size() + " 个");
        });
    }

    private void loadSelectedTableColumns() {
        int row = browserTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        String schema = String.valueOf(browserTableModel.getValueAt(browserTable.convertRowIndexToModel(row), 0));
        String table = String.valueOf(browserTableModel.getValueAt(browserTable.convertRowIndexToModel(row), 1));
        runOnceInBackground(() -> {
            DbConfig config = requireCurrentConfig(10);
            List<ColumnInfo> columns = metadataRepository.listColumns(config, currentPassword, schema, table);
            SwingUtilities.invokeLater(() -> {
                columnModel.setRowCount(0);
                for (ColumnInfo column : columns) {
                    columnModel.addRow(new Object[] {
                            column.name(), column.typeName(), column.size(), column.nullable()});
                }
                appendStatus("已加载字段：" + schema + "." + table + " / " + columns.size() + " 个");
            });
        });
    }

    private void previewSelectedTable() throws Exception {
        int row = browserTable.getSelectedRow();
        if (row < 0) {
            throw new IllegalArgumentException("请先选择要预览的表或视图");
        }
        String schema = String.valueOf(browserTableModel.getValueAt(browserTable.convertRowIndexToModel(row), 0));
        String table = String.valueOf(browserTableModel.getValueAt(browserTable.convertRowIndexToModel(row), 1));
        DbConfig config = requireCurrentConfig(10);
        TablePreview preview = metadataRepository.previewTable(config, currentPassword, schema, table, 100);
        SwingUtilities.invokeLater(() -> {
            previewModel.setColumnIdentifiers(preview.columnNames().toArray());
            previewModel.setRowCount(0);
            for (List<String> previewRow : preview.rows()) {
                previewModel.addRow(previewRow.toArray());
            }
            appendStatus("已预览：" + schema + "." + table + " / " + preview.rows().size() + " 行");
        });
    }

    private String requireSelectedSchema() {
        Object selected = schemaBox.getSelectedItem();
        if (selected == null || String.valueOf(selected).isBlank()) {
            throw new IllegalArgumentException("请先刷新并选择 Schema");
        }
        return String.valueOf(selected);
    }

    private void loadPointConfig() {
        ConfigStore.StoredConfig stored = configStore.load();
        intervalSpinner.setValue(stored.intervalSeconds);
        pointModel.setRowCount(0);
        for (PointDefinition point : stored.points) {
            pointModel.addRow(new Object[] {point.alias(), point.code(), point.intervalMinutes()});
        }
    }

    private List<PointDefinition> readPoints() {
        List<PointDefinition> points = new ArrayList<>();
        for (int i = 0; i < pointModel.getRowCount(); i++) {
            String alias = cellText(i, 0);
            String code = cellText(i, 1);
            String intervalText = cellText(i, 2);
            if (!alias.isEmpty() || !code.isEmpty()) {
                if (code.isEmpty() || alias.isEmpty()) {
                    throw new IllegalArgumentException("点位别名和编码必须同时填写");
                }
                points.add(new PointDefinition(code, alias, parseIntervalMinutes(intervalText)));
            }
        }
        if (points.isEmpty()) {
            throw new IllegalArgumentException("至少添加一个点位");
        }
        return points;
    }

    private String cellText(int row, int column) {
        Object value = pointModel.getValueAt(row, column);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int parseIntervalMinutes(String value) {
        if (value == null || value.isBlank()) {
            return PointDefinition.DEFAULT_INTERVAL_MINUTES;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("监测周期必须是整数分钟");
        }
    }

    private void removeSelectedPointRows() {
        int[] rows = pointTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            pointModel.removeRow(pointTable.convertRowIndexToModel(rows[i]));
        }
    }

    private void savePointConfig() {
        try {
            configStore.save(requireCurrentConfig((Integer) intervalSpinner.getValue()), readPoints());
            appendStatus("点位配置已保存。密码未保存。");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void checkNow() throws Exception {
        DbConfig config = requireCurrentConfig((Integer) intervalSpinner.getValue());
        List<PointDefinition> points = pointSchedule.forceAll(readPoints());
        checkPoints(config, points, false, LocalDateTime.now(), "手动检测");
    }

    private void checkDuePoints() throws Exception {
        DbConfig config = requireCurrentConfig((Integer) intervalSpinner.getValue());
        List<PointDefinition> allPoints = readPoints();
        LocalDateTime now = LocalDateTime.now();
        List<PointDefinition> duePoints = pointSchedule.duePoints(allPoints, now);
        if (duePoints.isEmpty()) {
            return;
        }
        checkPoints(config, duePoints, true, now, "自动检测");
    }

    private void checkPoints(
            DbConfig config,
            List<PointDefinition> points,
            boolean updateSchedule,
            LocalDateTime now,
            String source) throws Exception {
        List<PointRecord> records = pointRepository.fetch(config, currentPassword, points);
        MonitorEvaluation evaluation = MonitorLogic.evaluate(points, records, alertState);
        appendStatus(formatCheckResult(points, records, evaluation, source));
        if (updateSchedule) {
            pointSchedule.markChecked(points, now);
        }
        if (evaluation.hasActiveAlert()) {
            showAlertDialog(evaluation);
        }
    }

    private String formatCheckResult(
            List<PointDefinition> checkedPoints,
            List<PointRecord> records,
            MonitorEvaluation evaluation,
            String source) {
        StringBuilder builder = new StringBuilder();
        builder.append(source)
                .append("完成，检查点位 ")
                .append(checkedPoints.size())
                .append(" 个，返回记录 ")
                .append(records.size())
                .append(" 条");
        if (evaluation.hasActiveAlert()) {
            builder.append("，发现异常：");
            for (PointAlert alert : evaluation.alerts()) {
                builder.append(" ").append(alert.alias()).append("(").append(alert.code()).append(") ")
                        .append(alert.message()).append(";");
            }
        } else if (evaluation.suppressedByAck()) {
            builder.append("，异常已关注，本轮不重复弹窗");
        } else {
            builder.append("，无报警");
        }
        for (PointRecord record : records) {
            builder.append(System.lineSeparator())
                    .append("  ")
                    .append(record.mapDataCode())
                    .append(" shelf_code=")
                    .append(record.podCode())
                    .append(" status=")
                    .append(record.status())
                    .append(" lock_state=")
                    .append(record.indLock())
                    .append(" updated_at=")
                    .append(record.dateChg());
        }
        return builder.toString();
    }

    private void startMonitoring() {
        try {
            DbConfig config = requireCurrentConfig((Integer) intervalSpinner.getValue());
            List<PointDefinition> points = readPoints();
            configStore.save(config, points);
            stopMonitoring();
            pointSchedule.clear();
            scheduledTask = executor.scheduleWithFixedDelay(
                    () -> runWithUiErrorHandling(this::checkDuePoints),
                    0,
                    config.intervalSeconds(),
                    TimeUnit.SECONDS);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            appendStatus("已开始监控。全局扫描间隔 " + config.intervalSeconds()
                    + " 秒；点位按各自分钟周期到期查询。");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void stopMonitoring() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        pointSchedule.clear();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private DbConfig requireCurrentConfig(int intervalSeconds) {
        if (currentProfile == null) {
            throw new IllegalStateException("请先在“连接管理”中测试并使用一个连接");
        }
        return currentProfile.toDbConfig(intervalSeconds);
    }

    private void showAlertDialog(MonitorEvaluation evaluation) {
        SwingUtilities.invokeLater(() -> {
            if (activeDialog != null && activeDialog.isShowing()) {
                return;
            }
            JDialog dialog = new JDialog(this, "点位货架异常", false);
            dialog.setAlwaysOnTop(true);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setLayout(new BorderLayout(12, 12));
            dialog.getRootPane().setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JTextArea text = new JTextArea(alertText(evaluation));
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            text.setBackground(new Color(255, 250, 240));
            dialog.add(text, BorderLayout.CENTER);

            JButton ack = new JButton("已关注");
            ack.addActionListener(e -> {
                alertState.acknowledge(evaluation.alertKey());
                appendStatus("用户已关注报警：" + evaluation.alertKey());
                dialog.dispose();
                activeDialog = null;
            });
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.add(ack);
            dialog.add(buttons, BorderLayout.SOUTH);

            dialog.setSize(520, 320);
            dialog.setLocationRelativeTo(this);
            activeDialog = dialog;
            dialog.setVisible(true);
        });
    }

    private String alertText(MonitorEvaluation evaluation) {
        StringBuilder builder = new StringBuilder();
        builder.append("检测时间：").append(TIME_FORMAT.format(LocalDateTime.now())).append(System.lineSeparator())
                .append(System.lineSeparator());
        for (PointAlert alert : evaluation.alerts()) {
            builder.append(alert.alias())
                    .append("：")
                    .append(alert.code())
                    .append(System.lineSeparator())
                    .append("状态：")
                    .append(alert.message())
                    .append(System.lineSeparator())
                    .append(System.lineSeparator());
        }
        builder.append("请现场确认后点击“已关注”。在点位恢复正常前，本次相同报警不会重复弹出。");
        return builder.toString();
    }

    private void runOnceInBackground(CheckedRunnable runnable) {
        executor.submit(() -> runWithUiErrorHandling(runnable));
    }

    private void runWithUiErrorHandling(CheckedRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> showError(ex));
            appendStatus("执行失败：" + ex.getMessage());
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
    }

    private void appendStatus(String message) {
        String line = "[" + TIME_FORMAT.format(LocalDateTime.now()) + "] " + message;
        SwingUtilities.invokeLater(() -> {
            statusArea.append(line + System.lineSeparator());
            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
        writeLog(line);
    }

    private void writeLog(String line) {
        try {
            Files.createDirectories(logPath.getParent());
            OpenOption[] options = {StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            Files.writeString(logPath, line + System.lineSeparator(), StandardCharsets.UTF_8, options);
        } catch (Exception ignored) {
        }
    }

    private void openLogs() {
        try {
            Files.createDirectories(logPath.getParent());
            Desktop.getDesktop().open(logPath.getParent().toFile());
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private interface CheckedRunnable {
        void run() throws Exception;
    }
}


