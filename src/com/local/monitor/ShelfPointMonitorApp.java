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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
    private static final String EXPECTED_SELF_TEST_VERSION = "0.4.0";
    private static final Pattern PRIVATE_10_NET_PATTERN = Pattern.compile("\\b10(?:\\.\\d{1,3}){3}\\b");
    private static final Pattern REAL_POINT_CODE_PATTERN = Pattern.compile("\\b\\d{6}BB\\d{6}\\b");
    private static final String PAGE_CONNECTIONS = "连接管理";
    private static final String PAGE_BROWSER = "数据库浏览器";
    private static final String PAGE_ALERTS = "点位缺料报警";

    private final ConfigStore configStore = new ConfigStore(Paths.get("data", "config.properties"));
    private final GroupConfigStore groupConfigStore = new GroupConfigStore(Paths.get("data", "group-config.properties"));
    private final ConnectionProfileStore profileStore = new ConnectionProfileStore(
            Paths.get("data", "connections.properties"));
    private final PointRepository pointRepository = new PointRepository();
    private final DbMetadataRepository metadataRepository = new DbMetadataRepository();
    private final AlertState alertState = new AlertState();
    private final PointSchedule pointSchedule = new PointSchedule();
    private final GroupLogWriter groupLogWriter = new GroupLogWriter(Paths.get("logs"));
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
    private final JSpinner profilePortSpinner = new JSpinner(new SpinnerNumberModel(2345, 1, 65535, 1));
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
    private final DefaultListModel<String> groupListModel = new DefaultListModel<>();
    private final JList<String> groupList = new JList<>(groupListModel);
    private final JTextField groupIdField = new JTextField(16);
    private final JTextField groupAreaField = new JTextField(14);
    private final JTextField groupNameField = new JTextField(14);
    private final JTextField groupMaterialField = new JTextField(14);
    private final JCheckBox groupEnabledBox = new JCheckBox("启用");
    private final JCheckBox ruleEnabledBox = new JCheckBox("启用规则", true);
    private final JCheckBox requireUseEmptyBox = new JCheckBox("使用位无货架", true);
    private final JCheckBox backupThresholdParticipatesBox = new JCheckBox("备用位下限参与报警", true);
    private final JSpinner minBackupAvailableSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 999, 1));
    private final JSpinner durationMinutesSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 1440, 1));
    private final JSpinner groupCheckIntervalMinutesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1440, 1));
    private final DefaultTableModel groupPointModel = new DefaultTableModel(
            new Object[] {"角色", "别名", "点位编码", "启用"}, 0) {
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 3 ? Boolean.class : String.class;
        }
    };
    private final JTable groupPointTable = new JTable(groupPointModel);
    private final JPanel pointStatusPanel = new JPanel(new GridBagLayout());
    private final JLabel groupSummaryLabel = new JLabel("当前判断：未检测");
    private final JTextArea groupRuntimeArea = new JTextArea();

    private List<ConnectionProfile> profiles = new ArrayList<>();
    private List<PointGroupDefinition> pointGroups = new ArrayList<>();
    private volatile List<PointGroupDefinition> monitoredGroups = List.of();
    private final Object groupMonitorLock = new Object();
    private final Map<String, GroupRuntimeState> groupStates = new LinkedHashMap<>();
    private final Map<String, GroupAlertStatus> lastGroupStatuses = new LinkedHashMap<>();
    private ConnectionProfile currentProfile;
    private char[] currentPassword = new char[0];
    private ScheduledFuture<?> scheduledTask;
    private JDialog activeDialog;
    private String activeDialogGroupId = "";

    public static void main(String[] args) {
        if (args.length > 0 && "--self-test".equals(args[0])) {
            try {
                runSelfTest(resolveSelfTestAppRoot());
                System.out.println("ShelfPointMonitor SELF_TEST_OK");
            } catch (Exception ex) {
                System.err.println("ShelfPointMonitor SELF_TEST_FAILED: " + ex.getMessage());
                ex.printStackTrace(System.err);
                System.exit(2);
            }
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
        loadGroupConfig();
        appendStatus("程序已启动。密码只保存在本次运行内，不写入配置文件。");
    }

    static void runSelfTestForTest(Path appRoot) throws Exception {
        runSelfTest(appRoot);
    }

    private static Path resolveSelfTestAppRoot() throws Exception {
        String configured = System.getProperty("shelf.monitor.appRoot");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured).toAbsolutePath().normalize();
        }
        Path codeLocation = Path.of(ShelfPointMonitorApp.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        if (Files.isRegularFile(codeLocation)) {
            return codeLocation.getParent().toAbsolutePath().normalize();
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    private static void runSelfTest(Path appRoot) throws Exception {
        Path root = appRoot.toAbsolutePath().normalize();
        requireFile(root.resolve("ShelfPointMonitor.jar"), "packaged application jar");
        String version = Files.readString(requireFile(root.resolve("VERSION"), "VERSION"), StandardCharsets.UTF_8).trim();
        if (!EXPECTED_SELF_TEST_VERSION.equals(version)) {
            throw new IllegalStateException("VERSION must be " + EXPECTED_SELF_TEST_VERSION + ", actual=" + version);
        }

        requireFile(root.resolve("lib/postgresql-42.2.25.jar"), "PostgreSQL JDBC driver");
        requireFile(root.resolve("lib/h2-2.2.224.jar"), "H2 JDBC driver");
        Class.forName("org.postgresql.Driver");
        Class.forName("org.h2.Driver");

        Path data = requireDirectory(root.resolve("data"), "data directory");
        Path configPath = requireFile(data.resolve("config.properties"), "data/config.properties");
        Path connectionsPath = requireFile(data.resolve("connections.properties"), "data/connections.properties");
        Path groupConfigPath = requireFile(data.resolve("group-config.properties"), "data/group-config.properties");

        Properties configProperties = loadSelfTestProperties(configPath);
        Properties connectionProperties = loadSelfTestProperties(connectionsPath);
        Properties groupProperties = loadSelfTestProperties(groupConfigPath);
        assertNoPasswordProperty(configProperties, "config.properties");
        assertNoPasswordProperty(connectionProperties, "connections.properties");
        assertNoPasswordProperty(groupProperties, "group-config.properties");
        assertNoSensitiveProperties(configProperties, "config.properties");
        assertNoSensitiveProperties(connectionProperties, "connections.properties");
        assertNoSensitiveProperties(groupProperties, "group-config.properties");

        ConfigStore.StoredConfig config = new ConfigStore(configPath).load();
        if (!"__SITE_HOST__".equals(config.host) || !"__SITE_USER__".equals(config.user)) {
            throw new IllegalStateException("config.properties must use site placeholders");
        }
        if (!"data/local-test-db".equals(config.localPath)) {
            throw new IllegalStateException("config.properties localPath must remain data/local-test-db");
        }
        assertSamplePointDefinitions(config.points, "config.properties points");

        ConnectionProfileStore.StoredProfiles profiles = new ConnectionProfileStore(connectionsPath).load();
        boolean foundPlaceholderProfile = false;
        boolean foundLocalProfile = false;
        for (ConnectionProfile profile : profiles.profiles()) {
            if ("__SITE_HOST__".equals(profile.host()) && "__SITE_USER__".equals(profile.user())) {
                foundPlaceholderProfile = true;
            }
            if ("h2".equals(profile.dbType())) {
                foundLocalProfile = true;
            }
        }
        if (!foundPlaceholderProfile || !foundLocalProfile) {
            throw new IllegalStateException("connections.properties must include placeholder and local profiles");
        }

        List<PointGroupDefinition> groups = new GroupConfigStore(groupConfigPath).load();
        if (groups.isEmpty()) {
            throw new IllegalStateException("group-config.properties must include a sample group");
        }
        PointGroupDefinition group = groups.get(0);
        if (!group.rule().backupThresholdParticipates()) {
            throw new IllegalStateException("sample group must enable backupThresholdParticipates");
        }
        assertSampleGroupPoints(group.points(), "group-config.properties points");

        Path localDbPath = root.resolve(config.localPath).normalize();
        if (!localDbPath.startsWith(root)) {
            throw new IllegalStateException("local test database path must stay under packaged root");
        }
        LocalTestDatabase.reset(DbConfig.localTest(localDbPath.toString(), 30));
    }

    private static Path requireFile(Path path, String label) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(label + " is missing: " + path);
        }
        return path;
    }

    private static Path requireDirectory(Path path, String label) {
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(label + " is missing: " + path);
        }
        return path;
    }

    private static Properties loadSelfTestProperties(Path path) throws Exception {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static void assertNoPasswordProperty(Properties properties, String label) {
        for (String name : properties.stringPropertyNames()) {
            if (name.toLowerCase(java.util.Locale.ROOT).contains("password")) {
                throw new IllegalStateException(label + " must not contain password property: " + name);
            }
        }
    }

    private static void assertNoSensitiveProperties(Properties properties, String label) {
        for (String name : properties.stringPropertyNames()) {
            assertNotSensitive(label + "." + name, properties.getProperty(name, ""));
        }
    }

    private static void assertNotSensitive(String label, String value) {
        if (PRIVATE_10_NET_PATTERN.matcher(value).find()) {
            throw new IllegalStateException(label + " contains private 10.x address");
        }
        if (REAL_POINT_CODE_PATTERN.matcher(value).find()) {
            throw new IllegalStateException(label + " contains real-looking point code");
        }
    }

    private static void assertSamplePointDefinitions(List<PointDefinition> points, String label) {
        if (points.isEmpty()) {
            throw new IllegalStateException(label + " must not be empty");
        }
        for (PointDefinition point : points) {
            assertSamplePointCode(label, point.code());
        }
    }

    private static void assertSampleGroupPoints(List<GroupMonitorPoint> points, String label) {
        if (points.isEmpty()) {
            throw new IllegalStateException(label + " must not be empty");
        }
        for (GroupMonitorPoint point : points) {
            assertSamplePointCode(label, point.code());
        }
    }

    private static void assertSamplePointCode(String label, String code) {
        if (!(code.startsWith("USE_POINT_") || code.startsWith("BACKUP_POINT_"))) {
            throw new IllegalStateException(label + " must use sample point codes, actual=" + code);
        }
        assertNotSensitive(label, code);
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
        addField(form, row, 0, "连接ID", profileIdField);
        addField(form, row, 2, "连接名称", profileNameField);
        addField(form, row, 4, "数据库类型", profileDbTypeBox);
        row++;
        addField(form, row, 0, "服务器地址/IP", profileHostField);
        addField(form, row, 2, "端口", profilePortSpinner);
        addField(form, row, 4, "数据库名", profileDatabaseField);
        row++;
        addField(form, row, 0, "数据库空间/Schema", profileSchemaField);
        addField(form, row, 2, "用户名", profileUserField);
        addField(form, row, 4, "SSL模式", profileSslModeBox);
        row++;
        addField(form, row, 0, "本地测试库路径", profileLocalPathField);
        addField(form, row, 2, "密码", profilePasswordField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton newButton = new JButton("新建连接");
        JButton saveButton = new JButton("保存连接");
        JButton deleteButton = new JButton("删除连接");
        JButton testButton = new JButton("测试连接并使用");
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
        page.setBorder(javax.swing.BorderFactory.createTitledBorder("点位组缺料报警"));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton addGroupButton = new JButton("新增组");
        JButton removeGroupButton = new JButton("删除组");
        JButton addPointButton = new JButton("新增点位");
        JButton removePointButton = new JButton("删除点位");
        JButton saveButton = new JButton("保存配置");
        top.add(addGroupButton);
        top.add(removeGroupButton);
        top.add(addPointButton);
        top.add(removePointButton);
        top.add(saveButton);
        top.add(startButton);
        top.add(stopButton);
        top.add(checkButton);
        page.add(top, BorderLayout.NORTH);

        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                populateSelectedGroup();
            }
        });
        JScrollPane groupScroll = new JScrollPane(groupList);
        groupScroll.setPreferredSize(new Dimension(240, 360));

        JPanel detail = new JPanel(new BorderLayout(8, 8));
        detail.add(buildGroupDetailForm(), BorderLayout.NORTH);

        groupPointTable.setRowHeight(26);
        groupPointTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        groupPointTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        groupPointTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        groupPointTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        groupPointTable.getColumnModel().getColumn(0).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(new String[] {PointRole.USE.name(), PointRole.BACKUP.name()})));
        JPanel pointConfigPanel = new JPanel(new BorderLayout(8, 8));
        pointConfigPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("点位配置"));
        pointConfigPanel.add(new JScrollPane(groupPointTable), BorderLayout.CENTER);

        JPanel statusBoard = new JPanel(new BorderLayout(8, 8));
        statusBoard.setBorder(javax.swing.BorderFactory.createTitledBorder("点位状态看板"));
        groupSummaryLabel.setFont(groupSummaryLabel.getFont().deriveFont(Font.BOLD, 15f));
        pointStatusPanel.setBackground(Color.WHITE);
        pointStatusPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 6, 6, 6));
        statusBoard.add(groupSummaryLabel, BorderLayout.NORTH);
        statusBoard.add(new JScrollPane(pointStatusPanel), BorderLayout.CENTER);

        groupRuntimeArea.setEditable(false);
        groupRuntimeArea.setRows(4);
        groupRuntimeArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        statusBoard.add(new JScrollPane(groupRuntimeArea), BorderLayout.SOUTH);

        JSplitPane detailSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, statusBoard, pointConfigPanel);
        detailSplit.setResizeWeight(0.65);
        detailSplit.setDividerLocation(360);
        detail.add(detailSplit, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, groupScroll, detail);
        split.setDividerLocation(240);
        page.add(split, BorderLayout.CENTER);

        addGroupButton.addActionListener(e -> addPointGroup());
        removeGroupButton.addActionListener(e -> removeSelectedGroup());
        addPointButton.addActionListener(e -> groupPointModel.addRow(new Object[] {
                PointRole.BACKUP.name(), "备用位", "", Boolean.TRUE}));
        removePointButton.addActionListener(e -> removeSelectedGroupPointRows());
        saveButton.addActionListener(e -> saveGroupConfig());
        startButton.addActionListener(e -> startMonitoring());
        stopButton.addActionListener(e -> stopMonitoring());
        checkButton.addActionListener(e -> checkNow());
        stopButton.setEnabled(false);
        return page;
    }

    private JPanel buildGroupDetailForm() {
        JPanel form = new JPanel(new GridBagLayout());
        int row = 0;
        addField(form, row, 0, "组ID", groupIdField);
        addField(form, row, 2, "区域", groupAreaField);
        addCheckBox(form, row, 4, groupEnabledBox);
        row++;
        addField(form, row, 0, "组名", groupNameField);
        addField(form, row, 2, "物料", groupMaterialField);
        addCheckBox(form, row, 4, ruleEnabledBox);
        row++;
        addField(form, row, 0, "检测周期(分钟)", groupCheckIntervalMinutesSpinner);
        addField(form, row, 2, "报警持续(分钟)", durationMinutesSpinner);
        addCheckBox(form, row, 4, requireUseEmptyBox);
        row++;
        addField(form, row, 0, "最少备用位有料", minBackupAvailableSpinner);
        addCheckBox(form, row, 2, backupThresholdParticipatesBox);
        return form;
    }

    private void addCheckBox(JPanel panel, int row, int col, JCheckBox box) {
        GridBagConstraints constraints = gbc(col, row);
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        panel.add(box, constraints);
    }

    private JPanel buildLegacyAlertPage() {
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
        checkButton.addActionListener(e -> checkNow());
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
        profileNameField.setText("现场数据库");
        profileDbTypeBox.setSelectedItem("postgres");
        profileHostField.setText("__SITE_HOST__");
        profilePortSpinner.setValue(2345);
        profileDatabaseField.setText("cms_web");
        profileSchemaField.setText("public");
        profileUserField.setText("__SITE_USER__");
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

    private void loadGroupConfig() {
        pointGroups = new ArrayList<>(groupConfigStore.load());
        refreshGroupList(pointGroups.isEmpty() ? "" : pointGroups.get(0).id());
    }

    private void refreshGroupList(String selectedId) {
        groupListModel.clear();
        int selectedIndex = -1;
        for (int i = 0; i < pointGroups.size(); i++) {
            PointGroupDefinition group = pointGroups.get(i);
            groupListModel.addElement(group.areaName() + " / " + group.groupName() + " [" + group.id() + "]");
            if (group.id().equals(selectedId)) {
                selectedIndex = i;
            }
        }
        if (selectedIndex < 0 && !pointGroups.isEmpty()) {
            selectedIndex = 0;
        }
        if (selectedIndex >= 0) {
            groupList.setSelectedIndex(selectedIndex);
        }
    }

    private void populateSelectedGroup() {
        int index = groupList.getSelectedIndex();
        if (index < 0 || index >= pointGroups.size()) {
            return;
        }
        PointGroupDefinition group = pointGroups.get(index);
        groupIdField.setText(group.id());
        groupAreaField.setText(group.areaName());
        groupNameField.setText(group.groupName());
        groupMaterialField.setText(group.materialName());
        groupEnabledBox.setSelected(group.enabled());
        ruleEnabledBox.setSelected(group.rule().enabled());
        requireUseEmptyBox.setSelected(group.rule().requireUsePointEmpty());
        minBackupAvailableSpinner.setValue(group.rule().minBackupAvailable());
        durationMinutesSpinner.setValue(group.rule().durationMinutes());
        backupThresholdParticipatesBox.setSelected(group.rule().backupThresholdParticipates());
        groupCheckIntervalMinutesSpinner.setValue(Math.max(1, (group.checkIntervalSeconds() + 59) / 60));
        groupPointModel.setRowCount(0);
        for (GroupMonitorPoint point : group.points()) {
            groupPointModel.addRow(new Object[] {
                    point.role().name(), point.alias(), point.code(), point.enabled()});
        }
        groupSummaryLabel.setText("当前判断：未检测");
        pointStatusPanel.removeAll();
        pointStatusPanel.revalidate();
        pointStatusPanel.repaint();
    }

    private void addPointGroup() {
        try {
            if (!pointGroups.isEmpty() && groupList.getSelectedIndex() >= 0) {
                updateSelectedGroupFromForm();
            }
            String id = "group-" + System.currentTimeMillis();
            pointGroups.add(new PointGroupDefinition(
                    id,
                    "区域",
                    "物料组",
                    "物料",
                    true,
                    PointGroupDefinition.DEFAULT_CHECK_INTERVAL_SECONDS,
                    List.of(
                            new GroupMonitorPoint(id + "-use", "USE_POINT_001", "使用位", PointRole.USE, true, 1),
                            new GroupMonitorPoint(id + "-backup-1", "BACKUP_POINT_001", "备用位1", PointRole.BACKUP, true, 2),
                            new GroupMonitorPoint(id + "-backup-2", "BACKUP_POINT_002", "备用位2", PointRole.BACKUP, true, 3),
                            new GroupMonitorPoint(id + "-backup-3", "BACKUP_POINT_003", "备用位3", PointRole.BACKUP, true, 4),
                            new GroupMonitorPoint(id + "-backup-4", "BACKUP_POINT_004", "备用位4", PointRole.BACKUP, true, 5)),
                    new GroupAlertRule(true, true, 3, 5, true)));
            refreshGroupList(id);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void removeSelectedGroup() {
        int index = groupList.getSelectedIndex();
        if (index < 0 || index >= pointGroups.size()) {
            showError(new IllegalArgumentException("请先选择点位组"));
            return;
        }
        PointGroupDefinition removed = pointGroups.remove(index);
        synchronized (groupMonitorLock) {
            groupStates.remove(removed.id());
            lastGroupStatuses.remove(removed.id());
        }
        refreshGroupList(pointGroups.isEmpty() ? "" : pointGroups.get(0).id());
        if (pointGroups.isEmpty()) {
            groupPointModel.setRowCount(0);
        }
    }

    private void removeSelectedGroupPointRows() {
        int[] rows = groupPointTable.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            groupPointModel.removeRow(groupPointTable.convertRowIndexToModel(rows[i]));
        }
    }

    private List<PointGroupDefinition> readGroups() {
        if (!pointGroups.isEmpty() && groupList.getSelectedIndex() >= 0) {
            updateSelectedGroupFromForm();
        }
        if (pointGroups.isEmpty()) {
            throw new IllegalArgumentException("至少配置一个点位组");
        }
        GroupConfigStore.validateGroups(pointGroups);
        return new ArrayList<>(pointGroups);
    }

    List<PointGroupDefinition> captureMonitoredGroupsForTest() {
        return captureMonitoredGroups(readGroups());
    }

    List<PointGroupDefinition> monitoredGroupsSnapshotForTest() {
        return monitoredGroups;
    }

    private List<PointGroupDefinition> captureMonitoredGroups(List<PointGroupDefinition> groups) {
        monitoredGroups = List.copyOf(groups);
        return monitoredGroups;
    }

    private void updateSelectedGroupFromForm() {
        int index = groupList.getSelectedIndex();
        if (index < 0 || index >= pointGroups.size()) {
            return;
        }
        stopGroupTableEditing();
        PointGroupDefinition group = new PointGroupDefinition(
                groupIdField.getText(),
                groupAreaField.getText(),
                groupNameField.getText(),
                groupMaterialField.getText(),
                groupEnabledBox.isSelected(),
                ((Integer) groupCheckIntervalMinutesSpinner.getValue()) * 60,
                readGroupPoints(),
                new GroupAlertRule(
                        ruleEnabledBox.isSelected(),
                        requireUseEmptyBox.isSelected(),
                        (Integer) minBackupAvailableSpinner.getValue(),
                        (Integer) durationMinutesSpinner.getValue(),
                        backupThresholdParticipatesBox.isSelected()));
        pointGroups.set(index, group);
    }

    private List<GroupMonitorPoint> readGroupPoints() {
        List<GroupMonitorPoint> points = new ArrayList<>();
        for (int i = 0; i < groupPointModel.getRowCount(); i++) {
            String role = groupCellText(i, 0);
            String alias = groupCellText(i, 1);
            String code = groupCellText(i, 2);
            boolean enabled = groupCellBoolean(i, 3);
            if (!role.isEmpty() || !alias.isEmpty() || !code.isEmpty()) {
                if (role.isEmpty() || alias.isEmpty() || code.isEmpty()) {
                    throw new IllegalArgumentException("点位角色、别名、编码必须同时填写");
                }
                points.add(new GroupMonitorPoint(
                        groupIdField.getText().trim() + "-point-" + (i + 1),
                        code,
                        alias,
                        PointRole.valueOf(role),
                        enabled,
                        i + 1));
            }
        }
        return points;
    }

    private void stopGroupTableEditing() {
        if (groupPointTable.isEditing() && groupPointTable.getCellEditor() != null) {
            groupPointTable.getCellEditor().stopCellEditing();
        }
    }

    private String groupCellText(int row, int column) {
        Object value = groupPointModel.getValueAt(row, column);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean groupCellBoolean(int row, int column) {
        Object value = groupPointModel.getValueAt(row, column);
        return value instanceof Boolean ? (Boolean) value : Boolean.parseBoolean(String.valueOf(value));
    }

    private void saveGroupConfig() {
        try {
            List<PointGroupDefinition> groups = readGroups();
            groupConfigStore.save(groups);
            pointGroups = new ArrayList<>(groups);
            refreshGroupList(pointGroups.get(0).id());
            appendStatus("点位组配置已保存。数据库密码未保存。");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void checkNow() {
        try {
            DbConfig config = requireCurrentConfig(60);
            char[] passwordSnapshot = Arrays.copyOf(currentPassword, currentPassword.length);
            List<PointGroupDefinition> groups = List.copyOf(readGroups());
            groupConfigStore.save(groups);
            runOnceInBackground(() -> checkGroups(
                    config,
                    passwordSnapshot,
                    groups,
                    LocalDateTime.now(),
                    "手动检测"));
        } catch (Exception ex) {
            showError(ex);
            appendStatus("执行失败：" + ex.getMessage());
        }
    }

    private void checkDueGroups() throws Exception {
        DbConfig config = requireCurrentConfig(60);
        List<PointGroupDefinition> allGroups = monitoredGroups;
        LocalDateTime now = LocalDateTime.now();
        List<PointGroupDefinition> dueGroups;
        synchronized (groupMonitorLock) {
            dueGroups = GroupCheckPlanner.dueGroups(allGroups, groupStates, now);
        }
        if (dueGroups.isEmpty()) {
            return;
        }
        checkGroups(config, dueGroups, now, "自动检测");
    }

    private void checkGroups(
            DbConfig config,
            List<PointGroupDefinition> groups,
            LocalDateTime now,
            String source) {
        checkGroups(config, currentPassword, groups, now, source);
    }

    private void checkGroups(
            DbConfig config,
            char[] password,
            List<PointGroupDefinition> groups,
            LocalDateTime now,
            String source) {
        checkGroupsWithFetcher(
                groups,
                now,
                source,
                group -> pointRepository.fetch(config, password, pointDefinitions(group)));
    }

    GroupCheckRunResult checkGroupsWithFetcher(
            List<PointGroupDefinition> groups,
            LocalDateTime now,
            String source,
            GroupPointFetcher fetcher) {
        StringBuilder runtime = new StringBuilder();
        boolean dialogRequested = false;
        int checkedGroups = 0;
        int failedGroups = 0;
        List<GroupEvaluation> evaluations = new ArrayList<>();
        for (PointGroupDefinition group : groups) {
            GroupRuntimeState state;
            synchronized (groupMonitorLock) {
                state = groupStates.computeIfAbsent(group.id(), key -> new GroupRuntimeState());
                state.markChecked(now);
            }
            List<PointRecord> records;
            try {
                records = fetcher.fetch(group);
            } catch (Exception ex) {
                failedGroups++;
                String errorSummary = queryFailureMessage(ex);
                GroupEvaluation evaluation;
                synchronized (groupMonitorLock) {
                    evaluation = GroupMonitorLogic.queryFailed(group, state, now, errorSummary);
                }
                appendCheckLog(now, evaluation);
                appendGroupEvents(now, evaluation);
                evaluations.add(evaluation);
                closeActiveGroupAlertDialogIfOwnedBy(group.id());
                updateSelectedGroupBoard(evaluation);
                runtime.append(formatGroupCheckResult(source, List.of(), evaluation)).append(System.lineSeparator());
                appendStatus(source + "失败，点位组 " + group.id() + " " + errorSummary);
                continue;
            }
            GroupEvaluation evaluation;
            synchronized (groupMonitorLock) {
                evaluation = GroupMonitorLogic.evaluate(group, records, state, now);
            }
            appendCheckLog(now, evaluation);
            appendGroupEvents(now, evaluation);
            evaluations.add(evaluation);
            updateSelectedGroupBoard(evaluation);
            runtime.append(formatGroupCheckResult(source, records, evaluation)).append(System.lineSeparator());
            checkedGroups++;
            if (evaluation.shouldShowDialog() && !dialogRequested) {
                showGroupAlertDialog(evaluation);
                dialogRequested = true;
            }
        }
        String runtimeText = runtime.toString();
        SwingUtilities.invokeLater(() -> groupRuntimeArea.setText(runtimeText));
        appendStatus(source + "完成，点位组 " + checkedGroups + " 个"
                + (failedGroups > 0 ? "，失败 " + failedGroups + " 个" : "") + "。");
        return new GroupCheckRunResult(checkedGroups, failedGroups, dialogRequested, evaluations);
    }

    private void updateSelectedGroupBoard(GroupEvaluation evaluation) {
        SwingUtilities.invokeLater(() -> {
            if (!evaluation.groupId().equals(selectedGroupId())) {
                return;
            }
            String message = evaluation.message();
            if (message == null || message.isBlank()) {
                message = GroupStatusText.statusText(evaluation.status());
            }
            groupSummaryLabel.setText("当前判断：" + message);
            renderPointStatusBoard(evaluation);
        });
    }

    private String selectedGroupId() {
        int index = groupList.getSelectedIndex();
        if (index < 0 || index >= pointGroups.size()) {
            return "";
        }
        return pointGroups.get(index).id();
    }

    private List<PointDefinition> pointDefinitions(PointGroupDefinition group) {
        List<PointDefinition> points = new ArrayList<>();
        for (GroupMonitorPoint point : group.points()) {
            if (point.enabled()) {
                points.add(new PointDefinition(point.code(), point.alias()));
            }
        }
        return points;
    }

    private void appendCheckLog(LocalDateTime now, GroupEvaluation evaluation) {
        try {
            groupLogWriter.appendCheck(now, evaluation);
        } catch (Exception ex) {
            appendStatus("CSV检测日志写入失败：" + ex.getMessage());
        }
    }

    private void appendGroupEvents(LocalDateTime now, GroupEvaluation evaluation) {
        GroupAlertStatus previous;
        synchronized (groupMonitorLock) {
            previous = lastGroupStatuses.getOrDefault(evaluation.groupId(), GroupAlertStatus.NORMAL);
            lastGroupStatuses.put(evaluation.groupId(), evaluation.status());
        }
        try {
            if (evaluation.status() == GroupAlertStatus.QUERY_FAILED) {
                if (previous != GroupAlertStatus.QUERY_FAILED) {
                    groupLogWriter.appendEvent(now, "QUERY_FAILED", evaluation);
                }
            } else {
                if (previous == GroupAlertStatus.QUERY_FAILED) {
                    groupLogWriter.appendEvent(now, "QUERY_RECOVERED", evaluation);
                }
                if (evaluation.status() == GroupAlertStatus.ACTIVE_ALERT
                        && previous != GroupAlertStatus.ACTIVE_ALERT
                        && previous != GroupAlertStatus.ACKED_ALERT) {
                    groupLogWriter.appendEvent(now, "ALERT_OPEN", evaluation);
                } else if (evaluation.status() == GroupAlertStatus.NORMAL
                        && previous != GroupAlertStatus.NORMAL
                        && previous != GroupAlertStatus.QUERY_FAILED) {
                    groupLogWriter.appendEvent(now, "RECOVERED", evaluation);
                }
            }
        } catch (Exception ex) {
            appendStatus("CSV事件日志写入失败：" + ex.getMessage());
        }
    }

    private void renderPointStatusBoard(GroupEvaluation evaluation) {
        pointStatusPanel.removeAll();

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(6, 6, 6, 6);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;

        if (evaluation.status() == GroupAlertStatus.QUERY_FAILED) {
            JPanel card = new JPanel(new GridLayout(0, 1, 4, 4));
            card.setBackground(new Color(255, 250, 230));
            card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                    javax.swing.BorderFactory.createLineBorder(new Color(190, 120, 30), 2),
                    javax.swing.BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            JLabel title = new JLabel("查询失败");
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            title.setForeground(new Color(160, 92, 20));
            card.add(title);
            card.add(new JLabel(evaluation.message()));
            card.add(new JLabel("本次未获得点位状态，不按无料处理。"));
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 4;
            pointStatusPanel.add(card, constraints);
            pointStatusPanel.revalidate();
            pointStatusPanel.repaint();
            return;
        }

        List<PointStatusView> usePoints = new ArrayList<>();
        List<PointStatusView> backupPoints = new ArrayList<>();
        for (PointStatusView point : evaluation.pointStatuses()) {
            if (point.role() == PointRole.USE) {
                usePoints.add(point);
            } else {
                backupPoints.add(point);
            }
        }

        int row = 0;
        for (PointStatusView point : usePoints) {
            JPanel card = pointStatusCard(point);
            constraints.gridx = 0;
            constraints.gridy = row++;
            constraints.gridwidth = 4;
            pointStatusPanel.add(card, constraints);
        }

        constraints.gridwidth = 1;
        int col = 0;
        for (PointStatusView point : backupPoints) {
            JPanel card = pointStatusCard(point);
            constraints.gridx = col;
            constraints.gridy = row;
            pointStatusPanel.add(card, constraints);
            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }

        pointStatusPanel.revalidate();
        pointStatusPanel.repaint();
    }

    private JPanel pointStatusCard(PointStatusView point) {
        JPanel card = new JPanel(new GridLayout(0, 1, 4, 4));
        Color color = statusColor(point.status());
        card.setBackground(Color.WHITE);
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createLineBorder(color, 2),
                javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel title = new JLabel((point.role() == PointRole.USE ? "使用位：" : "备用位：") + point.alias());
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, point.role() == PointRole.USE ? 16 : 14));
        JLabel status = new JLabel(point.statusText());
        status.setFont(new Font(Font.SANS_SERIF, Font.BOLD, point.role() == PointRole.USE ? 26 : 22));
        status.setForeground(color);

        String shelfCode = point.shelfCode() == null || point.shelfCode().isBlank() ? "--" : point.shelfCode();
        String reason = point.reason() == null || point.reason().isBlank() ? "--" : point.reason();
        card.add(title);
        card.add(status);
        card.add(new JLabel("点位：" + point.pointCode()));
        card.add(new JLabel("货架：" + shelfCode));
        card.add(new JLabel("原因：" + reason));
        return card;
    }

    private Color statusColor(PointMaterialStatus status) {
        if (status == PointMaterialStatus.AVAILABLE) {
            return new Color(24, 128, 72);
        }
        if (status == PointMaterialStatus.EMPTY) {
            return new Color(190, 48, 48);
        }
        if (status == PointMaterialStatus.MISSING) {
            return new Color(112, 112, 112);
        }
        return new Color(150, 150, 150);
    }

    private String formatGroupCheckResult(String source, List<PointRecord> records, GroupEvaluation evaluation) {
        String message;
        if (evaluation.status() == GroupAlertStatus.QUERY_FAILED
                && evaluation.message() != null
                && !evaluation.message().isBlank()) {
            message = evaluation.message();
        } else {
            message = GroupStatusText.summary(
                    evaluation.areaName(),
                    evaluation.groupName(),
                    evaluation.materialName(),
                    evaluation.status(),
                    evaluation.usePointEmpty(),
                    evaluation.backupTotal(),
                    evaluation.backupAvailableCount(),
                    evaluation.continuousMatchedSeconds(),
                    evaluation.alertDurationSeconds(),
                    evaluation.pointStatuses());
        }
        return TIME_FORMAT.format(LocalDateTime.now())
                + " "
                + GroupStatusText.statusText(evaluation.status())
                + "："
                + message;
    }

    private static String queryFailureMessage(Exception ex) {
        return "查询失败：" + sanitizedExceptionSummary(ex);
    }

    private static String sanitizedExceptionSummary(Exception ex) {
        String raw = ex == null ? "" : ex.getMessage();
        if (raw == null || raw.isBlank()) {
            raw = ex == null ? "未知错误" : ex.getClass().getSimpleName();
        } else if (ex != null) {
            raw = ex.getClass().getSimpleName() + ": " + raw;
        }
        String safe = raw.replace('\r', ' ').replace('\n', ' ');
        safe = safe.replaceAll("(?i)jdbc:[^\\s,;]+", "jdbc:***");
        safe = safe.replaceAll("(?i)((?:password|passwd|pwd|token|access_token|secret)\\s*[=:]\\s*)[^\\s,;]+",
                "$1***");
        safe = safe.replaceAll("(?i)((?:user\\s*id|uid|user(?:name)?|role)\\s*[=:]\\s*)[^\\s,;]+",
                "$1***");
        safe = safe.replaceAll("(?i)user\\s+\"[^\"]+\"", "user ***");
        safe = safe.replaceAll("(?i)user\\s+'[^']+'", "user ***");
        safe = safe.replaceAll("(?i)role\\s+\"[^\"]+\"", "role ***");
        safe = safe.replaceAll("(?i)role\\s+'[^']+'", "role ***");
        safe = safe.replaceAll("\\bat\\s+[A-Za-z0-9_.$]+\\([^)]*\\)", "");
        safe = safe.replaceAll("[A-Za-z0-9_.$-]+\\.java:\\d+", "***.java:***");
        safe = safe.replaceAll("\\s+", " ").trim();
        if (safe.length() > 180) {
            safe = safe.substring(0, 177) + "...";
        }
        return safe.isBlank() ? "未知错误" : safe;
    }

    private void closeActiveGroupAlertDialogIfOwnedBy(String groupId) {
        SwingUtilities.invokeLater(() -> {
            if (activeDialog == null || groupId == null || !groupId.equals(activeDialogGroupId)) {
                return;
            }
            activeDialog.dispose();
            activeDialog = null;
            activeDialogGroupId = "";
        });
    }

    private void startMonitoring() {
        try {
            DbConfig config = requireCurrentConfig(60);
            List<PointGroupDefinition> groups = readGroups();
            groupConfigStore.save(groups);
            stopMonitoring();
            captureMonitoredGroups(groups);
            clearGroupMonitorState();
            scheduledTask = executor.scheduleWithFixedDelay(
                    () -> runWithUiErrorHandling(this::checkDueGroups),
                    0,
                    10,
                    TimeUnit.SECONDS);
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            appendStatus("已开始点位组监控。系统每 10 秒扫描到期点位组，各组按自身检测周期查询数据库。");
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void stopMonitoring() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
        monitoredGroups = List.of();
        clearGroupMonitorState();
        startButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void clearGroupMonitorState() {
        synchronized (groupMonitorLock) {
            groupStates.clear();
            lastGroupStatuses.clear();
        }
    }

    private void showGroupAlertDialog(GroupEvaluation evaluation) {
        SwingUtilities.invokeLater(() -> {
            if (activeDialog != null && activeDialog.isShowing()) {
                return;
            }
            JDialog dialog = new JDialog(this, "点位组缺料报警", false);
            dialog.setAlwaysOnTop(true);
            dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            dialog.setLayout(new BorderLayout(12, 12));
            dialog.getRootPane().setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));

            JTextArea text = new JTextArea(groupAlertText(evaluation));
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            text.setBackground(new Color(255, 250, 240));
            dialog.add(text, BorderLayout.CENTER);

            dialog.add(buildGroupAlertButtons(evaluation, () -> {
                dialog.dispose();
                activeDialog = null;
                activeDialogGroupId = "";
            }), BorderLayout.SOUTH);

            dialog.setSize(640, 420);
            dialog.setLocationRelativeTo(this);
            activeDialog = dialog;
            activeDialogGroupId = evaluation.groupId();
            dialog.setVisible(true);
        });
    }

    private String groupAlertText(GroupEvaluation evaluation) {
        String lineSeparator = System.lineSeparator();
        return "检测时间：" + TIME_FORMAT.format(LocalDateTime.now())
                + lineSeparator
                + evaluation.areaName()
                + " / "
                + evaluation.groupName()
                + " "
                + alertHeadlineStatusText(evaluation.status())
                + lineSeparator
                + "物料：" + evaluation.materialName()
                + lineSeparator
                + "使用位：" + (evaluation.usePointEmpty() ? "无料" : "有料")
                + lineSeparator
                + "备用位：" + evaluation.backupAvailableCount() + "/" + evaluation.backupTotal() + " 有料"
                + lineSeparator
                + "持续：" + evaluation.continuousMatchedMinutes() + " 分钟"
                + lineSeparator
                + lineSeparator
                + "异常点位列表："
                + lineSeparator
                + abnormalPointText(evaluation)
                + lineSeparator
                + lineSeparator
                + "报警条件已达到报警时间，请现场确认补料或调度状态。";
    }

    private String abnormalPointText(GroupEvaluation evaluation) {
        StringBuilder builder = new StringBuilder();
        for (PointStatusView point : evaluation.pointStatuses()) {
            if (point.status() != PointMaterialStatus.EMPTY && point.status() != PointMaterialStatus.MISSING) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(System.lineSeparator());
            }
            builder.append(roleText(point.role()))
                    .append(" ")
                    .append(point.pointCode())
                    .append(" ")
                    .append(point.statusText())
                    .append(" 原因：")
                    .append(point.reason() == null || point.reason().isBlank() ? "未填写原因" : point.reason());
        }
        if (builder.length() == 0) {
            return "无异常点位明细";
        }
        return builder.toString();
    }

    private String alertHeadlineStatusText(GroupAlertStatus status) {
        if (status == GroupAlertStatus.ACTIVE_ALERT) {
            return "需要关注";
        }
        return GroupStatusText.statusText(status);
    }

    private JPanel buildGroupAlertButtons(GroupEvaluation evaluation, Runnable closeDialog) {
        return buildGroupAlertButtons(evaluation, this::openLogs, closeDialog);
    }

    JPanel buildGroupAlertButtons(GroupEvaluation evaluation, Runnable openLogsAction, Runnable closeDialog) {
        JButton openLogsButton = new JButton("打开日志目录");
        openLogsButton.addActionListener(e -> {
            if (openLogsAction != null) {
                openLogsAction.run();
            }
        });

        JButton ack = new JButton("已关注");
        ack.addActionListener(e -> {
            acknowledgeGroupAlert(evaluation);
            if (closeDialog != null) {
                closeDialog.run();
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(openLogsButton);
        buttons.add(ack);
        return buttons;
    }

    private void acknowledgeGroupAlert(GroupEvaluation evaluation) {
        synchronized (groupMonitorLock) {
            GroupRuntimeState state = groupStates.get(evaluation.groupId());
            if (state != null) {
                state.acknowledge();
            }
            lastGroupStatuses.put(evaluation.groupId(), GroupAlertStatus.ACKED_ALERT);
        }
        try {
            groupLogWriter.appendEvent(LocalDateTime.now(), "ACKNOWLEDGED", evaluation);
        } catch (Exception ex) {
            appendStatus("CSV事件日志写入失败：" + ex.getMessage());
        }
        appendStatus("用户已关注点位组报警：" + evaluation.groupId());
    }

    private String roleText(PointRole role) {
        if (role == PointRole.USE) {
            return "使用位";
        }
        if (role == PointRole.BACKUP) {
            return "备用位";
        }
        return "点位";
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

    private void checkNowLegacy() throws Exception {
        DbConfig config = requireCurrentConfig((Integer) intervalSpinner.getValue());
        List<PointDefinition> points = pointSchedule.forceAll(readPoints());
        checkPointsLegacy(config, points, false, LocalDateTime.now(), "手动检测");
    }

    private void checkDuePointsLegacy() throws Exception {
        DbConfig config = requireCurrentConfig((Integer) intervalSpinner.getValue());
        List<PointDefinition> allPoints = readPoints();
        LocalDateTime now = LocalDateTime.now();
        List<PointDefinition> duePoints = pointSchedule.duePoints(allPoints, now);
        if (duePoints.isEmpty()) {
            return;
        }
        checkPointsLegacy(config, duePoints, true, now, "自动检测");
    }

    private void checkPointsLegacy(
            DbConfig config,
            List<PointDefinition> points,
            boolean updateSchedule,
            LocalDateTime now,
            String source) throws Exception {
        List<PointRecord> records = pointRepository.fetch(config, currentPassword, points);
        MonitorEvaluation evaluation = MonitorLogic.evaluate(points, records, alertState);
        appendStatus(formatCheckResultLegacy(points, records, evaluation, source));
        if (updateSchedule) {
            pointSchedule.markChecked(points, now);
        }
        if (evaluation.hasActiveAlert()) {
            showAlertDialog(evaluation);
        }
    }

    private String formatCheckResultLegacy(
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

    private void startMonitoringLegacy() {
        try {
            DbConfig config = requireCurrentConfig((Integer) intervalSpinner.getValue());
            List<PointDefinition> points = readPoints();
            configStore.save(config, points);
            stopMonitoringLegacy();
            pointSchedule.clear();
            scheduledTask = executor.scheduleWithFixedDelay(
                    () -> runWithUiErrorHandling(this::checkDuePointsLegacy),
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

    private void stopMonitoringLegacy() {
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
                activeDialogGroupId = "";
            });
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.add(ack);
            dialog.add(buttons, BorderLayout.SOUTH);

            dialog.setSize(520, 320);
            dialog.setLocationRelativeTo(this);
            activeDialog = dialog;
            activeDialogGroupId = "";
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

    @FunctionalInterface
    interface GroupPointFetcher {
        List<PointRecord> fetch(PointGroupDefinition group) throws Exception;
    }

    static final class GroupCheckRunResult {
        private final int checkedGroups;
        private final int failedGroups;
        private final boolean dialogRequested;
        private final List<GroupEvaluation> evaluations;

        GroupCheckRunResult(
                int checkedGroups,
                int failedGroups,
                boolean dialogRequested,
                List<GroupEvaluation> evaluations) {
            this.checkedGroups = checkedGroups;
            this.failedGroups = failedGroups;
            this.dialogRequested = dialogRequested;
            this.evaluations = List.copyOf(evaluations);
        }

        int checkedGroups() {
            return checkedGroups;
        }

        int failedGroups() {
            return failedGroups;
        }

        boolean dialogRequested() {
            return dialogRequested;
        }

        List<GroupEvaluation> evaluations() {
            return evaluations;
        }
    }

    private interface CheckedRunnable {
        void run() throws Exception;
    }
}
