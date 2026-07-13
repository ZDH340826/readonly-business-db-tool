package com.local.monitor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.function.BooleanSupplier;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
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
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public final class ShelfPointMonitorApp
extends JFrame {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String EXPECTED_SELF_TEST_VERSION = "0.4.0";
    private static final Pattern PRIVATE_10_NET_PATTERN = Pattern.compile("\\b10(?:\\.\\d{1,3}){3}\\b");
    private static final Pattern REAL_POINT_CODE_PATTERN = Pattern.compile("\\b\\d{6}BB\\d{6}\\b");
    private static final String PAGE_OVERVIEW = "\u76d1\u63a7\u603b\u89c8";
    private static final String PAGE_GROUPS = "\u70b9\u4f4d\u7ec4\u7ba1\u7406";
    private static final String PAGE_ALERT_CENTER = "\u62a5\u8b66\u4e2d\u5fc3";
    private static final String PAGE_CONNECTIONS = "\u8fde\u63a5\u7ba1\u7406";
    private static final String PAGE_DATA_QUERY = "\u6570\u636e\u67e5\u8be2";
    private static final String PAGE_BROWSER = "\u6570\u636e\u6e90\u6d4f\u89c8\u5668";
    private static final String PAGE_LOGS = "\u65e5\u5fd7\u4e0e\u7cfb\u7edf";
    private static final String PAGE_SETTINGS = "\u7cfb\u7edf\u8bbe\u7f6e";
    private final ConfigStore configStore = new ConfigStore(Paths.get("data", "config.properties"));
    private final GroupConfigStore groupConfigStore = new GroupConfigStore(Paths.get("data", "group-config.properties"));
    private final ConnectionProfileStore profileStore = new ConnectionProfileStore(Paths.get("data", "connections.properties"));
    private final UiPreferencesStore uiPreferencesStore = new UiPreferencesStore(Paths.get("data", "ui-settings.properties"));
    private final PointRepository pointRepository = new PointRepository();
    private final DbMetadataRepository metadataRepository = new DbMetadataRepository();
    private final PointDataQueryRepository pointDataQueryRepository = new PointDataQueryRepository();
    private final AlertState alertState = new AlertState();
    private final PointSchedule pointSchedule = new PointSchedule();
    private final GroupLogWriter groupLogWriter = new GroupLogWriter(Paths.get("logs"));
    private ScheduledExecutorService monitorExecutor = ShelfPointMonitorApp.newDaemonExecutor("shelf-point-monitor");
    private ScheduledExecutorService ioExecutor = ShelfPointMonitorApp.newDaemonExecutor("shelf-point-io");
    private final Path logPath = Paths.get("logs", "monitor.log");
    private UiPreferences uiPreferences = this.uiPreferencesStore.load();
    private final JList<String> navigationList = new JList<String>(UiPreferences.pageNames().toArray(String[]::new));
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(this.cardLayout);
    private final JLabel currentConnectionLabel = new JLabel("\u5f53\u524d\u8fde\u63a5\uff1a\u672a\u8fde\u63a5");
    private final JLabel monitorStatusLabel = new JLabel("\u76d1\u63a7\u72b6\u6001\uff1a\u672a\u8fd0\u884c");
    private final JLabel lastCheckLabel = new JLabel("\u4e0a\u6b21\u68c0\u6d4b\uff1a--");
    private final JLabel nextCheckLabel = new JLabel("\u4e0b\u6b21\u68c0\u6d4b\uff1a--");
    private final JLabel bottomStatusLabel = new JLabel("\u5c31\u7eea");
    private final DefaultListModel<String> profileListModel = new DefaultListModel<>();
    private final JList<String> profileList = new JList<String>(this.profileListModel);
    private final JTextField profileIdField = new JTextField(12);
    private final JTextField profileNameField = new JTextField(14);
    private final JComboBox<String> profileDbTypeBox = new JComboBox<String>(new String[]{"postgres", "h2"});
    private final JTextField profileHostField = new JTextField(16);
    private final JSpinner profilePortSpinner = new JSpinner(new SpinnerNumberModel(2345, 1, 65535, 1));
    private final JTextField profileDatabaseField = new JTextField(12);
    private final JTextField profileSchemaField = new JTextField(10);
    private final JTextField profileUserField = new JTextField(14);
    private final JComboBox<String> profileSslModeBox = new JComboBox<String>(new String[]{"disable", "prefer", "require"});
    private final JTextField profileLocalPathField = new JTextField(18);
    private final JPasswordField profilePasswordField = new JPasswordField(14);
    private final StatusBadge profileTestStatusLabel = new StatusBadge();
    private final DefaultComboBoxModel<String> schemaModel = new DefaultComboBoxModel<>();
    private final JComboBox<String> schemaBox = new JComboBox<String>(this.schemaModel);
    private final DefaultMutableTreeNode dataSourceTreeRoot = new DefaultMutableTreeNode("\u6570\u636e\u6e90");
    private final JTree dataSourceTree = new JTree(this.dataSourceTreeRoot);
    private final DefaultTableModel browserTableModel = new DefaultTableModel(new Object[]{"Schema", "Name", "Type"}, 0);
    private final JTable browserTable = new JTable(this.browserTableModel);
    private final DefaultTableModel columnModel = new DefaultTableModel(new Object[]{"\u5217\u540d", "\u6570\u636e\u7c7b\u578b", "\u957f\u5ea6", "\u53ef\u7a7a", "\u9ed8\u8ba4\u503c", "\u5907\u6ce8"}, 0);
    private final JTable columnTable = new JTable(this.columnModel);
    private final DefaultTableModel previewModel = new DefaultTableModel();
    private final JTable previewTable = new JTable(this.previewModel);
    private final JLabel schemaCountLabel = new JLabel("Schema \u6570\u91cf\uff1a--");
    private final JLabel objectCountLabel = new JLabel("\u8868 / \u89c6\u56fe\u6570\u91cf\uff1a--");
    private final JLabel objectTypeLabel = new JLabel("\u5f53\u524d\u5bf9\u8c61\u7c7b\u578b\uff1a--");
    private final JLabel browserModeLabel = new JLabel("\u8fde\u63a5\u6a21\u5f0f\uff1a\u53ea\u8bfb");
    private final DefaultTableModel overviewModel = new DefaultTableModel(new Object[]{"\u72b6\u6001", "\u533a\u57df", "\u7269\u6599\u7ec4", "\u4f7f\u7528\u4f4d\u72b6\u6001", "\u5907\u7528\u4f4d\u53ef\u7528\u6570", "\u6301\u7eed\u65f6\u95f4", "\u4e0a\u6b21\u68c0\u6d4b"}, 0){

        @Override
        public boolean isCellEditable(int index, int index2) {
            return false;
        }
    };
    private final JTable overviewTable = new JTable(this.overviewModel);
    private final List<String> overviewRowGroupIds = new ArrayList<String>();
    private final JTextArea overviewDetailArea = new JTextArea();
    private final JLabel overviewGroupCountLabel = new JLabel("0");
    private final JLabel overviewAlertCountLabel = new JLabel("0");
    private final JLabel overviewPendingCountLabel = new JLabel("0");
    private final JLabel overviewDataErrorCountLabel = new JLabel("0");
    private final DefaultTableModel alertCenterModel = new DefaultTableModel(new Object[]{"\u65f6\u95f4", "\u533a\u57df", "\u7269\u6599\u7ec4", "\u539f\u56e0", "\u6301\u7eed\u65f6\u95f4", "\u5904\u7406\u72b6\u6001"}, 0){

        @Override
        public boolean isCellEditable(int index, int index2) {
            return false;
        }
    };
    private final JTable alertCenterTable = new JTable(this.alertCenterModel);
    private final JComboBox<String> alertCenterFilterBox = new JComboBox<String>(new String[]{"\u6d3b\u8dc3\u62a5\u8b66", "\u5df2\u5173\u6ce8", "\u89c2\u5bdf\u4e2d", "\u67e5\u8be2\u5931\u8d25", "\u5df2\u6062\u590d"});
    private List<AlertCenterEntry> alertCenterEntries = List.of();
    private final JTextArea alertCenterDetailArea = new JTextArea();
    private final JTextField queryPointKeywordField = new JTextField(12);
    private final JTextField queryShelfKeywordField = new JTextField(12);
    private final JTextField queryAreaCodeField = new JTextField(10);
    private final JTextField queryRelateAreaCodeField = new JTextField(10);
    private final JTextField queryUpdatedFromField = new JTextField(14);
    private final JTextField queryUpdatedToField = new JTextField(14);
    private final JSpinner queryLimitSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 500, 10));
    private final JButton queryPrevButton = new JButton("\u4e0a\u4e00\u9875");
    private final JButton queryNextButton = new JButton("\u4e0b\u4e00\u9875");
    private final JLabel queryPageLabel = new JLabel("\u7b2c 0 / 0 \u9875");
    private final JLabel queryTotalLabel = new JLabel("\u603b\u8bb0\u5f55\u6570\uff1a0");
    private int queryCurrentPage = 1;
    private int queryTotalCount = 0;
    private PointDataQuery lastPointDataQuery;
    private final DefaultTableModel dataQueryModel = new DefaultTableModel(new Object[]{"\u70b9\u4f4d\u7f16\u7801", "\u8d27\u67b6\u7f16\u53f7", "\u8d27\u67b6\u72b6\u6001", "\u72b6\u6001", "\u9501\u5b9a\u72b6\u6001", "\u533a\u57df\u7f16\u7801", "\u5173\u8054\u533a\u57df", "\u66f4\u65b0\u65f6\u95f4", "\u67e5\u8be2\u72b6\u6001"}, 0){

        @Override
        public boolean isCellEditable(int index, int index2) {
            return false;
        }
    };
    private final JTable dataQueryTable = new JTable(this.dataQueryModel);
    private final JTextArea dataQueryDetailArea = new JTextArea();
    private final DefaultTableModel systemLogModel = new DefaultTableModel(new Object[]{"\u65f6\u95f4", "\u4e8b\u4ef6\u7c7b\u578b", "\u7ea7\u522b", "\u70b9\u4f4d\u7ec4", "\u63cf\u8ff0", "\u6765\u6e90"}, 0){

        @Override
        public boolean isCellEditable(int index, int index2) {
            return false;
        }
    };
    private final JTable systemLogTable = new JTable(this.systemLogModel);
    private final JComboBox<String> systemLogTypeFilterBox = new JComboBox<String>(new String[]{"\u5168\u90e8", "\u62a5\u8b66\u89e6\u53d1", "\u5df2\u5173\u6ce8", "\u6062\u590d", "\u67e5\u8be2\u5931\u8d25", "\u67e5\u8be2\u6062\u590d", "\u68c0\u6d4b\u5b8c\u6210", "\u8fde\u63a5\u6210\u529f"});
    private final JTextField systemLogFromField = new JTextField(12);
    private final JTextField systemLogToField = new JTextField(12);
    private final JTextField systemLogGroupField = new JTextField(10);
    private final JTextField systemLogKeywordField = new JTextField(12);
    private final JTextArea systemLogDetailArea = new JTextArea();
    private final JLabel schedulerHealthLabel = new JLabel("\u76d1\u63a7\u8c03\u5ea6\u5668\uff1a\u672a\u8fd0\u884c");
    private final JLabel connectionHealthLabel = new JLabel("\u5f53\u524d\u8fde\u63a5\uff1a\u672a\u8fde\u63a5");
    private final JLabel detectionHealthLabel = new JLabel("\u6700\u8fd1\u4e00\u6b21\u68c0\u6d4b\uff1a\u672a\u68c0\u6d4b");
    private final JLabel configHealthLabel = new JLabel("\u914d\u7f6e\u6587\u4ef6\uff1a\u672a\u68c0\u67e5");
    private final JLabel logDirHealthLabel = new JLabel("\u65e5\u5fd7\u76ee\u5f55\uff1a\u672a\u68c0\u67e5");
    private final JLabel selfTestHealthLabel = new JLabel("\u81ea\u68c0\u72b6\u6001\uff1a\u672a\u6267\u884c");
    private volatile String latestDetectionHealth = "\u672a\u68c0\u6d4b";
    private volatile String latestSelfTestHealth = "\u672a\u6267\u884c";
    private volatile String currentConnectionHealth = "\u672a\u9009\u62e9\u8fde\u63a5";
    private final JComboBox<String> settingsDefaultPageBox = new JComboBox<String>(UiPreferences.pageNames().toArray(String[]::new));
    private final JSpinner settingsOverviewRefreshSpinner = new JSpinner(new SpinnerNumberModel(10, 5, 3600, 5));
    private final JCheckBox settingsAlertPopupBox = new JCheckBox("\u62a5\u8b66\u5f39\u7a97\u542f\u7528");
    private final JCheckBox settingsAlertSoundBox = new JCheckBox("\u62a5\u8b66\u58f0\u97f3\u63d0\u793a");
    private final JSpinner settingsLogRetentionSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 3650, 1));
    private final JComboBox<String> settingsDensityBox = new JComboBox<String>(UiPreferences.densities().toArray(String[]::new));
    private final JCheckBox settingsStartupSelfTestBox = new JCheckBox("\u542f\u52a8\u65f6\u6267\u884c\u81ea\u68c0");
    private final JCheckBox settingsAutoCleanupLogsBox = new JCheckBox("\u65e5\u5fd7\u81ea\u52a8\u6e05\u7406");
    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(10, 10, 86400, 10));
    private final DefaultTableModel pointModel = new DefaultTableModel(new Object[]{"\u522b\u540d", "\u70b9\u4f4d\u7f16\u7801", "\u76d1\u6d4b\u5468\u671f(\u5206\u949f)"}, 0){

        @Override
        public Class<?> getColumnClass(int index) {
            return index == 2 ? Integer.class : String.class;
        }
    };
    private final JTable pointTable = new JTable(this.pointModel);
    private final JButton startButton = new JButton("\u5f00\u59cb\u76d1\u63a7");
    private final JButton stopButton = new JButton("\u505c\u6b62");
    private final JButton checkButton = new JButton("\u7acb\u5373\u68c0\u6d4b");
    private final JTextArea statusArea = new JTextArea();
    private final DefaultListModel<String> groupListModel = new DefaultListModel<>();
    private final JList<String> groupList = new JList<String>(this.groupListModel);
    private final JTextField groupIdField = new JTextField(16);
    private final JTextField groupAreaField = new JTextField(14);
    private final JTextField groupNameField = new JTextField(14);
    private final JTextField groupMaterialField = new JTextField(14);
    private final JCheckBox groupEnabledBox = new JCheckBox("\u542f\u7528");
    private final JCheckBox ruleEnabledBox = new JCheckBox("\u542f\u7528\u89c4\u5219", true);
    private final JCheckBox requireUseEmptyBox = new JCheckBox("\u4f7f\u7528\u4f4d\u65e0\u8d27\u67b6", true);
    private final JCheckBox backupThresholdParticipatesBox = new JCheckBox("\u5907\u7528\u4f4d\u4e0b\u9650\u53c2\u4e0e\u62a5\u8b66", true);
    private final JSpinner minBackupAvailableSpinner = new JSpinner(new SpinnerNumberModel(3, 0, 999, 1));
    private final JSpinner durationMinutesSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 1440, 1));
    private final JSpinner groupCheckIntervalMinutesSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1440, 1));
    private final DefaultTableModel groupPointModel = new DefaultTableModel(new Object[]{"\u89d2\u8272", "\u522b\u540d", "\u70b9\u4f4d\u7f16\u7801", "\u542f\u7528"}, 0){

        @Override
        public Class<?> getColumnClass(int index) {
            return index == 3 ? Boolean.class : String.class;
        }
    };
    private final JTable groupPointTable = new JTable(this.groupPointModel);
    private final JPanel pointStatusPanel = new JPanel(new GridBagLayout());
    private final JLabel groupSummaryLabel = new JLabel("\u5f53\u524d\u5224\u65ad\uff1a\u672a\u68c0\u6d4b");
    private final JTextArea groupRuntimeArea = new JTextArea();
    private List<ConnectionProfile> profiles = new ArrayList<ConnectionProfile>();
    private List<PointGroupDefinition> pointGroups = new ArrayList<PointGroupDefinition>();
    private volatile List<PointGroupDefinition> monitoredGroups = List.of();
    private volatile List<PointDefinition> monitoredLegacyPoints = List.of();
    private final Object groupMonitorLock = new Object();
    private final Map<String, GroupRuntimeState> groupStates = new LinkedHashMap<String, GroupRuntimeState>();
    private final Map<String, GroupAlertStatus> lastGroupStatuses = new LinkedHashMap<String, GroupAlertStatus>();
    private final Map<String, GroupEvaluation> lastGroupEvaluations = new LinkedHashMap<String, GroupEvaluation>();
    private final Object oneShotSessionLock = new Object();
    private final Set<MonitoringSession> inFlightOneShotSessions = new LinkedHashSet<MonitoringSession>();
    private ConnectionProfile currentProfile;
    private char[] currentPassword = new char[0];
    private ScheduledFuture<?> scheduledTask;
    private Timer overviewRefreshTimer;
    private Timer systemLogRefreshDebounceTimer;
    private long lastSystemLogRefreshAtMillis;
    private volatile MonitoringSession monitoringSession;
    private volatile long monitoringGeneration;
    private volatile long connectionOperationGeneration;
    private JDialog activeDialog;
    private String activeDialogGroupId = "";
    private long activeDialogGeneration = -1L;

    public static void main(String[] commandLineArguments) {
        if (commandLineArguments.length > 0 && "--self-test".equals(commandLineArguments[0])) {
            try {
                ShelfPointMonitorApp.runSelfTest(ShelfPointMonitorApp.resolveSelfTestAppRoot());
                System.out.println("ShelfPointMonitor SELF_TEST_OK");
            }
            catch (Exception exception) {
                System.err.println("ShelfPointMonitor SELF_TEST_FAILED: " + ShelfPointMonitorApp.userVisibleErrorMessage(exception));
                System.exit(2);
            }
            return;
        }
        SwingUtilities.invokeLater(() -> {
            ShelfPointMonitorApp.setLookAndFeel();
            ShelfPointMonitorApp shelfPointMonitorApp = new ShelfPointMonitorApp();
            shelfPointMonitorApp.setVisible(true);
        });
    }

    public ShelfPointMonitorApp() {
        super("\u53ea\u8bfb\u4e1a\u52a1\u6570\u636e\u5e93\u5de5\u5177");
        AppTheme.install();
        this.setDefaultCloseOperation(3);
        this.setPreferredSize(AppTheme.PREFERRED_WINDOW_SIZE);
        this.setMinimumSize(AppTheme.MINIMUM_WINDOW_SIZE);
        this.setSize(AppTheme.PREFERRED_WINDOW_SIZE);
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                ShelfPointMonitorApp.this.stopMonitoring();
                ShelfPointMonitorApp.this.shutdownExecutors();
                if (ShelfPointMonitorApp.this.overviewRefreshTimer != null) {
                    ShelfPointMonitorApp.this.overviewRefreshTimer.stop();
                }
                if (ShelfPointMonitorApp.this.systemLogRefreshDebounceTimer != null) {
                    ShelfPointMonitorApp.this.systemLogRefreshDebounceTimer.stop();
                }
            }
        });
        this.buildUi();
        this.loadProfiles();
        this.loadGroupConfig();
        this.loadSettingsForm();
        this.applyUiPreferences();
        this.selectDefaultPage();
        this.appendStatus("\u7a0b\u5e8f\u5df2\u542f\u52a8\u3002\u5bc6\u7801\u53ea\u4fdd\u5b58\u5728\u672c\u6b21\u8fd0\u884c\u5185\uff0c\u4e0d\u5199\u5165\u914d\u7f6e\u6587\u4ef6\u3002");
        if (this.uiPreferences.startupSelfTestEnabled()) {
            this.runIoInBackground(() -> {
                ShelfPointMonitorApp.runSelfTest(ShelfPointMonitorApp.resolveSelfTestAppRoot());
                SwingUtilities.invokeLater(() -> this.appendStatus("\u542f\u52a8\u81ea\u68c0\u901a\u8fc7\u3002"));
            });
        }
    }

    @Override
    public void dispose() {
        this.stopMonitoring();
        this.shutdownExecutors();
        if (this.overviewRefreshTimer != null) {
            this.overviewRefreshTimer.stop();
        }
        if (this.systemLogRefreshDebounceTimer != null) {
            this.systemLogRefreshDebounceTimer.stop();
        }
        super.dispose();
    }

    private static ScheduledExecutorService newDaemonExecutor(String threadName) {
        return Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    private void shutdownExecutors() {
        this.monitorExecutor.shutdownNow();
        this.ioExecutor.shutdownNow();
    }

    static void runSelfTestForTest(Path path) throws Exception {
        ShelfPointMonitorApp.runSelfTest(path);
    }

    private static Path resolveSelfTestAppRoot() throws Exception {
        String text = System.getProperty("shelf.monitor.appRoot");
        if (text != null && !text.isBlank()) {
            return Path.of(text).toAbsolutePath().normalize();
        }
        Path path = Path.of(ShelfPointMonitorApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isRegularFile(path)) {
            return path.getParent().toAbsolutePath().normalize();
        }
        return Path.of("").toAbsolutePath().normalize();
    }

    private static void runSelfTest(Path path) throws Exception {
        Path path2 = path.toAbsolutePath().normalize();
        ShelfPointMonitorApp.requireFile(path2.resolve("ShelfPointMonitor.jar"), "packaged application jar");
        String text = Files.readString(ShelfPointMonitorApp.requireFile(path2.resolve("VERSION"), "VERSION"), StandardCharsets.UTF_8).trim();
        if (!EXPECTED_SELF_TEST_VERSION.equals(text)) {
            throw new IllegalStateException("VERSION must be 0.4.0, actual=" + text);
        }
        ShelfPointMonitorApp.requireFile(path2.resolve("lib/postgresql-42.2.25.jar"), "PostgreSQL JDBC driver");
        ShelfPointMonitorApp.requireFile(path2.resolve("lib/h2-2.2.224.jar"), "H2 JDBC driver");
        Class.forName("org.postgresql.Driver");
        Class.forName("org.h2.Driver");
        Path path3 = ShelfPointMonitorApp.requireDirectory(path2.resolve("data"), "data directory");
        Path path4 = ShelfPointMonitorApp.requireFile(path3.resolve("config.properties"), "data/config.properties");
        Path path5 = ShelfPointMonitorApp.requireFile(path3.resolve("connections.properties"), "data/connections.properties");
        Path path6 = ShelfPointMonitorApp.requireFile(path3.resolve("group-config.properties"), "data/group-config.properties");
        Properties properties = ShelfPointMonitorApp.loadSelfTestProperties(path4);
        Properties properties2 = ShelfPointMonitorApp.loadSelfTestProperties(path5);
        Properties properties3 = ShelfPointMonitorApp.loadSelfTestProperties(path6);
        ShelfPointMonitorApp.assertNoPasswordProperty(properties, "config.properties");
        ShelfPointMonitorApp.assertNoPasswordProperty(properties2, "connections.properties");
        ShelfPointMonitorApp.assertNoPasswordProperty(properties3, "group-config.properties");
        ShelfPointMonitorApp.assertNoSensitiveProperties(properties, "config.properties");
        ShelfPointMonitorApp.assertNoSensitiveProperties(properties2, "connections.properties");
        ShelfPointMonitorApp.assertNoSensitiveProperties(properties3, "group-config.properties");
        ConfigStore.StoredConfig storedConfig = new ConfigStore(path4).load();
        if (!"__SITE_HOST__".equals(storedConfig.host) || !"__SITE_USER__".equals(storedConfig.user)) {
            throw new IllegalStateException("config.properties must use site placeholders");
        }
        if (!"data/local-test-db".equals(storedConfig.localPath)) {
            throw new IllegalStateException("config.properties localPath must remain data/local-test-db");
        }
        ShelfPointMonitorApp.assertSamplePointDefinitions(storedConfig.points, "config.properties points");
        ConnectionProfileStore.StoredProfiles storedProfiles = new ConnectionProfileStore(path5).load();
        boolean flag = false;
        boolean bl2 = false;
        for (ConnectionProfile value2 : storedProfiles.profiles()) {
            if ("__SITE_HOST__".equals(value2.host()) && "__SITE_USER__".equals(value2.user())) {
                flag = true;
            }
            if (!"h2".equals(value2.dbType())) continue;
            bl2 = true;
        }
        if (!flag || !bl2) {
            throw new IllegalStateException("connections.properties must include placeholder and local profiles");
        }
        List<PointGroupDefinition> list = new GroupConfigStore(path6).load();
        if (list.isEmpty()) {
            throw new IllegalStateException("group-config.properties must include a sample group");
        }
        PointGroupDefinition pointGroupDefinition = (PointGroupDefinition)list.get(0);
        if (!pointGroupDefinition.rule().backupThresholdParticipates()) {
            throw new IllegalStateException("sample group must enable backupThresholdParticipates");
        }
        ShelfPointMonitorApp.assertSampleGroupPoints(pointGroupDefinition.points(), "group-config.properties points");
        Path path7 = path2.resolve(storedConfig.localPath).normalize();
        if (!path7.startsWith(path2)) {
            throw new IllegalStateException("local test database path must stay under packaged root");
        }
        LocalTestDatabase.reset(DbConfig.localTest(path7.toString(), 30));
    }

    private static Path requireFile(Path path, String text) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException(text + " is missing: " + String.valueOf(path));
        }
        return path;
    }

    private static Path requireDirectory(Path path, String text) {
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException(text + " is missing: " + String.valueOf(path));
        }
        return path;
    }

    private static Properties loadSelfTestProperties(Path path) throws Exception {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path);){
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static void assertNoPasswordProperty(Properties properties, String text) {
        for (String text2 : properties.stringPropertyNames()) {
            if (!text2.toLowerCase(Locale.ROOT).contains("password")) continue;
            throw new IllegalStateException(text + " must not contain password property: " + text2);
        }
    }

    private static void assertNoSensitiveProperties(Properties properties, String text) {
        for (String text2 : properties.stringPropertyNames()) {
            ShelfPointMonitorApp.assertNotSensitive(text + "." + text2, properties.getProperty(text2, ""));
        }
    }

    private static void assertNotSensitive(String text, String text2) {
        if (PRIVATE_10_NET_PATTERN.matcher(text2).find()) {
            throw new IllegalStateException(text + " contains private 10.x address");
        }
        if (REAL_POINT_CODE_PATTERN.matcher(text2).find()) {
            throw new IllegalStateException(text + " contains real-looking point code");
        }
    }

    private static void assertSamplePointDefinitions(List<PointDefinition> list, String text) {
        if (list.isEmpty()) {
            throw new IllegalStateException(text + " must not be empty");
        }
        for (PointDefinition pointDefinition : list) {
            ShelfPointMonitorApp.assertSamplePointCode(text, pointDefinition.code());
        }
    }

    private static void assertSampleGroupPoints(List<GroupMonitorPoint> list, String text) {
        if (list.isEmpty()) {
            throw new IllegalStateException(text + " must not be empty");
        }
        for (GroupMonitorPoint groupMonitorPoint : list) {
            ShelfPointMonitorApp.assertSamplePointCode(text, groupMonitorPoint.code());
        }
    }

    private static void assertSamplePointCode(String text, String text2) {
        if (!text2.startsWith("USE_POINT_") && !text2.startsWith("BACKUP_POINT_")) {
            throw new IllegalStateException(text + " must use sample point codes, actual=" + text2);
        }
        ShelfPointMonitorApp.assertNotSensitive(text, text2);
    }

    private void buildUi() {
        this.navigationList.setSelectionMode(0);
        this.navigationList.setSelectedIndex(0);
        this.navigationList.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                this.cardLayout.show(this.cardPanel, this.navigationList.getSelectedValue());
                if (PAGE_LOGS.equals(this.navigationList.getSelectedValue())) {
                    this.loadSystemLogs();
                    this.refreshSystemHealthStatus();
                }
            }
        });
        this.cardPanel.add((Component)this.buildOverviewPage(), PAGE_OVERVIEW);
        this.cardPanel.add((Component)this.buildGroupManagementPage(), PAGE_GROUPS);
        this.cardPanel.add((Component)this.buildAlertCenterPage(), PAGE_ALERT_CENTER);
        this.cardPanel.add((Component)this.buildConnectionPage(), PAGE_CONNECTIONS);
        this.cardPanel.add((Component)this.buildDataQueryPage(), PAGE_DATA_QUERY);
        this.cardPanel.add((Component)this.buildBrowserPage(), PAGE_BROWSER);
        this.cardPanel.add((Component)this.buildLogsSystemPage(), PAGE_LOGS);
        this.cardPanel.add((Component)this.buildSystemSettingsPage(), PAGE_SETTINGS);
        this.setContentPane(new AppShell(
                this.navigationList,
                this.cardPanel,
                this.buildTopStatusBar(),
                this.buildBottomStatusBar()));
    }

    private JPanel buildTopStatusBar() {
        return new TopStatusBar(
                this.currentConnectionLabel,
                this.monitorStatusLabel,
                this.lastCheckLabel,
                this.nextCheckLabel);
    }

    private JPanel buildBottomStatusBar() {
        return new BottomStatusBar(this.bottomStatusLabel, EXPECTED_SELF_TEST_VERSION);
    }

    private JPanel buildOverviewPage() {
        return new OverviewPage(
                this.overviewGroupCountLabel,
                this.overviewAlertCountLabel,
                this.overviewPendingCountLabel,
                this.overviewDataErrorCountLabel,
                this.overviewTable,
                this.overviewDetailArea,
                this::updateOverviewDetail,
                this::startMonitoring,
                this::stopMonitoring,
                this::checkNow,
                () -> this.navigationList.setSelectedValue(PAGE_ALERT_CENTER, true),
                this::acknowledgeSelectedOverviewAlert);
    }

    private JPanel buildConnectionPage() {
        ConnectionManagementPage.Components components = new ConnectionManagementPage.Components(
                this.profileList,
                this.profileIdField,
                this.profileNameField,
                this.profileDbTypeBox,
                this.profileHostField,
                this.profilePortSpinner,
                this.profileDatabaseField,
                this.profileSchemaField,
                this.profileUserField,
                this.profileSslModeBox,
                this.profileLocalPathField,
                this.profilePasswordField,
                this.profileTestStatusLabel);
        ConnectionManagementPage.Actions actions = new ConnectionManagementPage.Actions(
                () -> {
                    int selectedIndex = this.profileList.getSelectedIndex();
                    if (selectedIndex >= 0 && selectedIndex < this.profiles.size()) {
                        this.populateProfileForm(this.profiles.get(selectedIndex));
                    }
                },
                this::updateProfileTypeEnabled,
                this::newProfileForm,
                this::saveProfile,
                this::deleteProfile,
                this::testSelectedProfile,
                this::useProfileWithoutTest);
        return new ConnectionManagementPage(components, actions);
    }

    private JPanel buildBrowserPage() {
        DataSourceBrowserPage.Components components = new DataSourceBrowserPage.Components(
                this.schemaCountLabel,
                this.objectCountLabel,
                this.objectTypeLabel,
                this.browserModeLabel,
                this.schemaBox,
                this.dataSourceTree,
                this.browserTable,
                this.columnTable,
                this.previewTable);
        DataSourceBrowserPage.Actions actions = new DataSourceBrowserPage.Actions(
                () -> this.runOnceInBackground(this::refreshSchemas),
                () -> this.runOnceInBackground(this::loadTablesForSelectedSchema),
                this::loadSelectedTableColumns,
                () -> this.runOnceInBackground(this::previewSelectedTable));
        return new DataSourceBrowserPage(components, actions);
    }

    private JPanel browserStatCard(JLabel label) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 225, 232)), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        panel.add((Component)label, "Center");
        return panel;
    }

    private JPanel buildDataQueryPage() {
        this.queryPrevButton.setEnabled(false);
        this.queryNextButton.setEnabled(false);
        DataQueryPage.Components components = new DataQueryPage.Components(
                this.queryPointKeywordField,
                this.queryShelfKeywordField,
                this.queryAreaCodeField,
                this.queryRelateAreaCodeField,
                this.queryUpdatedFromField,
                this.queryUpdatedToField,
                this.queryLimitSpinner,
                this.queryPrevButton,
                this.queryNextButton,
                this.queryPageLabel,
                this.queryTotalLabel,
                this.dataQueryTable,
                this.dataQueryDetailArea);
        DataQueryPage.Actions actions = new DataQueryPage.Actions(
                () -> this.startPointDataQuery(true),
                this::showPreviousPointDataQueryPage,
                this::showNextPointDataQueryPage,
                this::exportCurrentPointDataQueryResult,
                this::updateDataQueryDetail,
                this::resetPointDataQueryPage);
        return new DataQueryPage(components, actions);
    }

    private JPanel buildGroupManagementPage() {
        GroupManagementPage.Components components = new GroupManagementPage.Components(
                this.groupList,
                this.groupIdField,
                this.groupAreaField,
                this.groupNameField,
                this.groupMaterialField,
                this.groupEnabledBox,
                this.ruleEnabledBox,
                this.requireUseEmptyBox,
                this.backupThresholdParticipatesBox,
                this.minBackupAvailableSpinner,
                this.durationMinutesSpinner,
                this.groupCheckIntervalMinutesSpinner,
                this.groupPointTable,
                this.pointStatusPanel,
                this.groupSummaryLabel,
                this.groupRuntimeArea,
                this.startButton,
                this.stopButton,
                this.checkButton);
        GroupManagementPage.Actions actions = new GroupManagementPage.Actions(
                this::populateSelectedGroup,
                this::addPointGroup,
                this::removeSelectedGroup,
                () -> this.groupPointModel.addRow(new Object[]{
                        PointRole.BACKUP.name(), "\u5907\u7528\u4f4d", "", Boolean.TRUE}),
                this::removeSelectedGroupPointRows,
                this::saveGroupConfig,
                this::loadGroupConfig,
                this::validateGroupConfigFromUi,
                this::startMonitoring,
                this::stopMonitoring,
                this::checkNow);
        return new GroupManagementPage(components, actions);
    }

    private JPanel buildAlertCenterPage() {
        return new AlertCenterPage(
                this.alertCenterFilterBox,
                this.alertCenterTable,
                this.alertCenterDetailArea,
                this::updateAlertCenterDetail,
                this::refreshAlertCenterPage,
                this::refreshAlertCenterPage,
                this::acknowledgeSelectedAlertCenterGroup,
                this::showSelectedAlertCenterGroupInOverview,
                this::checkSelectedAlertCenterGroup,
                () -> this.navigationList.setSelectedValue(PAGE_CONNECTIONS, true));
    }

    private JPanel buildLogsSystemPage() {
        LogsSystemPage.Components components = new LogsSystemPage.Components(
                this.schedulerHealthLabel,
                this.connectionHealthLabel,
                this.detectionHealthLabel,
                this.configHealthLabel,
                this.logDirHealthLabel,
                this.selfTestHealthLabel,
                this.systemLogTypeFilterBox,
                this.systemLogFromField,
                this.systemLogToField,
                this.systemLogGroupField,
                this.systemLogKeywordField,
                this.systemLogTable,
                this.systemLogDetailArea,
                this.statusArea);
        LogsSystemPage.Actions actions = new LogsSystemPage.Actions(
                this::loadSystemLogs,
                this::requestSystemLogRefreshDebounced,
                () -> this.runIoInBackground(this::openLogs),
                () -> this.runIoInBackground(this::executeSelfTestFromUi),
                () -> this.runIoInBackground(this::exportDiagnostics),
                this::updateSystemLogDetail);
        return new LogsSystemPage(components, actions);
    }

    private JPanel buildSystemSettingsPage() {
        SystemSettingsPage.Components components = new SystemSettingsPage.Components(
                this.settingsDefaultPageBox,
                this.settingsOverviewRefreshSpinner,
                this.settingsAlertPopupBox,
                this.settingsAlertSoundBox,
                this.settingsLogRetentionSpinner,
                this.settingsDensityBox,
                this.settingsStartupSelfTestBox,
                this.settingsAutoCleanupLogsBox);
        SystemSettingsPage.Actions actions = new SystemSettingsPage.Actions(
                this::saveUiPreferences,
                this::restoreUiPreferences,
                this::reloadUiPreferences);
        return new SystemSettingsPage(components, actions);
    }

    private void addCheckBox(JPanel panel, int index, int index2, JCheckBox jCheckBox) {
        GridBagConstraints gridBagConstraints = this.gbc(index2, index);
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = 17;
        panel.add((Component)jCheckBox, gridBagConstraints);
    }

    private JPanel buildLegacyAlertPage() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("\u70b9\u4f4d\u7f3a\u6599\u62a5\u8b66"));
        JPanel panel2 = new JPanel(new GridBagLayout());
        this.addField(panel2, 0, 0, "\u5168\u5c40\u626b\u63cf(\u79d2)", this.intervalSpinner);
        JPanel panel3 = new JPanel(new FlowLayout(0, 8, 0));
        JButton button = new JButton("\u4fdd\u5b58\u70b9\u4f4d\u914d\u7f6e");
        panel3.add(button);
        panel3.add(this.startButton);
        panel3.add(this.stopButton);
        panel3.add(this.checkButton);
        GridBagConstraints gridBagConstraints = this.gbc(2, 0);
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        panel2.add((Component)panel3, gridBagConstraints);
        panel.add((Component)panel2, "North");
        this.pointTable.setRowHeight(26);
        this.pointTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        this.pointTable.getColumnModel().getColumn(1).setPreferredWidth(360);
        this.pointTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        panel.add((Component)new JScrollPane(this.pointTable), "Center");
        JPanel panel4 = new JPanel(new FlowLayout(0));
        JButton button2 = new JButton("\u6dfb\u52a0\u70b9\u4f4d");
        JButton button3 = new JButton("\u5220\u9664\u9009\u4e2d");
        panel4.add(button2);
        panel4.add(button3);
        panel.add((Component)panel4, "South");
        button2.addActionListener(actionEvent -> this.pointModel.addRow(new Object[]{"\u65b0\u70b9\u4f4d", "", 5}));
        button3.addActionListener(actionEvent -> this.removeSelectedPointRows());
        button.addActionListener(actionEvent -> this.savePointConfig());
        this.startButton.addActionListener(actionEvent -> this.startMonitoring());
        this.stopButton.addActionListener(actionEvent -> this.stopMonitoring());
        this.checkButton.addActionListener(actionEvent -> this.checkNow());
        this.stopButton.setEnabled(false);
        return panel;
    }

    private JPanel buildStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("\u8fd0\u884c\u65e5\u5fd7"));
        this.statusArea.setEditable(false);
        this.statusArea.setRows(8);
        this.statusArea.setFont(new Font("Monospaced", 0, 13));
        panel.add((Component)new JScrollPane(this.statusArea), "Center");
        JButton button = new JButton("\u6253\u5f00\u65e5\u5fd7\u76ee\u5f55");
        button.addActionListener(actionEvent -> this.runIoInBackground(this::openLogs));
        JPanel panel2 = new JPanel(new FlowLayout(2));
        panel2.add(button);
        panel.add((Component)panel2, "South");
        return panel;
    }

    private void addField(JPanel panel, int index, int index2, String text, Component component) {
        GridBagConstraints gridBagConstraints = this.gbc(index2, index);
        gridBagConstraints.anchor = 13;
        panel.add((Component)new JLabel(text + "\uff1a"), gridBagConstraints);
        GridBagConstraints gridBagConstraints2 = this.gbc(index2 + 1, index);
        gridBagConstraints2.fill = 2;
        gridBagConstraints2.weightx = 1.0;
        panel.add(component, gridBagConstraints2);
    }

    private GridBagConstraints gbc(int index, int index2) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = index;
        gridBagConstraints.gridy = index2;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        return gridBagConstraints;
    }

    private void loadProfiles() {
        ConnectionProfileStore.StoredProfiles storedProfiles = this.profileStore.load();
        this.profiles = new ArrayList<ConnectionProfile>(storedProfiles.profiles());
        this.refreshProfileList(storedProfiles.currentId());
    }

    private void refreshProfileList(String text) {
        this.profileListModel.clear();
        int index = 0;
        for (int i = 0; i < this.profiles.size(); ++i) {
            ConnectionProfile connectionProfile = this.profiles.get(i);
            this.profileListModel.addElement(connectionProfile.name() + " [" + connectionProfile.id() + "]");
            if (!connectionProfile.id().equals(text)) continue;
            index = i;
        }
        if (!this.profiles.isEmpty()) {
            this.profileList.setSelectedIndex(index);
        }
    }

    private void populateProfileForm(ConnectionProfile connectionProfile) {
        this.profileIdField.setText(connectionProfile.id());
        this.profileNameField.setText(connectionProfile.name());
        this.profileDbTypeBox.setSelectedItem(connectionProfile.dbType());
        this.profileHostField.setText(connectionProfile.host());
        this.profilePortSpinner.setValue(connectionProfile.port());
        this.profileDatabaseField.setText(connectionProfile.database());
        this.profileSchemaField.setText(connectionProfile.schema());
        this.profileUserField.setText(connectionProfile.user());
        this.profileSslModeBox.setSelectedItem(connectionProfile.sslMode());
        this.profileLocalPathField.setText(connectionProfile.localPath());
        this.updateProfileTypeEnabled();
    }

    private void newProfileForm() {
        this.profileIdField.setText("profile" + System.currentTimeMillis());
        this.profileNameField.setText("\u73b0\u573a\u6570\u636e\u5e93");
        this.profileDbTypeBox.setSelectedItem("postgres");
        this.profileHostField.setText("__SITE_HOST__");
        this.profilePortSpinner.setValue(2345);
        this.profileDatabaseField.setText("cms_web");
        this.profileSchemaField.setText("public");
        this.profileUserField.setText("__SITE_USER__");
        this.profileSslModeBox.setSelectedItem("disable");
        this.profileLocalPathField.setText("data/local-test-db");
        this.profilePasswordField.setText("");
        this.profileTestStatusLabel.setStatus("尚未执行连接测试", AppTheme.MUTED);
        this.profileList.clearSelection();
        this.updateProfileTypeEnabled();
    }

    private void updateProfileTypeEnabled() {
        boolean flag = "h2".equals(this.profileDbTypeBox.getSelectedItem());
        this.profileHostField.setEnabled(!flag);
        this.profilePortSpinner.setEnabled(!flag);
        this.profileDatabaseField.setEnabled(!flag);
        this.profileSchemaField.setEnabled(true);
        this.profileUserField.setEnabled(!flag);
        this.profileSslModeBox.setEnabled(!flag);
        this.profileLocalPathField.setEnabled(flag);
        if (flag) {
            this.profileHostField.setText("local");
            this.profilePortSpinner.setValue(1);
            this.profileDatabaseField.setText("local-test");
            this.profileSchemaField.setText("public");
            this.profileUserField.setText("sa");
            this.profileSslModeBox.setSelectedItem("disable");
        }
    }

    private void useProfileWithoutTest() {
        try {
            ConnectionProfile connectionProfile = this.readProfileForm();
            char[] password = this.profilePasswordField.getPassword();
            this.applyCurrentConnection(connectionProfile, password, "\u5df2\u9009\u62e9\u4f46\u672a\u6d4b\u8bd5");
            Arrays.fill(password, '\u0000');
            this.appendStatus("\u5df2\u8bbe\u4e3a\u5f53\u524d\u8fde\u63a5\uff1a" + connectionProfile.name() + "\u3002\u5efa\u8bae\u6267\u884c\u6d4b\u8bd5\u8fde\u63a5\u786e\u8ba4\u8d26\u53f7\u53ef\u7528\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private ConnectionProfile readProfileForm() {
        Object value = this.profileIdField.getText().trim();
        if (((String)value).isEmpty()) {
            value = "profile" + System.currentTimeMillis();
            this.profileIdField.setText((String)value);
        }
        return new ConnectionProfile((String)value, this.profileNameField.getText(), String.valueOf(this.profileDbTypeBox.getSelectedItem()), this.profileHostField.getText(), (Integer)this.profilePortSpinner.getValue(), this.profileDatabaseField.getText(), this.profileSchemaField.getText(), this.profileUserField.getText(), String.valueOf(this.profileSslModeBox.getSelectedItem()), this.profileLocalPathField.getText());
    }

    private void saveProfile() {
        try {
            this.invalidatePendingConnectionOperations();
            ConnectionProfile connectionProfile = this.readProfileForm();
            int index = this.findProfileIndex(connectionProfile.id());
            if (index >= 0) {
                this.profiles.set(index, connectionProfile);
            } else {
                this.profiles.add(connectionProfile);
            }
            this.profileStore.save(connectionProfile.id(), this.profiles);
            this.refreshProfileList(connectionProfile.id());
            this.appendStatus("\u8fde\u63a5\u914d\u7f6e\u5df2\u4fdd\u5b58\uff1a" + connectionProfile.name());
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void deleteProfile() {
        int index = this.profileList.getSelectedIndex();
        if (index < 0 || index >= this.profiles.size()) {
            this.showError(new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u8981\u5220\u9664\u7684\u8fde\u63a5"));
            return;
        }
        if (this.profiles.size() == 1) {
            this.showError(new IllegalArgumentException("\u81f3\u5c11\u4fdd\u7559\u4e00\u4e2a\u8fde\u63a5\u914d\u7f6e"));
            return;
        }
        this.invalidatePendingConnectionOperations();
        ConnectionProfile connectionProfile = this.profiles.remove(index);
        try {
            String text = this.profiles.get(0).id();
            this.profileStore.save(text, this.profiles);
            if (this.currentProfile != null && this.currentProfile.id().equals(connectionProfile.id())) {
                this.stopMonitoring();
                this.currentProfile = null;
                Arrays.fill(this.currentPassword, '\u0000');
                this.currentPassword = new char[0];
                this.currentConnectionHealth = "\u672a\u9009\u62e9\u8fde\u63a5";
                this.updateCurrentConnectionLabel();
            }
            this.refreshProfileList(text);
            this.appendStatus("\u8fde\u63a5\u914d\u7f6e\u5df2\u5220\u9664\uff1a" + connectionProfile.name());
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void testSelectedProfile() {
        try {
            ConnectionProfile connectionProfile = this.readProfileForm();
            char[] passwordChars = this.profilePasswordField.getPassword();
            char[] passwordSnapshot = Arrays.copyOf(passwordChars, passwordChars.length);
            Arrays.fill(passwordChars, '\u0000');
            long operationGeneration = this.beginConnectionTestOperation();
            this.profileTestStatusLabel.setStatus("正在测试连接", AppTheme.PRIMARY);
            this.runIoInBackground(() -> {
                try {
                    String testResult = this.pointRepository.testConnection(connectionProfile.toDbConfig(10), passwordSnapshot);
                    SwingUtilities.invokeLater(() -> this.applyTestConnectionSuccess(
                            operationGeneration,
                            connectionProfile,
                            passwordSnapshot,
                            testResult));
                }
                catch (Exception exception) {
                    String failureSummary = ShelfPointMonitorApp.userVisibleErrorMessage(exception);
                    SwingUtilities.invokeLater(() -> this.applyTestConnectionFailure(
                            operationGeneration,
                            passwordSnapshot,
                            failureSummary));
                }
            });
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private long beginConnectionTestOperation() {
        return ++this.connectionOperationGeneration;
    }

    private void invalidatePendingConnectionOperations() {
        ++this.connectionOperationGeneration;
    }

    private boolean isCurrentConnectionOperation(long operationGeneration) {
        return operationGeneration == this.connectionOperationGeneration;
    }

    private void applyTestConnectionSuccess(
            long operationGeneration,
            ConnectionProfile connectionProfile,
            char[] passwordSnapshot,
            String testResult) {
        if (!this.isCurrentConnectionOperation(operationGeneration)) {
            Arrays.fill(passwordSnapshot, '\u0000');
            return;
        }
        try {
            this.applyCurrentConnection(connectionProfile, passwordSnapshot, "\u6d4b\u8bd5\u6210\u529f\u5e76\u6b63\u5728\u4f7f\u7528");
            this.appendStatus("\u6d4b\u8bd5\u8fde\u63a5\u6210\u529f\u5e76\u8bbe\u4e3a\u5f53\u524d\u8fde\u63a5\uff1a"
                    + ShelfPointMonitorApp.sanitizeVisibleLog(testResult));
        }
        finally {
            Arrays.fill(passwordSnapshot, '\u0000');
        }
    }

    private void applyTestConnectionFailure(
            long operationGeneration,
            char[] passwordSnapshot,
            String failureSummary) {
        try {
            if (!this.isCurrentConnectionOperation(operationGeneration)) {
                return;
            }
            this.currentConnectionHealth = "\u4e0a\u6b21\u8fde\u63a5\u6d4b\u8bd5\u5931\u8d25";
            this.profileTestStatusLabel.setStatus("测试失败：" + failureSummary, AppTheme.QUERY_FAILED);
            this.refreshSystemHealthStatus();
            this.appendStatus("\u6d4b\u8bd5\u8fde\u63a5\u5931\u8d25\uff1a" + failureSummary);
            this.showErrorSummary("\u6d4b\u8bd5\u8fde\u63a5\u5931\u8d25\uff1a" + failureSummary);
        }
        finally {
            Arrays.fill(passwordSnapshot, '\u0000');
        }
    }

    private int findProfileIndex(String text) {
        for (int i = 0; i < this.profiles.size(); ++i) {
            if (!this.profiles.get(i).id().equals(text)) continue;
            return i;
        }
        return -1;
    }

    private void updateCurrentConnectionLabel() {
        if (this.currentProfile == null) {
            this.currentConnectionLabel.setText("\u5f53\u524d\u8fde\u63a5\uff1a\u672a\u8fde\u63a5");
            this.currentConnectionHealth = "\u672a\u9009\u62e9\u8fde\u63a5";
            this.refreshSystemHealthStatus();
            return;
        }
        this.currentConnectionLabel.setText("\u5f53\u524d\u8fde\u63a5\uff1a" + this.currentProfile.name() + " / " + this.currentProfile.dbType() + " / " + this.currentProfile.schema());
        this.refreshSystemHealthStatus();
    }

    private void applyCurrentConnection(ConnectionProfile connectionProfile, char[] password, String healthStatus) {
        this.invalidatePendingConnectionOperations();
        this.stopMonitoring();
        this.currentProfile = connectionProfile;
        Arrays.fill(this.currentPassword, '\u0000');
        this.currentPassword = password == null ? new char[0] : Arrays.copyOf(password, password.length);
        this.currentConnectionHealth = healthStatus;
        this.profileTestStatusLabel.setStatus(
                healthStatus,
                healthStatus.contains("\u6d4b\u8bd5\u6210\u529f") ? AppTheme.SUCCESS : AppTheme.WARNING);
        this.updateCurrentConnectionLabel();
    }

    private void refreshSchemas() throws Exception {
        DbConfig dbConfig = this.requireCurrentConfig(10);
        List<SchemaInfo> list = this.metadataRepository.listSchemas(dbConfig, this.currentPassword);
        SwingUtilities.invokeLater(() -> {
            this.schemaModel.removeAllElements();
            for (SchemaInfo schemaInfo : list) {
                this.schemaModel.addElement(schemaInfo.name());
            }
            this.refreshDataSourceTree(list, List.of());
            this.schemaCountLabel.setText("Schema \u6570\u91cf\uff1a" + list.size());
            this.selectSchema(this.currentProfile.schema());
            this.appendStatus("\u5df2\u5237\u65b0 Schema\uff1a" + list.size() + " \u4e2a");
        });
    }

    private void selectSchema(String text) {
        for (int i = 0; i < this.schemaModel.getSize(); ++i) {
            if (!this.schemaModel.getElementAt(i).equalsIgnoreCase(text)) continue;
            this.schemaBox.setSelectedIndex(i);
            return;
        }
        if (this.schemaModel.getSize() > 0) {
            this.schemaBox.setSelectedIndex(0);
        }
    }

    private void refreshDataSourceTree(List<SchemaInfo> list, List<TableInfo> list2) {
        this.dataSourceTreeRoot.removeAllChildren();
        if (!list.isEmpty()) {
            for (SchemaInfo value : list) {
                this.dataSourceTreeRoot.add(new DefaultMutableTreeNode(value.name()));
            }
        } else {
            LinkedHashMap<String, DefaultMutableTreeNode> linkedHashMap = new LinkedHashMap<>();
            for (TableInfo tableInfo : list2) {
                DefaultMutableTreeNode defaultMutableTreeNode = linkedHashMap.computeIfAbsent(tableInfo.schema(), schema -> {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(schema);
                    this.dataSourceTreeRoot.add(node);
                    return node;
                });
                defaultMutableTreeNode.add(new DefaultMutableTreeNode(tableInfo.name() + " [" + tableInfo.type() + "]"));
            }
        }
        ((DefaultTreeModel)this.dataSourceTree.getModel()).reload();
        for (int i = 0; i < this.dataSourceTree.getRowCount(); ++i) {
            this.dataSourceTree.expandRow(i);
        }
    }

    private void loadTablesForSelectedSchema() throws Exception {
        DbConfig dbConfig = this.requireCurrentConfig(10);
        String text = this.requireSelectedSchema();
        List<TableInfo> list = this.metadataRepository.listTables(dbConfig, this.currentPassword, text);
        SwingUtilities.invokeLater(() -> {
            this.browserTableModel.setRowCount(0);
            this.columnModel.setRowCount(0);
            this.previewModel.setRowCount(0);
            this.previewModel.setColumnCount(0);
            for (TableInfo tableInfo : list) {
                this.browserTableModel.addRow(new Object[]{tableInfo.schema(), tableInfo.name(), tableInfo.type()});
            }
            this.refreshDataSourceTree(List.of(), list);
            this.objectCountLabel.setText("\u8868 / \u89c6\u56fe\u6570\u91cf\uff1a" + list.size());
            this.objectTypeLabel.setText("\u5f53\u524d\u5bf9\u8c61\u7c7b\u578b\uff1a--");
            this.appendStatus("\u5df2\u52a0\u8f7d " + text + " \u4e0b\u8868/\u89c6\u56fe\uff1a" + list.size() + " \u4e2a");
        });
    }

    private void loadSelectedTableColumns() {
        int index = this.browserTable.getSelectedRow();
        if (index < 0) {
            return;
        }
        String text = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(index), 0));
        String text2 = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(index), 1));
        this.runOnceInBackground(() -> {
            DbConfig dbConfig = this.requireCurrentConfig(10);
            List<ColumnInfo> list = this.metadataRepository.listColumns(dbConfig, this.currentPassword, text, text2);
            SwingUtilities.invokeLater(() -> {
                this.columnModel.setRowCount(0);
                for (ColumnInfo columnInfo : list) {
                    this.columnModel.addRow(new Object[]{columnInfo.name(), columnInfo.typeName(), columnInfo.size(), columnInfo.nullable() ? "\u662f" : "\u5426", this.blankToDash(columnInfo.defaultValue()), this.blankToDash(columnInfo.remarks())});
                }
                this.objectTypeLabel.setText("\u5f53\u524d\u5bf9\u8c61\u7c7b\u578b\uff1a" + String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(index), 2)));
                this.appendStatus("\u5df2\u52a0\u8f7d\u5b57\u6bb5\uff1a" + text + "." + text2 + " / " + list.size() + " \u4e2a");
            });
        });
    }

    private void previewSelectedTable() throws Exception {
        int index = this.browserTable.getSelectedRow();
        if (index < 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u8981\u9884\u89c8\u7684\u8868\u6216\u89c6\u56fe");
        }
        String text = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(index), 0));
        String text2 = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(index), 1));
        DbConfig dbConfig = this.requireCurrentConfig(10);
        TablePreview tablePreview = this.metadataRepository.previewTable(dbConfig, this.currentPassword, text, text2, 100);
        SwingUtilities.invokeLater(() -> {
            this.previewModel.setColumnIdentifiers(tablePreview.columnNames().toArray());
            this.previewModel.setRowCount(0);
            for (List<String> list : tablePreview.rows()) {
                this.previewModel.addRow(list.toArray());
            }
            this.objectTypeLabel.setText("\u5f53\u524d\u5bf9\u8c61\u7c7b\u578b\uff1a\u9884\u89c8 " + tablePreview.rows().size() + " \u884c");
            this.appendStatus("\u5df2\u9884\u89c8\uff1a" + text + "." + text2 + " / " + tablePreview.rows().size() + " \u884c");
        });
    }

    private String requireSelectedSchema() {
        Object value = this.schemaBox.getSelectedItem();
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException("\u8bf7\u5148\u5237\u65b0\u5e76\u9009\u62e9 Schema");
        }
        return String.valueOf(value);
    }

    private void loadGroupConfig() {
        this.pointGroups = new ArrayList<PointGroupDefinition>(this.groupConfigStore.load());
        this.refreshGroupList(this.pointGroups.isEmpty() ? "" : this.pointGroups.get(0).id());
        this.refreshOverviewPage();
        this.refreshAlertCenterPage();
    }

    private void refreshGroupList(String text) {
        this.groupListModel.clear();
        int index = -1;
        for (int i = 0; i < this.pointGroups.size(); ++i) {
            PointGroupDefinition pointGroupDefinition = this.pointGroups.get(i);
            this.groupListModel.addElement(pointGroupDefinition.areaName() + " / " + pointGroupDefinition.groupName() + " [" + pointGroupDefinition.id() + "]");
            if (!pointGroupDefinition.id().equals(text)) continue;
            index = i;
        }
        if (index < 0 && !this.pointGroups.isEmpty()) {
            index = 0;
        }
        if (index >= 0) {
            this.groupList.setSelectedIndex(index);
        }
    }

    private void populateSelectedGroup() {
        int index = this.groupList.getSelectedIndex();
        if (index < 0 || index >= this.pointGroups.size()) {
            return;
        }
        PointGroupDefinition pointGroupDefinition = this.pointGroups.get(index);
        this.groupIdField.setText(pointGroupDefinition.id());
        this.groupAreaField.setText(pointGroupDefinition.areaName());
        this.groupNameField.setText(pointGroupDefinition.groupName());
        this.groupMaterialField.setText(pointGroupDefinition.materialName());
        this.groupEnabledBox.setSelected(pointGroupDefinition.enabled());
        this.ruleEnabledBox.setSelected(pointGroupDefinition.rule().enabled());
        this.requireUseEmptyBox.setSelected(pointGroupDefinition.rule().requireUsePointEmpty());
        this.minBackupAvailableSpinner.setValue(pointGroupDefinition.rule().minBackupAvailable());
        this.durationMinutesSpinner.setValue(pointGroupDefinition.rule().durationMinutes());
        this.backupThresholdParticipatesBox.setSelected(pointGroupDefinition.rule().backupThresholdParticipates());
        this.groupCheckIntervalMinutesSpinner.setValue(Math.max(1, (pointGroupDefinition.checkIntervalSeconds() + 59) / 60));
        this.groupPointModel.setRowCount(0);
        for (GroupMonitorPoint groupMonitorPoint : pointGroupDefinition.points()) {
            this.groupPointModel.addRow(new Object[]{groupMonitorPoint.role().name(), groupMonitorPoint.alias(), groupMonitorPoint.code(), groupMonitorPoint.enabled()});
        }
        this.groupSummaryLabel.setText("\u5f53\u524d\u5224\u65ad\uff1a\u672a\u68c0\u6d4b");
        this.pointStatusPanel.removeAll();
        this.pointStatusPanel.revalidate();
        this.pointStatusPanel.repaint();
    }

    private void addPointGroup() {
        try {
            if (!this.pointGroups.isEmpty() && this.groupList.getSelectedIndex() >= 0) {
                this.updateSelectedGroupFromForm();
            }
            String text = "group-" + System.currentTimeMillis();
            this.pointGroups.add(new PointGroupDefinition(text, "\u533a\u57df", "\u7269\u6599\u7ec4", "\u7269\u6599", true, 60, List.of(new GroupMonitorPoint(text + "-use", "USE_POINT_001", "\u4f7f\u7528\u4f4d", PointRole.USE, true, 1), new GroupMonitorPoint(text + "-backup-1", "BACKUP_POINT_001", "\u5907\u7528\u4f4d1", PointRole.BACKUP, true, 2), new GroupMonitorPoint(text + "-backup-2", "BACKUP_POINT_002", "\u5907\u7528\u4f4d2", PointRole.BACKUP, true, 3), new GroupMonitorPoint(text + "-backup-3", "BACKUP_POINT_003", "\u5907\u7528\u4f4d3", PointRole.BACKUP, true, 4), new GroupMonitorPoint(text + "-backup-4", "BACKUP_POINT_004", "\u5907\u7528\u4f4d4", PointRole.BACKUP, true, 5)), new GroupAlertRule(true, true, 3, 5, true)));
            this.refreshGroupList(text);
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void removeSelectedGroup() {
        int index = this.groupList.getSelectedIndex();
        if (index < 0 || index >= this.pointGroups.size()) {
            this.showError(new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u70b9\u4f4d\u7ec4"));
            return;
        }
        PointGroupDefinition pointGroupDefinition = this.pointGroups.remove(index);
        Object value = this.groupMonitorLock;
        synchronized (value) {
            this.groupStates.remove(pointGroupDefinition.id());
            this.lastGroupStatuses.remove(pointGroupDefinition.id());
        }
        this.refreshGroupList(this.pointGroups.isEmpty() ? "" : this.pointGroups.get(0).id());
        if (this.pointGroups.isEmpty()) {
            this.groupPointModel.setRowCount(0);
        }
    }

    private void removeSelectedGroupPointRows() {
        int[] nArray = this.groupPointTable.getSelectedRows();
        for (int i = nArray.length - 1; i >= 0; --i) {
            this.groupPointModel.removeRow(this.groupPointTable.convertRowIndexToModel(nArray[i]));
        }
    }

    private List<PointGroupDefinition> readGroups() {
        if (!this.pointGroups.isEmpty() && this.groupList.getSelectedIndex() >= 0) {
            this.updateSelectedGroupFromForm();
        }
        if (this.pointGroups.isEmpty()) {
            throw new IllegalArgumentException("\u81f3\u5c11\u914d\u7f6e\u4e00\u4e2a\u70b9\u4f4d\u7ec4");
        }
        GroupConfigStore.validateGroups(this.pointGroups);
        return new ArrayList<PointGroupDefinition>(this.pointGroups);
    }

    List<PointGroupDefinition> captureMonitoredGroupsForTest() {
        return this.captureMonitoredGroups(this.readGroups());
    }

    List<PointGroupDefinition> monitoredGroupsSnapshotForTest() {
        return this.monitoredGroups;
    }

    private List<PointGroupDefinition> captureMonitoredGroups(List<PointGroupDefinition> list) {
        this.monitoredGroups = List.copyOf(list);
        return this.monitoredGroups;
    }

    private void updateSelectedGroupFromForm() {
        int index = this.groupList.getSelectedIndex();
        if (index < 0 || index >= this.pointGroups.size()) {
            return;
        }
        this.stopGroupTableEditing();
        PointGroupDefinition pointGroupDefinition = new PointGroupDefinition(this.groupIdField.getText(), this.groupAreaField.getText(), this.groupNameField.getText(), this.groupMaterialField.getText(), this.groupEnabledBox.isSelected(), (Integer)this.groupCheckIntervalMinutesSpinner.getValue() * 60, this.readGroupPoints(), new GroupAlertRule(this.ruleEnabledBox.isSelected(), this.requireUseEmptyBox.isSelected(), (Integer)this.minBackupAvailableSpinner.getValue(), (Integer)this.durationMinutesSpinner.getValue(), this.backupThresholdParticipatesBox.isSelected()));
        this.pointGroups.set(index, pointGroupDefinition);
    }

    private List<GroupMonitorPoint> readGroupPoints() {
        ArrayList<GroupMonitorPoint> arrayList = new ArrayList<GroupMonitorPoint>();
        for (int i = 0; i < this.groupPointModel.getRowCount(); ++i) {
            String text = this.groupCellText(i, 0);
            String text2 = this.groupCellText(i, 1);
            String text3 = this.groupCellText(i, 2);
            boolean flag = this.groupCellBoolean(i, 3);
            if (text.isEmpty() && text2.isEmpty() && text3.isEmpty()) continue;
            if (text.isEmpty() || text2.isEmpty() || text3.isEmpty()) {
                throw new IllegalArgumentException("\u70b9\u4f4d\u89d2\u8272\u3001\u522b\u540d\u3001\u7f16\u7801\u5fc5\u987b\u540c\u65f6\u586b\u5199");
            }
            arrayList.add(new GroupMonitorPoint(this.groupIdField.getText().trim() + "-point-" + (i + 1), text3, text2, PointRole.valueOf(text), flag, i + 1));
        }
        return arrayList;
    }

    private void stopGroupTableEditing() {
        if (this.groupPointTable.isEditing() && this.groupPointTable.getCellEditor() != null) {
            this.groupPointTable.getCellEditor().stopCellEditing();
        }
    }

    private String groupCellText(int index, int index2) {
        Object value = this.groupPointModel.getValueAt(index, index2);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private boolean groupCellBoolean(int index, int index2) {
        Object value = this.groupPointModel.getValueAt(index, index2);
        return value instanceof Boolean ? (Boolean)value : Boolean.parseBoolean(String.valueOf(value));
    }

    private void saveGroupConfig() {
        try {
            List<PointGroupDefinition> list = this.readGroups();
            this.groupConfigStore.save(list);
            this.pointGroups = new ArrayList<PointGroupDefinition>(list);
            this.refreshGroupList(this.pointGroups.get(0).id());
            this.refreshOverviewPage();
            this.refreshAlertCenterPage();
            this.appendStatus("\u70b9\u4f4d\u7ec4\u914d\u7f6e\u5df2\u4fdd\u5b58\u3002\u6570\u636e\u5e93\u5bc6\u7801\u672a\u4fdd\u5b58\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void validateGroupConfigFromUi() {
        try {
            List<PointGroupDefinition> list = this.readGroups();
            GroupConfigStore.validateGroups(list);
            this.appendStatus("\u70b9\u4f4d\u7ec4\u914d\u7f6e\u9a8c\u8bc1\u901a\u8fc7\uff0c\u5171 " + list.size() + " \u4e2a\u70b9\u4f4d\u7ec4\u3002");
            JOptionPane.showMessageDialog(this, "\u914d\u7f6e\u9a8c\u8bc1\u901a\u8fc7\u3002", "\u9a8c\u8bc1\u914d\u7f6e", 1);
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void checkNow() {
        try {
            DbConfig dbConfig = this.requireCurrentConfig(60);
            List<PointGroupDefinition> groups = List.copyOf(this.readGroups());
            this.groupConfigStore.save(groups);
            MonitoringSession session = this.createOneShotSession(dbConfig);
            this.registerOneShotSession(session);
            this.runMonitorInBackground(() -> {
                try {
                    this.checkGroups(session, groups, LocalDateTime.now(), "\u624b\u52a8\u68c0\u6d4b",
                            () -> this.isCurrentOneShotSession(session));
                }
                finally {
                    session.clearPassword();
                    this.unregisterOneShotSession(session);
                }
            }, () -> this.isCurrentOneShotSession(session));
        }
        catch (Exception exception) {
            this.showError(exception);
            this.appendStatus("\u6267\u884c\u5931\u8d25\uff1a" + ShelfPointMonitorApp.userVisibleErrorMessage(exception));
        }
    }

    private void checkDueGroups() throws Exception {
        MonitoringSession session = this.monitoringSession;
        if (session == null) {
            return;
        }
        this.checkDueGroups(session);
    }

    private void checkDueGroups(MonitoringSession session) throws Exception {
        List<PointGroupDefinition> dueGroups;
        if (!this.isCurrentMonitoringSession(session)) {
            return;
        }
        List<PointGroupDefinition> monitoredSnapshot = this.monitoredGroups;
        LocalDateTime checkedAt = LocalDateTime.now();
        synchronized (this.groupMonitorLock) {
            if (!this.isCurrentMonitoringSession(session)) {
                return;
            }
            dueGroups = GroupCheckPlanner.dueGroups(monitoredSnapshot, this.groupStates, checkedAt);
        }
        if (dueGroups.isEmpty() || !this.isCurrentMonitoringSession(session)) {
            return;
        }
        this.checkGroups(session, dueGroups, checkedAt, "\u81ea\u52a8\u68c0\u6d4b",
                () -> this.isCurrentMonitoringSession(session));
    }

    private void checkGroups(
            MonitoringSession session,
            List<PointGroupDefinition> groups,
            LocalDateTime checkedAt,
            String source,
            BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        char[] taskPassword = session.copyPasswordForTask();
        try {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            this.checkGroupsWithFetcher(
                    groups,
                    checkedAt,
                    source,
                    pointGroupDefinition -> this.pointRepository.fetch(
                            session.config(), taskPassword, this.pointDefinitions(pointGroupDefinition)),
                    publicationAllowed);
        }
        finally {
            MonitoringSession.clearTaskPassword(taskPassword);
        }
    }

    GroupCheckRunResult checkGroupsWithFetcher(
            MonitoringSession session,
            List<PointGroupDefinition> groups,
            LocalDateTime checkedAt,
            String source,
            GroupPointFetcher groupPointFetcher) {
        return this.checkGroupsWithFetcher(
                groups,
                checkedAt,
                source,
                groupPointFetcher,
                () -> this.isCurrentMonitoringSession(session));
    }

    GroupCheckRunResult checkGroupsWithFetcher(
            List<PointGroupDefinition> groups,
            LocalDateTime checkedAt,
            String source,
            GroupPointFetcher groupPointFetcher) {
        return this.checkGroupsWithFetcher(groups, checkedAt, source, groupPointFetcher, () -> true);
    }

    private GroupCheckRunResult checkGroupsWithFetcher(
            List<PointGroupDefinition> groups,
            LocalDateTime checkedAt,
            String source,
            GroupPointFetcher groupPointFetcher,
            BooleanSupplier publicationAllowed) {
        StringBuilder runSummary = new StringBuilder();
        boolean dialogRequested = false;
        int checkedGroupCount = 0;
        int failedGroupCount = 0;
        ArrayList<GroupEvaluation> evaluations = new ArrayList<GroupEvaluation>();
        for (PointGroupDefinition pointGroupDefinition : groups) {
            if (!publicationAllowed.getAsBoolean()) {
                return this.cancelledGroupCheckRun();
            }
            GroupEvaluation groupEvaluation;
            GroupRuntimeState groupRuntimeState;
            synchronized (this.groupMonitorLock) {
                if (!publicationAllowed.getAsBoolean()) {
                    return this.cancelledGroupCheckRun();
                }
                groupRuntimeState = this.groupStates.computeIfAbsent(
                        pointGroupDefinition.id(), groupId -> new GroupRuntimeState());
                groupRuntimeState.markChecked(checkedAt);
            }
            List<PointRecord> pointRecords;
            try {
                pointRecords = groupPointFetcher.fetch(pointGroupDefinition);
            }
            catch (Exception exception) {
                if (!publicationAllowed.getAsBoolean()) {
                    return this.cancelledGroupCheckRun();
                }
                String failureSummary = ShelfPointMonitorApp.queryFailureMessage(exception);
                synchronized (this.groupMonitorLock) {
                    if (!publicationAllowed.getAsBoolean()) {
                        return this.cancelledGroupCheckRun();
                    }
                    groupEvaluation = GroupMonitorLogic.queryFailed(
                            pointGroupDefinition, groupRuntimeState, checkedAt, failureSummary);
                }
                failedGroupCount++;
                this.appendCheckLog(checkedAt, groupEvaluation, publicationAllowed);
                this.appendGroupEvents(checkedAt, groupEvaluation, publicationAllowed);
                evaluations.add(groupEvaluation);
                this.recordLatestEvaluation(groupEvaluation, publicationAllowed);
                this.closeActiveGroupAlertDialogIfOwnedBy(pointGroupDefinition.id(), publicationAllowed);
                this.updateSelectedGroupBoard(groupEvaluation, publicationAllowed);
                runSummary.append(this.formatGroupCheckResult(source, List.of(), groupEvaluation))
                        .append(System.lineSeparator());
                this.appendStatus(source + "\u5931\u8d25\uff0c\u70b9\u4f4d\u7ec4 " + pointGroupDefinition.id() + " " + failureSummary,
                        publicationAllowed);
                continue;
            }
            if (!publicationAllowed.getAsBoolean()) {
                return this.cancelledGroupCheckRun();
            }
            synchronized (this.groupMonitorLock) {
                if (!publicationAllowed.getAsBoolean()) {
                    return this.cancelledGroupCheckRun();
                }
                groupEvaluation = GroupMonitorLogic.evaluate(pointGroupDefinition, pointRecords, groupRuntimeState, checkedAt);
            }
            this.appendCheckLog(checkedAt, groupEvaluation, publicationAllowed);
            this.appendGroupEvents(checkedAt, groupEvaluation, publicationAllowed);
            evaluations.add(groupEvaluation);
            this.recordLatestEvaluation(groupEvaluation, publicationAllowed);
            this.updateSelectedGroupBoard(groupEvaluation, publicationAllowed);
            runSummary.append(this.formatGroupCheckResult(source, pointRecords, groupEvaluation))
                    .append(System.lineSeparator());
            checkedGroupCount++;
            if (!groupEvaluation.shouldShowDialog() || dialogRequested) continue;
            if (this.uiPreferences.alertPopupEnabled()) {
                this.showGroupAlertDialog(groupEvaluation, publicationAllowed);
                dialogRequested = true;
                continue;
            }
            this.appendStatus("\u62a5\u8b66\u5f39\u7a97\u5df2\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5173\u95ed\uff0c\u672c\u6b21\u53ea\u8bb0\u5f55\u72b6\u6001\u548c\u4e8b\u4ef6\uff1a" + groupEvaluation.groupId(),
                    publicationAllowed);
        }
        if (!publicationAllowed.getAsBoolean()) {
            return this.cancelledGroupCheckRun();
        }
        String summary = runSummary.toString();
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            this.groupRuntimeArea.setText(summary);
            this.lastCheckLabel.setText("\u4e0a\u6b21\u68c0\u6d4b\uff1a" + TIME_FORMAT.format(checkedAt));
            this.nextCheckLabel.setText(this.scheduledTask == null
                    ? "\u4e0b\u6b21\u68c0\u6d4b\uff1a--"
                    : "\u4e0b\u6b21\u68c0\u6d4b\uff1a" + TIME_FORMAT.format(checkedAt.plusSeconds(10L)));
            this.refreshOverviewPage();
            this.refreshAlertCenterPage();
        });
        this.appendStatus(source + "\u5b8c\u6210\uff0c\u70b9\u4f4d\u7ec4 " + checkedGroupCount + " \u4e2a"
                + (failedGroupCount > 0 ? "\uff0c\u5931\u8d25 " + failedGroupCount + " \u4e2a" : "") + "\u3002", publicationAllowed);
        synchronized (this.groupMonitorLock) {
            if (!publicationAllowed.getAsBoolean()) {
                return this.cancelledGroupCheckRun();
            }
            this.latestDetectionHealth = failedGroupCount > 0 ? "\u67e5\u8be2\u5931\u8d25" : "\u6210\u529f";
        }
        this.refreshSystemHealthStatus(publicationAllowed);
        return new GroupCheckRunResult(checkedGroupCount, failedGroupCount, dialogRequested, evaluations);
    }

    private GroupCheckRunResult cancelledGroupCheckRun() {
        return new GroupCheckRunResult(0, 0, false, List.of());
    }

    private void recordLatestEvaluation(GroupEvaluation groupEvaluation, BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        synchronized (this.groupMonitorLock) {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            this.lastGroupEvaluations.put(groupEvaluation.groupId(), groupEvaluation);
        }
    }

    private void updateSelectedGroupBoard(GroupEvaluation groupEvaluation, BooleanSupplier publicationAllowed) {
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            if (!groupEvaluation.groupId().equals(this.selectedGroupId())) {
                return;
            }
            String text = groupEvaluation.message();
            if (text == null || text.isBlank()) {
                text = GroupStatusText.statusText(groupEvaluation.status());
            }
            this.groupSummaryLabel.setText("\u5f53\u524d\u5224\u65ad\uff1a" + ShelfPointMonitorApp.sanitizeVisibleLog(text));
            this.renderPointStatusBoard(groupEvaluation);
        });
    }

    private String selectedGroupId() {
        int index = this.groupList.getSelectedIndex();
        if (index < 0 || index >= this.pointGroups.size()) {
            return "";
        }
        return this.pointGroups.get(index).id();
    }

    private List<PointDefinition> pointDefinitions(PointGroupDefinition pointGroupDefinition) {
        ArrayList<PointDefinition> arrayList = new ArrayList<PointDefinition>();
        for (GroupMonitorPoint groupMonitorPoint : pointGroupDefinition.points()) {
            if (!groupMonitorPoint.enabled()) continue;
            arrayList.add(new PointDefinition(groupMonitorPoint.code(), groupMonitorPoint.alias()));
        }
        return arrayList;
    }

    private void appendCheckLog(
            LocalDateTime checkedAt,
            GroupEvaluation groupEvaluation,
            BooleanSupplier publicationAllowed) {
        this.enqueueIoOperation(publicationAllowed, "CSV\u68c0\u6d4b\u65e5\u5fd7\u5199\u5165\u5931\u8d25",
                () -> this.groupLogWriter.appendCheck(checkedAt, groupEvaluation));
    }

    private void appendGroupEvents(
            LocalDateTime checkedAt,
            GroupEvaluation groupEvaluation,
            BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        GroupAlertStatus groupAlertStatus;
        synchronized (this.groupMonitorLock) {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            groupAlertStatus = this.lastGroupStatuses.getOrDefault(groupEvaluation.groupId(), GroupAlertStatus.NORMAL);
            this.lastGroupStatuses.put(groupEvaluation.groupId(), groupEvaluation.status());
        }
        if (groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED) {
            if (groupAlertStatus != GroupAlertStatus.QUERY_FAILED) {
                this.enqueueIoOperation(publicationAllowed, "CSV\u4e8b\u4ef6\u65e5\u5fd7\u5199\u5165\u5931\u8d25",
                        () -> this.groupLogWriter.appendEvent(checkedAt, "QUERY_FAILED", groupEvaluation));
            }
            return;
        }
        if (groupAlertStatus == GroupAlertStatus.QUERY_FAILED) {
            this.enqueueIoOperation(publicationAllowed, "CSV\u4e8b\u4ef6\u65e5\u5fd7\u5199\u5165\u5931\u8d25",
                    () -> this.groupLogWriter.appendEvent(checkedAt, "QUERY_RECOVERED", groupEvaluation));
        }
        if (groupEvaluation.status() == GroupAlertStatus.ACTIVE_ALERT
                && groupAlertStatus != GroupAlertStatus.ACTIVE_ALERT
                && groupAlertStatus != GroupAlertStatus.ACKED_ALERT) {
            this.enqueueIoOperation(publicationAllowed, "CSV\u4e8b\u4ef6\u65e5\u5fd7\u5199\u5165\u5931\u8d25",
                    () -> this.groupLogWriter.appendEvent(checkedAt, "ALERT_OPEN", groupEvaluation));
        } else if (groupEvaluation.status() == GroupAlertStatus.NORMAL
                && groupAlertStatus != GroupAlertStatus.NORMAL
                && groupAlertStatus != GroupAlertStatus.QUERY_FAILED) {
            this.enqueueIoOperation(publicationAllowed, "CSV\u4e8b\u4ef6\u65e5\u5fd7\u5199\u5165\u5931\u8d25",
                    () -> this.groupLogWriter.appendEvent(checkedAt, "RECOVERED", groupEvaluation));
        }
    }

    private void renderPointStatusBoard(GroupEvaluation groupEvaluation) {
        this.pointStatusPanel.removeAll();
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(6, 6, 6, 6);
        gridBagConstraints.fill = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        if (groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED) {
            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
            panel.setBackground(new Color(255, 250, 230));
            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(190, 120, 30), 2), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            JLabel label = new JLabel("\u67e5\u8be2\u5931\u8d25");
            label.setFont(new Font("SansSerif", 1, 22));
            label.setForeground(new Color(160, 92, 20));
            panel.add(label);
            panel.add(new JLabel(ShelfPointMonitorApp.sanitizeVisibleLog(groupEvaluation.message())));
            panel.add(new JLabel("\u672c\u6b21\u672a\u83b7\u5f97\u70b9\u4f4d\u72b6\u6001\uff0c\u4e0d\u6309\u65e0\u6599\u5904\u7406\u3002"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 4;
            this.pointStatusPanel.add((Component)panel, gridBagConstraints);
            this.pointStatusPanel.revalidate();
            this.pointStatusPanel.repaint();
            return;
        }
        ArrayList<PointStatusView> arrayList = new ArrayList<PointStatusView>();
        ArrayList<PointStatusView> arrayList2 = new ArrayList<PointStatusView>();
        for (PointStatusView value : groupEvaluation.pointStatuses()) {
            if (value.role() == PointRole.USE) {
                arrayList.add(value);
                continue;
            }
            arrayList2.add(value);
        }
        int index = 0;
        for (Object value : arrayList) {
            Object value2 = this.pointStatusCard((PointStatusView)value);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = index++;
            gridBagConstraints.gridwidth = 4;
            this.pointStatusPanel.add((Component)value2, gridBagConstraints);
        }
        gridBagConstraints.gridwidth = 1;
        boolean flag = false;
        int index2 = 0;
        for (Object value2 : arrayList2) {
            JPanel panel = this.pointStatusCard((PointStatusView)value2);
            gridBagConstraints.gridx = index2++;
            gridBagConstraints.gridy = index++;
            this.pointStatusPanel.add((Component)panel, gridBagConstraints);
            if (index2 < 4) continue;
            index2 = 0;
        }
        this.pointStatusPanel.revalidate();
        this.pointStatusPanel.repaint();
    }

    private JPanel pointStatusCard(PointStatusView pointStatusView) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
        Color color = this.statusColor(pointStatusView.status());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel label = new JLabel((pointStatusView.role() == PointRole.USE ? "\u4f7f\u7528\u4f4d\uff1a" : "\u5907\u7528\u4f4d\uff1a") + pointStatusView.alias());
        label.setFont(new Font("SansSerif", 1, pointStatusView.role() == PointRole.USE ? 16 : 14));
        JLabel label2 = new JLabel(pointStatusView.statusText());
        label2.setFont(new Font("SansSerif", 1, pointStatusView.role() == PointRole.USE ? 26 : 22));
        label2.setForeground(color);
        String text = pointStatusView.shelfCode() == null || pointStatusView.shelfCode().isBlank() ? "--" : pointStatusView.shelfCode();
        String text2 = pointStatusView.reason() == null || pointStatusView.reason().isBlank() ? "--" : pointStatusView.reason();
        panel.add(label);
        panel.add(label2);
        panel.add(new JLabel("\u70b9\u4f4d\uff1a" + pointStatusView.pointCode()));
        panel.add(new JLabel("\u8d27\u67b6\uff1a" + text));
        panel.add(new JLabel("\u539f\u56e0\uff1a" + text2));
        return panel;
    }

    private Color statusColor(PointMaterialStatus pointMaterialStatus) {
        if (pointMaterialStatus == PointMaterialStatus.AVAILABLE) {
            return new Color(24, 128, 72);
        }
        if (pointMaterialStatus == PointMaterialStatus.EMPTY) {
            return new Color(190, 48, 48);
        }
        if (pointMaterialStatus == PointMaterialStatus.MISSING) {
            return new Color(112, 112, 112);
        }
        return new Color(150, 150, 150);
    }

    private String formatGroupCheckResult(String text, List<PointRecord> list, GroupEvaluation groupEvaluation) {
        String text2 = groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED && groupEvaluation.message() != null && !groupEvaluation.message().isBlank() ? groupEvaluation.message() : GroupStatusText.summary(groupEvaluation.areaName(), groupEvaluation.groupName(), groupEvaluation.materialName(), groupEvaluation.status(), groupEvaluation.usePointEmpty(), groupEvaluation.backupTotal(), groupEvaluation.backupAvailableCount(), groupEvaluation.continuousMatchedSeconds(), groupEvaluation.alertDurationSeconds(), groupEvaluation.pointStatuses());
        return TIME_FORMAT.format(LocalDateTime.now()) + " " + GroupStatusText.statusText(groupEvaluation.status())
                + "\uff1a" + ShelfPointMonitorApp.sanitizeVisibleLog(text2);
    }

    static String queryFailureMessage(Throwable exception) {
        return "\u67e5\u8be2\u5931\u8d25\uff1a" + ShelfPointMonitorApp.sanitizedExceptionSummary(exception);
    }

    static String userVisibleErrorMessage(Throwable exception) {
        return ShelfPointMonitorApp.sanitizedExceptionSummary(exception);
    }

    static String sanitizedExceptionSummary(Throwable exception) {
        String raw = exception == null ? "" : exception.getMessage();
        String summary;
        if (raw == null || raw.isBlank()) {
            summary = exception == null ? "\u672a\u77e5\u9519\u8bef" : exception.getClass().getSimpleName();
        } else if (exception == null) {
            summary = raw;
        } else {
            summary = exception.getClass().getSimpleName() + ": " + raw;
        }
        String sanitized = ShelfPointMonitorApp.sanitizeSensitiveText(summary);
        String category = ShelfPointMonitorApp.operationalErrorCategory(raw);
        if (!category.isBlank() && !sanitized.contains(category)) {
            sanitized = category + "：" + sanitized;
        }
        if (sanitized.length() > 180) {
            sanitized = sanitized.substring(0, 177) + "...";
        }
        return sanitized.isBlank() ? "\u672a\u77e5\u9519\u8bef" : sanitized;
    }

    private static String operationalErrorCategory(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lowerCase = raw.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("timeout") || lowerCase.contains("timed out")) {
            return "连接超时";
        }
        if (lowerCase.contains("authentication")
                || lowerCase.contains("password authentication")
                || lowerCase.contains("access denied")
                || lowerCase.contains("invalid authorization")) {
            return "认证失败";
        }
        if (lowerCase.contains("connection refused")) {
            return "连接被拒绝";
        }
        if (lowerCase.contains("config")
                && (lowerCase.contains("read") || lowerCase.contains("load"))) {
            return "配置读取失败";
        }
        if (lowerCase.contains("log")
                && (lowerCase.contains("not writable") || lowerCase.contains("not writeable"))) {
            return "日志目录不可写";
        }
        return "";
    }

    private void closeActiveGroupAlertDialogIfOwnedBy(String groupId, BooleanSupplier publicationAllowed) {
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()
                    || this.activeDialog == null
                    || groupId == null
                    || !groupId.equals(this.activeDialogGroupId)) {
                return;
            }
            this.activeDialog.dispose();
            this.clearActiveDialog();
        });
    }

    private void closeActiveAlertDialog() {
        Runnable closeDialog = () -> {
            JDialog dialog = this.activeDialog;
            this.clearActiveDialog();
            if (dialog != null) {
                dialog.dispose();
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            closeDialog.run();
            return;
        }
        SwingUtilities.invokeLater(closeDialog);
    }

    private void clearActiveDialog() {
        this.activeDialog = null;
        this.activeDialogGroupId = "";
        this.activeDialogGeneration = -1L;
    }

    private void clearActiveDialogIfOwnedBy(JDialog dialog) {
        if (this.activeDialog == dialog) {
            this.clearActiveDialog();
        }
    }

    private boolean isCurrentActiveDialog(JDialog dialog, String groupId, long dialogGeneration) {
        return dialog != null
                && dialog == this.activeDialog
                && dialogGeneration == this.activeDialogGeneration
                && dialogGeneration == this.monitoringGeneration
                && (groupId == null || groupId.equals(this.activeDialogGroupId));
    }

    private void startMonitoring() {
        try {
            DbConfig dbConfig = this.requireCurrentConfig(60);
            List<PointGroupDefinition> groups = this.readGroups();
            this.groupConfigStore.save(groups);
            this.stopMonitoring();
            MonitoringSession session = this.createMonitoringSession(dbConfig);
            this.monitoringSession = session;
            this.captureMonitoredGroups(groups);
            this.clearGroupMonitorState();
            this.scheduledTask = this.monitorExecutor.scheduleWithFixedDelay(
                    () -> this.runWithUiErrorHandling(
                            () -> this.checkDueGroups(session),
                            () -> this.isCurrentMonitoringSession(session)),
                    0L,
                    10L,
                    TimeUnit.SECONDS);
            this.startButton.setEnabled(false);
            this.stopButton.setEnabled(true);
            this.monitorStatusLabel.setText("\u76d1\u63a7\u72b6\u6001\uff1a\u8fd0\u884c\u4e2d");
            this.nextCheckLabel.setText("\u4e0b\u6b21\u68c0\u6d4b\uff1a" + TIME_FORMAT.format(LocalDateTime.now().plusSeconds(10L)));
            this.refreshSystemHealthStatus();
            this.appendStatus("\u5df2\u5f00\u59cb\u70b9\u4f4d\u7ec4\u76d1\u63a7\u3002\u7cfb\u7edf\u6bcf 10 \u79d2\u626b\u63cf\u5230\u671f\u70b9\u4f4d\u7ec4\uff0c\u5404\u7ec4\u6309\u81ea\u8eab\u68c0\u6d4b\u5468\u671f\u67e5\u8be2\u6570\u636e\u5e93\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void stopMonitoring() {
        this.invalidatePendingConnectionOperations();
        ScheduledFuture<?> taskToCancel;
        synchronized (this.groupMonitorLock) {
            ++this.monitoringGeneration;
            taskToCancel = this.scheduledTask;
            this.scheduledTask = null;
            this.clearMonitoringSession();
            this.groupStates.clear();
            this.lastGroupStatuses.clear();
            this.lastGroupEvaluations.clear();
            this.latestDetectionHealth = "\u672a\u68c0\u6d4b";
        }
        if (taskToCancel != null) {
            taskToCancel.cancel(false);
        }
        this.closeInFlightOneShotSessions();
        this.monitoredGroups = List.of();
        this.monitoredLegacyPoints = List.of();
        this.closeActiveAlertDialog();
        this.refreshOverviewPage();
        this.refreshAlertCenterPage();
        this.startButton.setEnabled(true);
        this.stopButton.setEnabled(false);
        this.monitorStatusLabel.setText("\u76d1\u63a7\u72b6\u6001\uff1a\u672a\u8fd0\u884c");
        this.nextCheckLabel.setText("\u4e0b\u6b21\u68c0\u6d4b\uff1a--");
        this.refreshSystemHealthStatus();
    }

    private MonitoringSession createMonitoringSession(DbConfig dbConfig) {
        synchronized (this.groupMonitorLock) {
            return new MonitoringSession(dbConfig, this.currentPassword, ++this.monitoringGeneration);
        }
    }

    private MonitoringSession createOneShotSession(DbConfig dbConfig) {
        synchronized (this.groupMonitorLock) {
            return new MonitoringSession(dbConfig, this.currentPassword, this.monitoringGeneration);
        }
    }

    private void registerOneShotSession(MonitoringSession session) {
        synchronized (this.oneShotSessionLock) {
            this.inFlightOneShotSessions.add(session);
        }
    }

    private void unregisterOneShotSession(MonitoringSession session) {
        synchronized (this.oneShotSessionLock) {
            this.inFlightOneShotSessions.remove(session);
        }
    }

    private void closeInFlightOneShotSessions() {
        List<MonitoringSession> sessions;
        synchronized (this.oneShotSessionLock) {
            sessions = new ArrayList<MonitoringSession>(this.inFlightOneShotSessions);
            this.inFlightOneShotSessions.clear();
        }
        for (MonitoringSession session : sessions) {
            session.close();
        }
    }

    private boolean isCurrentMonitoringSession(MonitoringSession session) {
        return this.isCurrentSessionGeneration(session) && session == this.monitoringSession;
    }

    private boolean isCurrentOneShotSession(MonitoringSession session) {
        return this.isCurrentSessionGeneration(session);
    }

    private boolean isCurrentSessionGeneration(MonitoringSession session) {
        return session != null && !session.isClosed() && session.generation() == this.monitoringGeneration;
    }

    private void clearMonitoringSession() {
        MonitoringSession session = this.monitoringSession;
        this.monitoringSession = null;
        if (session != null) {
            session.close();
        }
    }

    private void clearGroupMonitorState() {
        Object value = this.groupMonitorLock;
        synchronized (value) {
            this.groupStates.clear();
            this.lastGroupStatuses.clear();
            this.lastGroupEvaluations.clear();
        }
        this.refreshOverviewPage();
        this.refreshAlertCenterPage();
    }

    private void showGroupAlertDialog(GroupEvaluation groupEvaluation, BooleanSupplier publicationAllowed) {
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()
                    || this.activeDialog != null && this.activeDialog.isShowing()) {
                return;
            }
            if (this.uiPreferences.alertSoundEnabled()) {
                Toolkit.getDefaultToolkit().beep();
            }
            JDialog jDialog = new JDialog(this, "\u70b9\u4f4d\u7ec4\u7f3a\u6599\u62a5\u8b66", false);
            long dialogGeneration = this.monitoringGeneration;
            jDialog.setAlwaysOnTop(true);
            jDialog.setDefaultCloseOperation(0);
            jDialog.setLayout(new BorderLayout(12, 12));
            jDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            JTextArea jTextArea = new JTextArea(this.groupAlertText(groupEvaluation));
            jTextArea.setEditable(false);
            jTextArea.setLineWrap(true);
            jTextArea.setWrapStyleWord(true);
            jTextArea.setFont(new Font("SansSerif", 0, 15));
            jTextArea.setBackground(new Color(255, 250, 240));
            jDialog.add((Component)jTextArea, "Center");
            jDialog.add((Component)this.buildGroupAlertButtons(
                    groupEvaluation,
                    () -> this.runIoInBackground(this::openLogs),
                    () -> {
                        jDialog.dispose();
                        this.clearActiveDialogIfOwnedBy(jDialog);
                    },
                    () -> publicationAllowed.getAsBoolean()
                            && this.isCurrentActiveDialog(jDialog, groupEvaluation.groupId(), dialogGeneration)),
                    "South");
            jDialog.setSize(640, 420);
            jDialog.setLocationRelativeTo(this);
            this.activeDialog = jDialog;
            this.activeDialogGroupId = groupEvaluation.groupId();
            this.activeDialogGeneration = dialogGeneration;
            jDialog.setVisible(true);
        });
    }

    private String groupAlertText(GroupEvaluation groupEvaluation) {
        String text = System.lineSeparator();
        return "\u68c0\u6d4b\u65f6\u95f4\uff1a" + TIME_FORMAT.format(LocalDateTime.now()) + text + groupEvaluation.areaName() + " / " + groupEvaluation.groupName() + " " + this.alertHeadlineStatusText(groupEvaluation.status()) + text + "\u7269\u6599\uff1a" + groupEvaluation.materialName() + text + "\u4f7f\u7528\u4f4d\uff1a" + (groupEvaluation.usePointEmpty() ? "\u65e0\u6599" : "\u6709\u6599") + text + "\u5907\u7528\u4f4d\uff1a" + groupEvaluation.backupAvailableCount() + "/" + groupEvaluation.backupTotal() + " \u6709\u6599" + text + "\u6301\u7eed\uff1a" + groupEvaluation.continuousMatchedMinutes() + " \u5206\u949f" + text + text + "\u5f02\u5e38\u70b9\u4f4d\u5217\u8868\uff1a" + text + this.abnormalPointText(groupEvaluation) + text + text + "\u62a5\u8b66\u6761\u4ef6\u5df2\u8fbe\u5230\u62a5\u8b66\u65f6\u95f4\uff0c\u8bf7\u73b0\u573a\u786e\u8ba4\u8865\u6599\u6216\u8c03\u5ea6\u72b6\u6001\u3002";
    }

    private String abnormalPointText(GroupEvaluation groupEvaluation) {
        StringBuilder messageBuilder = new StringBuilder();
        for (PointStatusView pointStatusView : groupEvaluation.pointStatuses()) {
            if (pointStatusView.status() != PointMaterialStatus.EMPTY && pointStatusView.status() != PointMaterialStatus.MISSING) continue;
            if (messageBuilder.length() > 0) {
                messageBuilder.append(System.lineSeparator());
            }
            messageBuilder.append(this.roleText(pointStatusView.role())).append(" ").append(pointStatusView.pointCode()).append(" ").append(pointStatusView.statusText()).append(" \u539f\u56e0\uff1a").append(pointStatusView.reason() == null || pointStatusView.reason().isBlank() ? "\u672a\u586b\u5199\u539f\u56e0" : pointStatusView.reason());
        }
        if (messageBuilder.length() == 0) {
            return "\u65e0\u5f02\u5e38\u70b9\u4f4d\u660e\u7ec6";
        }
        return messageBuilder.toString();
    }

    private String alertHeadlineStatusText(GroupAlertStatus groupAlertStatus) {
        if (groupAlertStatus == GroupAlertStatus.ACTIVE_ALERT) {
            return "\u9700\u8981\u5173\u6ce8";
        }
        return GroupStatusText.statusText(groupAlertStatus);
    }

    private JPanel buildGroupAlertButtons(GroupEvaluation groupEvaluation, Runnable runnable) {
        return this.buildGroupAlertButtons(
                groupEvaluation,
                () -> this.runIoInBackground(this::openLogs),
                runnable,
                () -> true);
    }

    JPanel buildGroupAlertButtons(GroupEvaluation groupEvaluation, Runnable runnable, Runnable runnable2) {
        return this.buildGroupAlertButtons(groupEvaluation, runnable, runnable2, () -> true);
    }

    private JPanel buildGroupAlertButtons(
            GroupEvaluation groupEvaluation,
            Runnable runnable,
            Runnable runnable2,
            BooleanSupplier acknowledgementAllowed) {
        JButton button = new JButton("\u6253\u5f00\u65e5\u5fd7\u76ee\u5f55");
        button.addActionListener(actionEvent -> {
            if (runnable != null) {
                runnable.run();
            }
        });
        JButton button2 = new JButton("\u5df2\u5173\u6ce8");
        button2.addActionListener(actionEvent -> {
            this.acknowledgeGroupAlert(groupEvaluation, acknowledgementAllowed);
            if (runnable2 != null) {
                runnable2.run();
            }
        });
        JPanel panel = new JPanel(new FlowLayout(2));
        panel.add(button);
        panel.add(button2);
        return panel;
    }

    private void acknowledgeGroupAlert(GroupEvaluation groupEvaluation) {
        this.acknowledgeGroupAlert(groupEvaluation, () -> true);
    }

    private void acknowledgeGroupAlert(
            GroupEvaluation groupEvaluation,
            BooleanSupplier acknowledgementAllowed) {
        if (!acknowledgementAllowed.getAsBoolean()) {
            return;
        }
        synchronized (this.groupMonitorLock) {
            if (!acknowledgementAllowed.getAsBoolean()) {
                return;
            }
            GroupRuntimeState groupRuntimeState = this.groupStates.get(groupEvaluation.groupId());
            if (groupRuntimeState != null) {
                groupRuntimeState.acknowledge();
            }
            this.lastGroupStatuses.put(groupEvaluation.groupId(), GroupAlertStatus.ACKED_ALERT);
        }
        this.enqueueIoOperation(
                acknowledgementAllowed,
                "CSV\u4e8b\u4ef6\u65e5\u5fd7\u5199\u5165\u5931\u8d25",
                () -> this.groupLogWriter.appendEvent(LocalDateTime.now(), "ACKNOWLEDGED", groupEvaluation));
        this.appendStatus("\u7528\u6237\u5df2\u5173\u6ce8\u70b9\u4f4d\u7ec4\u62a5\u8b66\uff1a" + groupEvaluation.groupId(),
                acknowledgementAllowed);
    }

    private String roleText(PointRole pointRole) {
        if (pointRole == PointRole.USE) {
            return "\u4f7f\u7528\u4f4d";
        }
        if (pointRole == PointRole.BACKUP) {
            return "\u5907\u7528\u4f4d";
        }
        return "\u70b9\u4f4d";
    }

    private void refreshOverviewPage() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshOverviewPage);
            return;
        }
        ArrayList<PointGroupDefinition> arrayList = new ArrayList<PointGroupDefinition>(this.pointGroups);
        arrayList.sort(Comparator
                .comparingInt((PointGroupDefinition pointGroupDefinition) ->
                        this.statusPriority(this.groupStatus(pointGroupDefinition)))
                .thenComparing(PointGroupDefinition::areaName)
                .thenComparing(PointGroupDefinition::groupName));
        int index = 0;
        int index2 = 0;
        int index3 = 0;
        this.overviewModel.setRowCount(0);
        this.overviewRowGroupIds.clear();
        for (PointGroupDefinition pointGroupDefinition2 : arrayList) {
            GroupEvaluation groupEvaluation = this.latestEvaluation(pointGroupDefinition2.id());
            GroupAlertStatus groupAlertStatus = this.groupStatus(pointGroupDefinition2);
            if (groupAlertStatus == GroupAlertStatus.ACTIVE_ALERT || groupAlertStatus == GroupAlertStatus.ACKED_ALERT) {
                ++index;
            } else if (groupAlertStatus == GroupAlertStatus.PENDING_ALERT) {
                ++index2;
            } else if (groupAlertStatus == GroupAlertStatus.QUERY_FAILED) {
                ++index3;
            }
            this.overviewModel.addRow(new Object[]{this.statusTextForOverview(pointGroupDefinition2, groupAlertStatus), pointGroupDefinition2.areaName(), pointGroupDefinition2.groupName(), this.useStateText(groupEvaluation), this.backupStateText(groupEvaluation, pointGroupDefinition2), this.durationText(groupEvaluation), this.lastCheckedText(pointGroupDefinition2.id())});
            this.overviewRowGroupIds.add(pointGroupDefinition2.id());
        }
        this.overviewGroupCountLabel.setText(String.valueOf(arrayList.size()));
        this.overviewAlertCountLabel.setText(String.valueOf(index));
        this.overviewPendingCountLabel.setText(String.valueOf(index2));
        this.overviewDataErrorCountLabel.setText(String.valueOf(index3));
        if (this.overviewTable.getRowCount() > 0 && this.overviewTable.getSelectedRow() < 0) {
            this.overviewTable.setRowSelectionInterval(0, 0);
        }
        this.updateOverviewDetail();
    }

    private GroupAlertStatus groupStatus(PointGroupDefinition pointGroupDefinition) {
        if (!pointGroupDefinition.enabled()) {
            return GroupAlertStatus.NORMAL;
        }
        GroupEvaluation groupEvaluation = this.latestEvaluation(pointGroupDefinition.id());
        return groupEvaluation == null ? GroupAlertStatus.NORMAL : groupEvaluation.status();
    }

    private GroupEvaluation latestEvaluation(String text) {
        Object value = this.groupMonitorLock;
        synchronized (value) {
            return this.lastGroupEvaluations.get(text);
        }
    }

    private int statusPriority(GroupAlertStatus groupAlertStatus) {
        if (groupAlertStatus == GroupAlertStatus.ACTIVE_ALERT) {
            return 0;
        }
        if (groupAlertStatus == GroupAlertStatus.QUERY_FAILED) {
            return 1;
        }
        if (groupAlertStatus == GroupAlertStatus.PENDING_ALERT) {
            return 2;
        }
        if (groupAlertStatus == GroupAlertStatus.ACKED_ALERT) {
            return 4;
        }
        if (groupAlertStatus == GroupAlertStatus.NORMAL) {
            return 5;
        }
        return 6;
    }

    private String statusTextForOverview(PointGroupDefinition pointGroupDefinition, GroupAlertStatus groupAlertStatus) {
        if (!pointGroupDefinition.enabled()) {
            return "\u505c\u7528";
        }
        GroupEvaluation groupEvaluation = this.latestEvaluation(pointGroupDefinition.id());
        if (groupEvaluation == null) {
            return "\u672a\u68c0\u6d4b";
        }
        if (groupAlertStatus == GroupAlertStatus.QUERY_FAILED) {
            return OverviewPage.QUERY_FAILURE_TEXT;
        }
        return GroupStatusText.statusText(groupAlertStatus);
    }

    private String useStateText(GroupEvaluation groupEvaluation) {
        if (groupEvaluation == null) {
            return "--";
        }
        if (groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED) {
            return "\u67e5\u8be2\u5931\u8d25";
        }
        return groupEvaluation.usePointEmpty() ? "\u65e0\u6599" : "\u6709\u6599";
    }

    private String backupStateText(GroupEvaluation groupEvaluation, PointGroupDefinition pointGroupDefinition) {
        if (groupEvaluation == null) {
            int index = 0;
            for (GroupMonitorPoint groupMonitorPoint : pointGroupDefinition.points()) {
                if (!groupMonitorPoint.enabled() || groupMonitorPoint.role() != PointRole.BACKUP) continue;
                ++index;
            }
            return "--/" + index;
        }
        if (groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED) {
            return "\u6570\u636e\u4e0d\u53ef\u7528";
        }
        return groupEvaluation.backupAvailableCount() + "/" + groupEvaluation.backupTotal();
    }

    private String durationText(GroupEvaluation groupEvaluation) {
        if (groupEvaluation == null) {
            return "--";
        }
        return groupEvaluation.continuousMatchedMinutes() + " \u5206\u949f";
    }

    private String lastCheckedText(String text) {
        Object value = this.groupMonitorLock;
        synchronized (value) {
            GroupRuntimeState groupRuntimeState = this.groupStates.get(text);
            if (groupRuntimeState == null || groupRuntimeState.lastCheckedAt() == null) {
                return "--";
            }
            return TIME_FORMAT.format(groupRuntimeState.lastCheckedAt());
        }
    }

    private void updateOverviewDetail() {
        int index = this.overviewTable.getSelectedRow();
        if (index < 0) {
            this.overviewDetailArea.setText("\u8bf7\u9009\u62e9\u70b9\u4f4d\u7ec4\u3002");
            return;
        }
        PointGroupDefinition pointGroupDefinition = this.findGroupByOverviewRow(index);
        if (pointGroupDefinition == null) {
            this.overviewDetailArea.setText("\u672a\u627e\u5230\u5bf9\u5e94\u70b9\u4f4d\u7ec4\u3002");
            return;
        }
        GroupEvaluation groupEvaluation = this.latestEvaluation(pointGroupDefinition.id());
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("\u533a\u57df / \u7269\u6599\u7ec4\uff1a").append(pointGroupDefinition.areaName()).append(" / ").append(pointGroupDefinition.groupName()).append(System.lineSeparator());
        messageBuilder.append("\u5f53\u524d\u72b6\u6001\uff1a").append(this.statusTextForOverview(pointGroupDefinition, this.groupStatus(pointGroupDefinition))).append(System.lineSeparator());
        messageBuilder.append("\u89c4\u5219\u6458\u8981\uff1a\u4f7f\u7528\u4f4d\u65e0\u8d27\u67b6=").append(pointGroupDefinition.rule().requireUsePointEmpty() ? "\u662f" : "\u5426").append("\uff0c\u6700\u5c11\u5907\u7528\u4f4d\u6709\u6599 ").append(pointGroupDefinition.rule().minBackupAvailable()).append("\uff0c\u6301\u7eed ").append(pointGroupDefinition.rule().durationMinutes()).append(" \u5206\u949f").append(System.lineSeparator());
        messageBuilder.append("\u6301\u7eed\u65f6\u95f4\uff1a").append(this.durationText(groupEvaluation)).append(System.lineSeparator());
        messageBuilder.append("\u4e0a\u6b21\u68c0\u6d4b\uff1a").append(this.lastCheckedText(pointGroupDefinition.id())).append(System.lineSeparator());
        messageBuilder.append(System.lineSeparator()).append("\u70b9\u4f4d\u660e\u7ec6\uff1a").append(System.lineSeparator());
        if (groupEvaluation != null && !groupEvaluation.pointStatuses().isEmpty()) {
            for (PointStatusView pointStatusView : groupEvaluation.pointStatuses()) {
                messageBuilder.append(this.roleText(pointStatusView.role())).append(" / ").append(pointStatusView.alias()).append(" / ").append(pointStatusView.pointCode()).append(" / ").append(pointStatusView.statusText()).append(" / ").append(pointStatusView.updatedAt() == null ? "--" : TIME_FORMAT.format(pointStatusView.updatedAt())).append(System.lineSeparator());
            }
        } else {
            for (GroupMonitorPoint groupMonitorPoint : pointGroupDefinition.points()) {
                messageBuilder.append(this.roleText(groupMonitorPoint.role())).append(" / ").append(groupMonitorPoint.alias()).append(" / ").append(groupMonitorPoint.code()).append(" / \u672a\u68c0\u6d4b / --").append(System.lineSeparator());
            }
        }
        this.overviewDetailArea.setText(messageBuilder.toString());
    }

    private PointGroupDefinition findGroupByOverviewRow(int index) {
        int index2 = this.overviewTable.convertRowIndexToModel(index);
        if (index2 >= 0 && index2 < this.overviewRowGroupIds.size()) {
            return this.findGroupById(this.overviewRowGroupIds.get(index2));
        }
        return null;
    }

    private void acknowledgeSelectedOverviewAlert() {
        PointGroupDefinition pointGroupDefinition = this.findGroupByOverviewRow(this.overviewTable.getSelectedRow());
        if (pointGroupDefinition == null) {
            this.showError(new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u70b9\u4f4d\u7ec4"));
            return;
        }
        GroupEvaluation groupEvaluation = this.latestEvaluation(pointGroupDefinition.id());
        if (groupEvaluation == null || groupEvaluation.status() != GroupAlertStatus.ACTIVE_ALERT) {
            this.showError(new IllegalArgumentException("\u5f53\u524d\u70b9\u4f4d\u7ec4\u6ca1\u6709\u53ef\u5173\u6ce8\u7684\u7f3a\u6599\u62a5\u8b66"));
            return;
        }
        this.acknowledgeGroupAlert(groupEvaluation);
        this.refreshOverviewPage();
        this.refreshAlertCenterPage();
    }

    private void refreshAlertCenterPage() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshAlertCenterPage);
            return;
        }
        String text = String.valueOf(this.alertCenterFilterBox.getSelectedItem());
        this.alertCenterModel.setRowCount(0);
        this.alertCenterEntries = List.of();
        if (this.isRecoveredAlertFilter(text)) {
            this.runOnceInBackground(() -> {
                List<AlertCenterEntry> list = this.recoveredAlertCenterEntries();
                SwingUtilities.invokeLater(() -> this.applyAlertCenterEntries(list));
            });
            this.updateAlertCenterDetail();
            return;
        }
        this.applyAlertCenterEntries(this.liveAlertCenterEntries(text));
    }

    private List<AlertCenterEntry> liveAlertCenterEntries(String text) {
        ArrayList<AlertCenterEntry> arrayList = new ArrayList<AlertCenterEntry>();
        Object value = this.groupMonitorLock;
        synchronized (value) {
            for (GroupEvaluation groupEvaluation : this.lastGroupEvaluations.values()) {
                if (!this.matchesAlertFilter(text, groupEvaluation.status())) continue;
                arrayList.add(new AlertCenterEntry(groupEvaluation.groupId(), groupEvaluation.areaName(), groupEvaluation.groupName(), "LIVE", groupEvaluation.status(), this.lastCheckedText(groupEvaluation.groupId()), "\u5f53\u524d\u8fd0\u884c\u72b6\u6001", this.alertReason(groupEvaluation), groupEvaluation));
            }
        }
        return arrayList;
    }

    private List<AlertCenterEntry> recoveredAlertCenterEntries() {
        Path path = Paths.get("logs", "event-log.csv");
        if (!Files.exists(path)) {
            return List.of();
        }
        ArrayList<AlertCenterEntry> arrayList = new ArrayList<AlertCenterEntry>();
        try {
            for (String text : ShelfPointMonitorApp.tailLines(path, 1000)) {
                String text2;
                List<String> list;
                if (text.startsWith("event_at,") || (list = ShelfPointMonitorApp.parseCsvLine(text)).size() < 8 || !"RECOVERED".equals(text2 = list.get(1))) continue;
                arrayList.add(new AlertCenterEntry(list.get(2), list.get(3), list.get(4), text2, this.parseStatus(list.get(6)), list.get(0), "event-log.csv", this.eventTypeText(text2) + "\uff1a" + ShelfPointMonitorApp.sanitizeVisibleLog(list.get(7)), null));
            }
        }
        catch (Exception exception) {
            this.appendStatus("\u8bfb\u53d6\u62a5\u8b66\u4e8b\u4ef6\u65e5\u5fd7\u5931\u8d25\uff1a" + ShelfPointMonitorApp.sanitizedExceptionSummary(exception));
        }
        return arrayList;
    }

    private void applyAlertCenterEntries(List<AlertCenterEntry> list) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> this.applyAlertCenterEntries(list));
            return;
        }
        this.alertCenterEntries = List.copyOf(list);
        this.alertCenterModel.setRowCount(0);
        for (AlertCenterEntry alertCenterEntry : this.alertCenterEntries) {
            GroupEvaluation groupEvaluation = alertCenterEntry.liveEvaluation;
            this.alertCenterModel.addRow(new Object[]{alertCenterEntry.occurredAt, alertCenterEntry.areaName, alertCenterEntry.groupName, alertCenterEntry.description, groupEvaluation == null ? "--" : this.durationText(groupEvaluation), GroupStatusText.statusText(alertCenterEntry.status)});
        }
        if (this.alertCenterTable.getRowCount() > 0 && this.alertCenterTable.getSelectedRow() < 0) {
            this.alertCenterTable.setRowSelectionInterval(0, 0);
        }
        this.updateAlertCenterDetail();
    }

    private boolean isRecoveredAlertFilter(String text) {
        return "\u5df2\u6062\u590d".equals(text) || "\u5bb8\u53c9\u4eee\u6fb6?".equals(text);
    }

    private boolean matchesAlertFilter(String text, GroupAlertStatus groupAlertStatus) {
        if ("\u6d3b\u8dc3\u62a5\u8b66".equals(text)) {
            return groupAlertStatus == GroupAlertStatus.ACTIVE_ALERT;
        }
        if ("\u5df2\u5173\u6ce8".equals(text)) {
            return groupAlertStatus == GroupAlertStatus.ACKED_ALERT;
        }
        if ("\u89c2\u5bdf\u4e2d".equals(text)) {
            return groupAlertStatus == GroupAlertStatus.PENDING_ALERT;
        }
        if ("\u67e5\u8be2\u5931\u8d25".equals(text)) {
            return groupAlertStatus == GroupAlertStatus.QUERY_FAILED;
        }
        return !"\u5df2\u6062\u590d".equals(text);
    }

    private String alertReason(GroupEvaluation groupEvaluation) {
        if (groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED) {
            return "\u67e5\u8be2\u5931\u8d25\uff1a" + this.safeMessage(groupEvaluation.message());
        }
        if (groupEvaluation.status() == GroupAlertStatus.ACTIVE_ALERT) {
            return "\u7f3a\u6599\u62a5\u8b66\uff1a" + this.safeMessage(groupEvaluation.message());
        }
        if (groupEvaluation.status() == GroupAlertStatus.ACKED_ALERT) {
            return "\u5df2\u5173\u6ce8\uff0c\u7b49\u5f85\u5b9e\u9645\u6062\u590d";
        }
        return this.safeMessage(groupEvaluation.message());
    }

    private String safeMessage(String text) {
        return text == null || text.isBlank()
                ? GroupStatusText.statusText(GroupAlertStatus.NORMAL)
                : ShelfPointMonitorApp.sanitizeVisibleLog(text);
    }

    private GroupAlertStatus parseStatus(String text) {
        try {
            return GroupAlertStatus.valueOf(text);
        }
        catch (Exception exception) {
            return GroupAlertStatus.NORMAL;
        }
    }

    private void updateAlertCenterDetail() {
        int index = this.alertCenterTable.getSelectedRow();
        if (index < 0) {
            this.alertCenterDetailArea.setText("\u5f53\u524d\u65e0\u62a5\u8b66\u4e8b\u4ef6\u3002\u67e5\u8be2\u5931\u8d25\u5c5e\u4e8e\u7cfb\u7edf\u6570\u636e\u5f02\u5e38\uff0c\u4e0d\u5c5e\u4e8e\u7f3a\u6599\u62a5\u8b66\u3002");
            return;
        }
        int index2 = this.alertCenterTable.convertRowIndexToModel(index);
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("\u5f53\u524d\u72b6\u6001\uff1a").append(this.alertCenterModel.getValueAt(index2, 5)).append(System.lineSeparator());
        messageBuilder.append("\u89e6\u53d1\u65f6\u95f4\uff1a").append(this.alertCenterModel.getValueAt(index2, 0)).append(System.lineSeparator());
        messageBuilder.append("\u6301\u7eed\u65f6\u95f4\uff1a").append(this.alertCenterModel.getValueAt(index2, 4)).append(System.lineSeparator());
        messageBuilder.append("\u89c4\u5219\u547d\u4e2d\u5185\u5bb9\uff1a").append(this.alertCenterModel.getValueAt(index2, 3)).append(System.lineSeparator());
        messageBuilder.append("\u70b9\u4f4d\u660e\u7ec6\uff1a\u8bf7\u5728\u76d1\u63a7\u603b\u89c8\u67e5\u770b\u5f53\u524d\u70b9\u4f4d\u72b6\u6001\u3002").append(System.lineSeparator());
        messageBuilder.append("\u5904\u7406\u65f6\u95f4\u7ebf\uff1a\u4e8b\u4ef6\u6765\u81ea\u672c\u5730 event-log.csv \u548c\u672c\u6b21\u8fd0\u884c\u68c0\u6d4b\u7ed3\u679c\u3002");
        this.alertCenterDetailArea.setText(messageBuilder.toString());
    }

    private void acknowledgeSelectedAlertCenterGroup() {
        GroupEvaluation groupEvaluation;
        AlertCenterEntry alertCenterEntry = this.selectedAlertCenterEntry();
        GroupEvaluation groupEvaluation2 = groupEvaluation = alertCenterEntry == null ? null : alertCenterEntry.liveEvaluation;
        if (groupEvaluation == null || groupEvaluation.status() != GroupAlertStatus.ACTIVE_ALERT) {
            this.showError(new IllegalArgumentException("\u67e5\u8be2\u5931\u8d25\u6216\u975e\u6d3b\u8dc3\u7f3a\u6599\u62a5\u8b66\u4e0d\u80fd\u6807\u8bb0\u4e3a\u5df2\u5173\u6ce8"));
            return;
        }
        this.acknowledgeGroupAlert(groupEvaluation);
        this.refreshAlertCenterPage();
    }

    private void checkSelectedAlertCenterGroup() {
        AlertCenterEntry alertCenterEntry = this.selectedAlertCenterEntry();
        if (alertCenterEntry == null) {
            this.showError(new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u5f53\u524d\u8fd0\u884c\u4e2d\u7684\u70b9\u4f4d\u7ec4\u4e8b\u4ef6"));
            return;
        }
        PointGroupDefinition pointGroupDefinition = this.findGroupById(alertCenterEntry.groupId);
        if (pointGroupDefinition == null) {
            this.showError(new IllegalArgumentException("\u672a\u627e\u5230\u70b9\u4f4d\u7ec4\u914d\u7f6e"));
            return;
        }
        try {
            DbConfig dbConfig = this.requireCurrentConfig(60);
            MonitoringSession session = this.createOneShotSession(dbConfig);
            this.registerOneShotSession(session);
            this.runMonitorInBackground(() -> {
                try {
                    this.checkGroups(session, List.of(pointGroupDefinition), LocalDateTime.now(), "\u624b\u52a8\u68c0\u6d4b",
                            () -> this.isCurrentOneShotSession(session));
                }
                finally {
                    session.clearPassword();
                    this.unregisterOneShotSession(session);
                }
            }, () -> this.isCurrentOneShotSession(session));
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private GroupEvaluation selectedAlertCenterEvaluation() {
        AlertCenterEntry alertCenterEntry = this.selectedAlertCenterEntry();
        return alertCenterEntry == null ? null : alertCenterEntry.liveEvaluation;
    }

    private AlertCenterEntry selectedAlertCenterEntry() {
        int index = this.alertCenterTable.getSelectedRow();
        if (index < 0) {
            return null;
        }
        int index2 = this.alertCenterTable.convertRowIndexToModel(index);
        if (index2 >= 0 && index2 < this.alertCenterEntries.size()) {
            return this.alertCenterEntries.get(index2);
        }
        return null;
    }

    private void showSelectedAlertCenterGroupInOverview() {
        AlertCenterEntry alertCenterEntry = this.selectedAlertCenterEntry();
        if (alertCenterEntry == null) {
            this.showError(new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u62a5\u8b66\u4e2d\u5fc3\u4e8b\u4ef6"));
            return;
        }
        this.navigationList.setSelectedValue(PAGE_OVERVIEW, true);
        this.selectOverviewGroup(alertCenterEntry.groupId);
    }

    private void selectOverviewGroup(String text) {
        this.refreshOverviewPage();
        for (int i = 0; i < this.overviewRowGroupIds.size(); ++i) {
            if (!this.overviewRowGroupIds.get(i).equals(text)) continue;
            int index = this.overviewTable.convertRowIndexToView(i);
            if (index >= 0) {
                this.overviewTable.setRowSelectionInterval(index, index);
                this.overviewTable.scrollRectToVisible(this.overviewTable.getCellRect(index, 0, true));
            }
            return;
        }
    }

    private PointGroupDefinition findGroupById(String text) {
        for (PointGroupDefinition pointGroupDefinition : this.pointGroups) {
            if (!pointGroupDefinition.id().equals(text)) continue;
            return pointGroupDefinition;
        }
        return null;
    }

    private void resetPointDataQueryPage() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::resetPointDataQueryPage);
            return;
        }
        this.queryCurrentPage = 1;
        this.queryPrevButton.setEnabled(false);
        this.queryPageLabel.setText("\u7b2c 1 / -- \u9875");
    }

    private void showPreviousPointDataQueryPage() {
        if (this.queryCurrentPage > 1) {
            --this.queryCurrentPage;
            this.startPointDataQuery(false);
        }
    }

    private void showNextPointDataQueryPage() {
        ++this.queryCurrentPage;
        this.startPointDataQuery(false);
    }

    private void exportCurrentPointDataQueryResult() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::exportCurrentPointDataQueryResult);
            return;
        }
        List<String> headers = new ArrayList<String>();
        for (int column = 0; column < this.dataQueryModel.getColumnCount(); column++) {
            headers.add(this.dataQueryModel.getColumnName(column));
        }
        List<List<String>> rows = new ArrayList<List<String>>();
        for (int row = 0; row < this.dataQueryModel.getRowCount(); row++) {
            List<String> values = new ArrayList<String>();
            for (int column = 0; column < this.dataQueryModel.getColumnCount(); column++) {
                Object value = this.dataQueryModel.getValueAt(row, column);
                values.add(value == null ? "" : String.valueOf(value));
            }
            rows.add(List.copyOf(values));
        }
        List<String> headerSnapshot = List.copyOf(headers);
        List<List<String>> rowSnapshot = List.copyOf(rows);
        Path output = Paths.get("exports", "query-page-" + System.currentTimeMillis() + ".csv");
        this.runIoInBackground(() -> {
            CsvExportService.writeUtf8(output, headerSnapshot, rowSnapshot);
            this.appendStatus("\u5df2\u5bfc\u51fa\u5f53\u524d\u67e5\u8be2\u7ed3\u679c\uff1a" + output.getFileName());
        });
    }

    private void startPointDataQuery(boolean flag) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> this.startPointDataQuery(flag));
            return;
        }
        try {
            PointDataQuery pointDataQuery;
            DbConfig dbConfig = this.requireCurrentConfig(30);
            if (flag) {
                this.queryCurrentPage = 1;
            }
            int index = (Integer)this.queryLimitSpinner.getValue();
            this.lastPointDataQuery = pointDataQuery = new PointDataQuery(this.queryPointKeywordField.getText(), this.queryShelfKeywordField.getText(), this.queryAreaCodeField.getText(), this.queryRelateAreaCodeField.getText(), this.queryUpdatedFromField.getText(), this.queryUpdatedToField.getText(), index, (this.queryCurrentPage - 1) * index);
            char[] passwordChars = Arrays.copyOf(this.currentPassword, this.currentPassword.length);
            this.runOnceInBackground(() -> this.executePointDataQuery(dbConfig, pointDataQuery, passwordChars));
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void executePointDataQuery(DbConfig dbConfig, PointDataQuery pointDataQuery, char[] passwordChars) throws Exception {
        try {
            PointDataQueryResult pointDataQueryResult = this.pointDataQueryRepository.query(dbConfig, passwordChars, pointDataQuery);
            SwingUtilities.invokeLater(() -> this.applyPointDataQueryResult(pointDataQueryResult));
        }
        finally {
            Arrays.fill(passwordChars, '\u0000');
        }
    }

    private void applyPointDataQueryResult(PointDataQueryResult pointDataQueryResult) {
        this.dataQueryModel.setRowCount(0);
        for (PointRecord pointRecord : pointDataQueryResult.records()) {
            this.dataQueryModel.addRow(new Object[]{pointRecord.mapDataCode(), this.blankToDash(pointRecord.podCode()), this.blankToDash(pointRecord.podStatus()), this.materialText(pointRecord), pointRecord.indLock() == 0 ? "\u672a\u9501\u5b9a" : "\u9501\u5b9a", this.blankToDash(pointRecord.areaCode()), this.blankToDash(pointRecord.relateAreaCode()), pointRecord.dateChg() == null ? "--" : TIME_FORMAT.format(pointRecord.dateChg()), "\u6210\u529f"});
        }
        this.queryTotalCount = pointDataQueryResult.totalCount();
        int index = Math.max(1, pointDataQueryResult.limit());
        int index2 = this.queryTotalCount == 0 ? 0 : (this.queryTotalCount + index - 1) / index;
        this.queryCurrentPage = index2 == 0 ? 1 : Math.min(index2, pointDataQueryResult.offset() / index + 1);
        this.queryPrevButton.setEnabled(index2 > 0 && this.queryCurrentPage > 1);
        this.queryNextButton.setEnabled(index2 > 0 && this.queryCurrentPage < index2);
        this.queryPageLabel.setText("\u7b2c " + (index2 == 0 ? 0 : this.queryCurrentPage) + " / " + index2 + " \u9875");
        this.queryTotalLabel.setText("\u603b\u8bb0\u5f55\u6570\uff1a" + this.queryTotalCount);
        this.dataQueryDetailArea.setText("\u53ea\u8bfb\u67e5\u8be2\u5b8c\u6210\uff0c\u8fd4\u56de " + pointDataQueryResult.records().size() + " \u884c\u3002\n\u603b\u8bb0\u5f55\u6570\uff1a" + pointDataQueryResult.totalCount() + "\n\u5f53\u524d\u504f\u79fb\uff1a" + pointDataQueryResult.offset() + "\nSQL\u7c7b\u578b\uff1a" + pointDataQueryResult.sqlKind() + "\n\u56fa\u5b9a\u6765\u6e90\uff1atcs_map_data\n\u4e0d\u652f\u6301 SQL \u7f16\u8f91\n\u4e0d\u652f\u6301\u6570\u636e\u4fee\u6539");
        this.appendStatus("\u7ed3\u6784\u5316\u53ea\u8bfb\u67e5\u8be2\u5b8c\u6210\uff0c\u8fd4\u56de " + pointDataQueryResult.records().size() + " \u884c\uff0c\u603b\u8bb0\u5f55\u6570 " + pointDataQueryResult.totalCount() + "\u3002");
    }

    private String materialText(PointRecord pointRecord) {
        if (MonitorLogic.isBlank(pointRecord.podCode())) {
            return "\u65e0\u6599";
        }
        if (pointRecord.status() != 1 || pointRecord.indLock() != 0) {
            return "\u5f02\u5e38";
        }
        return "\u6709\u6599";
    }

    private String blankToDash(String text) {
        return text == null || text.isBlank() ? "--" : text;
    }

    private void updateDataQueryDetail() {
        int index = this.dataQueryTable.getSelectedRow();
        if (index < 0) {
            return;
        }
        int index2 = this.dataQueryTable.convertRowIndexToModel(index);
        StringBuilder messageBuilder = new StringBuilder("\u9009\u4e2d\u8bb0\u5f55\u5b57\u6bb5\uff1a").append(System.lineSeparator());
        for (int i = 0; i < this.dataQueryModel.getColumnCount(); ++i) {
            messageBuilder.append(this.dataQueryModel.getColumnName(i)).append("\uff1a").append(this.dataQueryModel.getValueAt(index2, i)).append(System.lineSeparator());
        }
        messageBuilder.append(System.lineSeparator()).append("\u53ea\u8bfb\u67e5\u8be2").append(System.lineSeparator()).append("\u4e0d\u652f\u6301 SQL \u7f16\u8f91").append(System.lineSeparator()).append("\u4e0d\u652f\u6301\u6570\u636e\u4fee\u6539");
        this.dataQueryDetailArea.setText(messageBuilder.toString());
    }

    private void loadSystemLogs() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::loadSystemLogs);
            return;
        }
        this.lastSystemLogRefreshAtMillis = System.currentTimeMillis();
        SystemLogFilter systemLogFilter = new SystemLogFilter(String.valueOf(this.systemLogTypeFilterBox.getSelectedItem()), this.systemLogFromField.getText().trim(), this.systemLogToField.getText().trim(), this.systemLogGroupField.getText().trim(), this.systemLogKeywordField.getText().trim());
        this.runIoInBackground(() -> {
            List<SystemLogEntry> list = this.readSystemLogEntries(systemLogFilter);
            SwingUtilities.invokeLater(() -> this.applySystemLogEntries(list));
        });
    }

    private void requestSystemLogRefreshDebounced() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::requestSystemLogRefreshDebounced);
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - this.lastSystemLogRefreshAtMillis;
        if (elapsed >= 5000L) {
            this.loadSystemLogs();
            return;
        }
        int delay = (int)Math.max(1L, 5000L - elapsed);
        if (this.systemLogRefreshDebounceTimer != null) {
            this.systemLogRefreshDebounceTimer.stop();
        }
        this.systemLogRefreshDebounceTimer = new Timer(delay, actionEvent -> this.loadSystemLogs());
        this.systemLogRefreshDebounceTimer.setRepeats(false);
        this.systemLogRefreshDebounceTimer.start();
    }

    private List<SystemLogEntry> readSystemLogEntries(SystemLogFilter systemLogFilter) {
        ArrayList<SystemLogEntry> arrayList = new ArrayList<SystemLogEntry>();
        this.appendSystemLogEntries(arrayList, systemLogFilter, Paths.get("logs", "event-log.csv"), "\u4e8b\u4ef6\u65e5\u5fd7", 1000);
        this.appendSystemLogEntries(arrayList, systemLogFilter, Paths.get("logs", "check-log.csv"), "\u68c0\u6d4b\u65e5\u5fd7", 1000);
        this.appendMonitorLogEntries(arrayList, systemLogFilter);
        return arrayList;
    }

    private void appendSystemLogEntries(List<SystemLogEntry> list, SystemLogFilter systemLogFilter, Path path, String text, int index) {
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String text2 : ShelfPointMonitorApp.tailLines(path, index)) {
                List<String> list2;
                if (text2.startsWith("event_at,") || text2.startsWith("checked_at,") || (list2 = ShelfPointMonitorApp.parseCsvLine(text2)).size() < 2) continue;
                String text3 = text.equals("\u4e8b\u4ef6\u65e5\u5fd7") ? this.eventTypeText(list2.get(1)) : "\u68c0\u6d4b\u5b8c\u6210";
                String text4 = list2.size() > 2 ? list2.get(2) : "";
                String text5 = list2.isEmpty() ? "" : list2.get(list2.size() - 1);
                this.addSystemLogEntry(list, systemLogFilter, new SystemLogEntry(list2.get(0), text3, "\u4fe1\u606f", text4, ShelfPointMonitorApp.sanitizeVisibleLog(text5), text));
            }
        }
        catch (Exception exception) {
            this.addSystemLogEntry(list, systemLogFilter, new SystemLogEntry(TIME_FORMAT.format(LocalDateTime.now()), "\u65e5\u5fd7\u8bfb\u53d6\u5931\u8d25", "\u8b66\u544a", "", ShelfPointMonitorApp.sanitizedExceptionSummary(exception), text));
        }
    }

    private void appendMonitorLogEntries(List<SystemLogEntry> list, SystemLogFilter systemLogFilter) {
        if (!Files.exists(this.logPath)) {
            return;
        }
        try {
            for (String text : ShelfPointMonitorApp.tailLines(this.logPath, 200)) {
                this.addSystemLogEntry(list, systemLogFilter, new SystemLogEntry("", "\u8fd0\u884c\u65e5\u5fd7", "\u4fe1\u606f", "", ShelfPointMonitorApp.sanitizeVisibleLog(text), "monitor.log"));
            }
        }
        catch (Exception exception) {
            this.addSystemLogEntry(list, systemLogFilter, new SystemLogEntry(TIME_FORMAT.format(LocalDateTime.now()), "\u65e5\u5fd7\u8bfb\u53d6\u5931\u8d25", "\u8b66\u544a", "", ShelfPointMonitorApp.sanitizedExceptionSummary(exception), "monitor.log"));
        }
    }

    private void addSystemLogEntry(List<SystemLogEntry> list, SystemLogFilter systemLogFilter, SystemLogEntry systemLogEntry) {
        if (!"\u5168\u90e8".equals(systemLogFilter.type) && !systemLogFilter.type.equals(systemLogEntry.type)) {
            return;
        }
        if (!systemLogFilter.groupId.isBlank() && !systemLogEntry.groupId.contains(systemLogFilter.groupId)) {
            return;
        }
        if (!systemLogFilter.from.isBlank() && !systemLogEntry.time.isBlank() && systemLogEntry.time.compareTo(systemLogFilter.from) < 0) {
            return;
        }
        if (!systemLogFilter.to.isBlank() && !systemLogEntry.time.isBlank() && systemLogEntry.time.compareTo(systemLogFilter.to) > 0) {
            return;
        }
        String text = systemLogEntry.time + " " + systemLogEntry.type + " " + systemLogEntry.groupId + " " + systemLogEntry.description + " " + systemLogEntry.source;
        if (!systemLogFilter.keyword.isBlank() && !text.contains(systemLogFilter.keyword)) {
            return;
        }
        list.add(systemLogEntry);
    }

    private void applySystemLogEntries(List<SystemLogEntry> list) {
        this.systemLogModel.setRowCount(0);
        for (SystemLogEntry systemLogEntry : list) {
            this.systemLogModel.addRow(new Object[]{systemLogEntry.time, systemLogEntry.type, systemLogEntry.level, systemLogEntry.groupId, systemLogEntry.description, systemLogEntry.source});
        }
        if (this.systemLogTable.getRowCount() > 0 && this.systemLogTable.getSelectedRow() < 0) {
            this.systemLogTable.setRowSelectionInterval(0, 0);
        }
        this.updateSystemLogDetail();
        this.refreshSystemHealthStatus();
    }

    private static List<String> tailLines(Path path, int index) throws Exception {
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            String text;
            while ((text = bufferedReader.readLine()) != null) {
                arrayDeque.addLast(text);
                while (arrayDeque.size() > index) {
                    arrayDeque.removeFirst();
                }
            }
        }
        return new ArrayList<String>(arrayDeque);
    }

    private String eventTypeText(String text) {
        if ("ALERT_OPEN".equals(text)) {
            return "\u62a5\u8b66\u89e6\u53d1";
        }
        if ("ACKNOWLEDGED".equals(text)) {
            return "\u5df2\u5173\u6ce8";
        }
        if ("RECOVERED".equals(text)) {
            return "\u6062\u590d";
        }
        if ("QUERY_FAILED".equals(text)) {
            return "\u67e5\u8be2\u5931\u8d25";
        }
        if ("QUERY_RECOVERED".equals(text)) {
            return "\u67e5\u8be2\u6062\u590d";
        }
        return text == null || text.isBlank() ? "\u68c0\u6d4b\u5b8c\u6210" : text;
    }

    private void updateSystemLogDetail() {
        int index = this.systemLogTable.getSelectedRow();
        if (index < 0) {
            this.systemLogDetailArea.setText("\u6682\u65e0\u65e5\u5fd7\u8bb0\u5f55\u3002");
            return;
        }
        int index2 = this.systemLogTable.convertRowIndexToModel(index);
        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < this.systemLogModel.getColumnCount(); ++i) {
            messageBuilder.append(this.systemLogModel.getColumnName(i)).append("\uff1a").append(this.systemLogModel.getValueAt(index2, i)).append(System.lineSeparator());
        }
        this.systemLogDetailArea.setText(messageBuilder.toString());
    }

    private static List<String> parseCsvLine(String text) {
        ArrayList<String> arrayList = new ArrayList<String>();
        StringBuilder messageBuilder = new StringBuilder();
        boolean flag = false;
        for (int i = 0; i < text.length(); ++i) {
            char c = text.charAt(i);
            if (c == '\"') {
                if (flag && i + 1 < text.length() && text.charAt(i + 1) == '\"') {
                    messageBuilder.append('\"');
                    ++i;
                    continue;
                }
                flag = !flag;
                continue;
            }
            if (c == ',' && !flag) {
                arrayList.add(messageBuilder.toString());
                messageBuilder.setLength(0);
                continue;
            }
            messageBuilder.append(c);
        }
        arrayList.add(messageBuilder.toString());
        return arrayList;
    }

    static String sanitizeVisibleLog(String text) {
        return SensitiveTextSanitizer.sanitize(text);
    }

    private static String sanitizeSensitiveText(String text) {
        return SensitiveTextSanitizer.sanitize(text);
    }

    private void executeSelfTestFromUi() throws Exception {
        ShelfPointMonitorApp.runSelfTest(ShelfPointMonitorApp.resolveSelfTestAppRoot());
        this.latestSelfTestHealth = "\u901a\u8fc7";
        this.refreshSystemHealthStatus();
        SwingUtilities.invokeLater(() -> this.appendStatus("\u81ea\u68c0\u901a\u8fc7\u3002"));
    }

    private void exportDiagnostics() {
        try {
            Path path = DiagnosticBundleService.create(
                    Path.of(""), Paths.get("diagnostics"), EXPECTED_SELF_TEST_VERSION);
            this.appendStatus("\u8bca\u65ad\u4fe1\u606f\u5df2\u5bfc\u51fa\uff1a" + String.valueOf(path.getFileName()));
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void refreshSystemHealthStatus() {
        this.refreshSystemHealthStatus(() -> true);
    }

    private void refreshSystemHealthStatus(BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        this.runIoInBackground(() -> this.refreshSystemHealthStatusOnIo(publicationAllowed));
    }

    private void refreshSystemHealthStatusOnIo(BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        String text = this.scheduledTask == null || this.scheduledTask.isCancelled() ? "\u672a\u8fd0\u884c" : "\u8fd0\u884c\u4e2d";
        String text2 = this.currentProfile == null ? "\u672a\u9009\u62e9\u8fde\u63a5" : this.currentConnectionHealth;
        String text3 = ShelfPointMonitorApp.configFileHealthStatus(Paths.get("data", "config.properties")) + " / " + ShelfPointMonitorApp.configFileHealthStatus(Paths.get("data", "group-config.properties"));
        Path path = this.logPath.getParent();
        String text4 = ShelfPointMonitorApp.logDirectoryHealthStatus(path);
        String text5 = this.latestDetectionHealth;
        String text6 = this.latestSelfTestHealth;
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            this.schedulerHealthLabel.setText("\u76d1\u63a7\u8c03\u5ea6\u5668\uff1a" + text);
            this.connectionHealthLabel.setText("\u5f53\u524d\u8fde\u63a5\uff1a" + text2);
            this.detectionHealthLabel.setText("\u6700\u8fd1\u4e00\u6b21\u68c0\u6d4b\uff1a" + text5);
            this.configHealthLabel.setText("\u914d\u7f6e\u6587\u4ef6\uff1a" + ("\u6b63\u5e38 / \u6b63\u5e38".equals(text3) ? "\u6b63\u5e38" : text3));
            this.logDirHealthLabel.setText("\u65e5\u5fd7\u76ee\u5f55\uff1a" + text4);
            this.selfTestHealthLabel.setText("\u81ea\u68c0\u72b6\u6001\uff1a" + text6);
        });
    }

    static String configFileHealthStatus(Path path) {
        if (path == null || !Files.exists(path)) {
            return "\u7f3a\u5931";
        }
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            return "\u8bfb\u53d6\u5931\u8d25";
        }
        return "\u6b63\u5e38";
    }

    static String logDirectoryHealthStatus(Path path) {
        if (path == null || !Files.exists(path)) {
            return "\u7f3a\u5931";
        }
        if (!Files.isDirectory(path) || !Files.isWritable(path)) {
            return "\u4e0d\u53ef\u5199";
        }
        return "\u53ef\u5199";
    }

    private void loadSettingsForm() {
        this.settingsDefaultPageBox.setSelectedItem(this.uiPreferences.defaultPage());
        this.settingsOverviewRefreshSpinner.setValue(this.uiPreferences.overviewRefreshSeconds());
        this.settingsAlertPopupBox.setSelected(this.uiPreferences.alertPopupEnabled());
        this.settingsAlertSoundBox.setSelected(this.uiPreferences.alertSoundEnabled());
        this.settingsLogRetentionSpinner.setValue(this.uiPreferences.logRetentionDays());
        this.settingsDensityBox.setSelectedItem(this.uiPreferences.density());
        this.settingsStartupSelfTestBox.setSelected(this.uiPreferences.startupSelfTestEnabled());
        this.settingsAutoCleanupLogsBox.setSelected(this.uiPreferences.autoCleanupLogsEnabled());
    }

    private UiPreferences readSettingsForm() {
        return new UiPreferences(String.valueOf(this.settingsDefaultPageBox.getSelectedItem()), (Integer)this.settingsOverviewRefreshSpinner.getValue(), this.settingsAlertPopupBox.isSelected(), this.settingsAlertSoundBox.isSelected(), (Integer)this.settingsLogRetentionSpinner.getValue(), String.valueOf(this.settingsDensityBox.getSelectedItem()), this.settingsStartupSelfTestBox.isSelected(), this.settingsAutoCleanupLogsBox.isSelected());
    }

    private void saveUiPreferences() {
        try {
            this.uiPreferences = this.readSettingsForm();
            this.uiPreferencesStore.save(this.uiPreferences);
            this.applyUiPreferences();
            this.appendStatus("\u7cfb\u7edf\u8bbe\u7f6e\u5df2\u4fdd\u5b58\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void restoreUiPreferences() {
        try {
            this.uiPreferencesStore.restoreDefaults();
            this.uiPreferences = this.uiPreferencesStore.load();
            this.loadSettingsForm();
            this.applyUiPreferences();
            this.appendStatus("\u7cfb\u7edf\u8bbe\u7f6e\u5df2\u6062\u590d\u9ed8\u8ba4\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void reloadUiPreferences() {
        this.uiPreferences = this.uiPreferencesStore.load();
        this.loadSettingsForm();
        this.applyUiPreferences();
        this.appendStatus("\u7cfb\u7edf\u8bbe\u7f6e\u5df2\u91cd\u65b0\u52a0\u8f7d\u3002");
    }

    private void applyUiPreferences() {
        this.applyDensity();
        this.restartOverviewRefreshTimer();
        if (this.uiPreferences.autoCleanupLogsEnabled()) {
            this.runOnceInBackground(this::cleanupOldLogs);
        }
    }

    private void applyDensity() {
        int index = "\u7d27\u51d1".equals(this.uiPreferences.density()) ? 22 : ("\u8212\u9002".equals(this.uiPreferences.density()) ? 32 : 26);
        for (JTable jTable : List.of(this.overviewTable, this.alertCenterTable, this.dataQueryTable, this.browserTable, this.columnTable, this.previewTable, this.groupPointTable, this.systemLogTable)) {
            jTable.setRowHeight(index);
        }
    }

    private void restartOverviewRefreshTimer() {
        if (this.overviewRefreshTimer != null) {
            this.overviewRefreshTimer.stop();
        }
        this.overviewRefreshTimer = new Timer(this.uiPreferences.overviewRefreshSeconds() * 1000, actionEvent -> this.refreshOverviewPage());
        this.overviewRefreshTimer.start();
    }

    private void cleanupOldLogs() {
        try {
            Path path2 = Paths.get("logs");
            if (!Files.isDirectory(path2)) {
                return;
            }
            long l = System.currentTimeMillis() - (long)this.uiPreferences.logRetentionDays() * 24L * 60L * 60L * 1000L;
            try (Stream<Path> stream = Files.list(path2);){
                stream.forEach(path -> {
                    try {
                        if (Files.isRegularFile(path) && Files.getLastModifiedTime(path).toMillis() < l) {
                            Files.deleteIfExists(path);
                        }
                    }
                    catch (Exception exception) {
                        this.reportNonLoggingFailure("日志清理失败", exception, () -> true);
                    }
                });
            }
        }
        catch (Exception exception) {
            this.appendStatus("\u65e5\u5fd7\u6e05\u7406\u5931\u8d25\uff1a" + ShelfPointMonitorApp.sanitizedExceptionSummary(exception));
        }
    }

    private void selectDefaultPage() {
        this.navigationList.setSelectedValue(this.uiPreferences.defaultPage(), true);
        this.cardLayout.show(this.cardPanel, this.uiPreferences.defaultPage());
    }

    private void loadPointConfig() {
        ConfigStore.StoredConfig storedConfig = this.configStore.load();
        this.intervalSpinner.setValue(storedConfig.intervalSeconds);
        this.pointModel.setRowCount(0);
        for (PointDefinition pointDefinition : storedConfig.points) {
            this.pointModel.addRow(new Object[]{pointDefinition.alias(), pointDefinition.code(), pointDefinition.intervalMinutes()});
        }
    }

    private List<PointDefinition> readPoints() {
        ArrayList<PointDefinition> arrayList = new ArrayList<PointDefinition>();
        for (int i = 0; i < this.pointModel.getRowCount(); ++i) {
            String text = this.cellText(i, 0);
            String text2 = this.cellText(i, 1);
            String text3 = this.cellText(i, 2);
            if (text.isEmpty() && text2.isEmpty()) continue;
            if (text2.isEmpty() || text.isEmpty()) {
                throw new IllegalArgumentException("\u70b9\u4f4d\u522b\u540d\u548c\u7f16\u7801\u5fc5\u987b\u540c\u65f6\u586b\u5199");
            }
            arrayList.add(new PointDefinition(text2, text, this.parseIntervalMinutes(text3)));
        }
        if (arrayList.isEmpty()) {
            throw new IllegalArgumentException("\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u70b9\u4f4d");
        }
        return arrayList;
    }

    private String cellText(int index, int index2) {
        Object value = this.pointModel.getValueAt(index, index2);
        return value == null ? "" : String.valueOf(value).trim();
    }

    private int parseIntervalMinutes(String text) {
        if (text == null || text.isBlank()) {
            return 5;
        }
        try {
            return Integer.parseInt(text.trim());
        }
        catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("\u76d1\u6d4b\u5468\u671f\u5fc5\u987b\u662f\u6574\u6570\u5206\u949f");
        }
    }

    private void removeSelectedPointRows() {
        int[] nArray = this.pointTable.getSelectedRows();
        for (int i = nArray.length - 1; i >= 0; --i) {
            this.pointModel.removeRow(this.pointTable.convertRowIndexToModel(nArray[i]));
        }
    }

    private void savePointConfig() {
        try {
            this.configStore.save(this.requireCurrentConfig((Integer)this.intervalSpinner.getValue()), this.readPoints());
            this.appendStatus("\u70b9\u4f4d\u914d\u7f6e\u5df2\u4fdd\u5b58\u3002\u5bc6\u7801\u672a\u4fdd\u5b58\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void checkNowLegacy() throws Exception {
        DbConfig dbConfig = this.requireCurrentConfig((Integer)this.intervalSpinner.getValue());
        List<PointDefinition> points = this.pointSchedule.forceAll(this.readPoints());
        MonitoringSession session = this.createOneShotSession(dbConfig);
        this.registerOneShotSession(session);
        this.runMonitorInBackground(() -> {
            try {
                this.checkPointsLegacy(
                        session,
                        points,
                        false,
                        LocalDateTime.now(),
                        "\u624b\u52a8\u68c0\u6d4b",
                        () -> this.isCurrentOneShotSession(session));
            }
            finally {
                session.clearPassword();
                this.unregisterOneShotSession(session);
            }
        }, () -> this.isCurrentOneShotSession(session));
    }

    private void checkDuePointsLegacy(MonitoringSession session) throws Exception {
        if (!this.isCurrentMonitoringSession(session)) {
            return;
        }
        LocalDateTime checkedAt = LocalDateTime.now();
        List<PointDefinition> duePoints = this.pointSchedule.duePoints(this.monitoredLegacyPoints, checkedAt);
        if (duePoints.isEmpty() || !this.isCurrentMonitoringSession(session)) {
            return;
        }
        this.checkPointsLegacy(
                session,
                duePoints,
                true,
                checkedAt,
                "\u81ea\u52a8\u68c0\u6d4b",
                () -> this.isCurrentMonitoringSession(session));
    }

    private void checkPointsLegacy(
            MonitoringSession session,
            List<PointDefinition> points,
            boolean markSchedule,
            LocalDateTime checkedAt,
            String source,
            BooleanSupplier publicationAllowed) throws Exception {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        char[] taskPassword = session.copyPasswordForTask();
        try {
            List<PointRecord> pointRecords = this.pointRepository.fetch(session.config(), taskPassword, points);
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            MonitorEvaluation monitorEvaluation;
            synchronized (this.groupMonitorLock) {
                if (!publicationAllowed.getAsBoolean()) {
                    return;
                }
                monitorEvaluation = MonitorLogic.evaluate(points, pointRecords, this.alertState);
                if (markSchedule) {
                    this.pointSchedule.markChecked(points, checkedAt);
                }
            }
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            this.appendStatus(this.formatCheckResultLegacy(points, pointRecords, monitorEvaluation, source), publicationAllowed);
            if (monitorEvaluation.hasActiveAlert()) {
                this.showAlertDialog(monitorEvaluation, publicationAllowed);
            }
        }
        finally {
            MonitoringSession.clearTaskPassword(taskPassword);
        }
    }

    private String formatCheckResultLegacy(List<PointDefinition> list, List<PointRecord> list2, MonitorEvaluation monitorEvaluation, String text) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append(text).append("\u5b8c\u6210\uff0c\u68c0\u67e5\u70b9\u4f4d ").append(list.size()).append(" \u4e2a\uff0c\u8fd4\u56de\u8bb0\u5f55 ").append(list2.size()).append(" \u6761");
        if (monitorEvaluation.hasActiveAlert()) {
            messageBuilder.append("\uff0c\u53d1\u73b0\u5f02\u5e38\uff1a");
            for (PointAlert value : monitorEvaluation.alerts()) {
                messageBuilder.append(" ").append(value.alias()).append("(").append(value.code()).append(") ").append(value.message()).append(";");
            }
        } else if (monitorEvaluation.suppressedByAck()) {
            messageBuilder.append("\uff0c\u5f02\u5e38\u5df2\u5173\u6ce8\uff0c\u672c\u8f6e\u4e0d\u91cd\u590d\u5f39\u7a97");
        } else {
            messageBuilder.append("\uff0c\u65e0\u62a5\u8b66");
        }
        for (PointRecord pointRecord : list2) {
            messageBuilder.append(System.lineSeparator()).append("  ").append(pointRecord.mapDataCode()).append(" shelf_code=").append(pointRecord.podCode()).append(" status=").append(pointRecord.status()).append(" lock_state=").append(pointRecord.indLock()).append(" updated_at=").append(pointRecord.dateChg());
        }
        return messageBuilder.toString();
    }

    private void startMonitoringLegacy() {
        try {
            DbConfig dbConfig = this.requireCurrentConfig((Integer)this.intervalSpinner.getValue());
            List<PointDefinition> points = this.readPoints();
            this.configStore.save(dbConfig, points);
            this.stopMonitoring();
            MonitoringSession session = this.createMonitoringSession(dbConfig);
            this.monitoringSession = session;
            this.monitoredLegacyPoints = List.copyOf(points);
            this.pointSchedule.clear();
            this.scheduledTask = this.monitorExecutor.scheduleWithFixedDelay(
                    () -> this.runWithUiErrorHandling(
                            () -> this.checkDuePointsLegacy(session),
                            () -> this.isCurrentMonitoringSession(session)),
                    0L,
                    dbConfig.intervalSeconds(),
                    TimeUnit.SECONDS);
            this.startButton.setEnabled(false);
            this.stopButton.setEnabled(true);
            this.appendStatus("\u5df2\u5f00\u59cb\u76d1\u63a7\u3002\u5168\u5c40\u626b\u63cf\u95f4\u9694 " + dbConfig.intervalSeconds() + " \u79d2\uff1b\u70b9\u4f4d\u6309\u5404\u81ea\u5206\u949f\u5468\u671f\u5230\u671f\u67e5\u8be2\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void stopMonitoringLegacy() {
        this.stopMonitoring();
        this.pointSchedule.clear();
    }

    private DbConfig requireCurrentConfig(int index) {
        if (this.currentProfile == null) {
            throw new IllegalStateException("\u8bf7\u5148\u5728\u201c\u8fde\u63a5\u7ba1\u7406\u201d\u4e2d\u6d4b\u8bd5\u5e76\u4f7f\u7528\u4e00\u4e2a\u8fde\u63a5");
        }
        return this.currentProfile.toDbConfig(index);
    }

    private void showAlertDialog(MonitorEvaluation monitorEvaluation, BooleanSupplier publicationAllowed) {
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()
                    || this.activeDialog != null && this.activeDialog.isShowing()) {
                return;
            }
            JDialog jDialog = new JDialog(this, "\u70b9\u4f4d\u8d27\u67b6\u5f02\u5e38", false);
            long dialogGeneration = this.monitoringGeneration;
            jDialog.setAlwaysOnTop(true);
            jDialog.setDefaultCloseOperation(0);
            jDialog.setLayout(new BorderLayout(12, 12));
            jDialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            JTextArea jTextArea = new JTextArea(this.alertText(monitorEvaluation));
            jTextArea.setEditable(false);
            jTextArea.setLineWrap(true);
            jTextArea.setWrapStyleWord(true);
            jTextArea.setFont(new Font("SansSerif", 0, 15));
            jTextArea.setBackground(new Color(255, 250, 240));
            jDialog.add((Component)jTextArea, "Center");
            JButton button = new JButton("\u5df2\u5173\u6ce8");
            button.addActionListener(actionEvent -> {
                if (this.isCurrentActiveDialog(jDialog, null, dialogGeneration)
                        && publicationAllowed.getAsBoolean()) {
                    this.alertState.acknowledge(monitorEvaluation.alertKey());
                    this.appendStatus("\u7528\u6237\u5df2\u5173\u6ce8\u62a5\u8b66\uff1a" + monitorEvaluation.alertKey(),
                            publicationAllowed);
                }
                jDialog.dispose();
                this.clearActiveDialogIfOwnedBy(jDialog);
            });
            JPanel panel = new JPanel(new FlowLayout(2));
            panel.add(button);
            jDialog.add((Component)panel, "South");
            jDialog.setSize(520, 320);
            jDialog.setLocationRelativeTo(this);
            this.activeDialog = jDialog;
            this.activeDialogGroupId = "";
            this.activeDialogGeneration = dialogGeneration;
            jDialog.setVisible(true);
        });
    }

    private String alertText(MonitorEvaluation monitorEvaluation) {
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("\u68c0\u6d4b\u65f6\u95f4\uff1a").append(TIME_FORMAT.format(LocalDateTime.now())).append(System.lineSeparator()).append(System.lineSeparator());
        for (PointAlert pointAlert : monitorEvaluation.alerts()) {
            messageBuilder.append(pointAlert.alias()).append("\uff1a").append(pointAlert.code()).append(System.lineSeparator()).append("\u72b6\u6001\uff1a").append(pointAlert.message()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        messageBuilder.append("\u8bf7\u73b0\u573a\u786e\u8ba4\u540e\u70b9\u51fb\u201c\u5df2\u5173\u6ce8\u201d\u3002\u5728\u70b9\u4f4d\u6062\u590d\u6b63\u5e38\u524d\uff0c\u672c\u6b21\u76f8\u540c\u62a5\u8b66\u4e0d\u4f1a\u91cd\u590d\u5f39\u51fa\u3002");
        return messageBuilder.toString();
    }

    private void runOnceInBackground(CheckedRunnable checkedRunnable) {
        this.runIoInBackground(checkedRunnable);
    }

    void runMonitorInBackground(CheckedRunnable checkedRunnable) {
        this.runMonitorInBackground(checkedRunnable, () -> true);
    }

    private void runMonitorInBackground(CheckedRunnable checkedRunnable, BooleanSupplier publicationAllowed) {
        this.submitToExecutor(this.monitorExecutor, checkedRunnable, publicationAllowed);
    }

    void runIoInBackground(CheckedRunnable checkedRunnable) {
        this.submitToExecutor(this.ioExecutor, checkedRunnable, () -> true);
    }

    private void submitToExecutor(ScheduledExecutorService executorService, CheckedRunnable checkedRunnable) {
        this.submitToExecutor(executorService, checkedRunnable, () -> true);
    }

    private void submitToExecutor(
            ScheduledExecutorService executorService,
            CheckedRunnable checkedRunnable,
            BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        try {
            executorService.submit(() -> this.runWithUiErrorHandling(checkedRunnable, publicationAllowed));
        }
        catch (RejectedExecutionException exception) {
            if (!executorService.isShutdown()) {
                this.reportNonLoggingFailure("后台任务提交失败", exception, publicationAllowed);
            }
        }
    }

    private void runWithUiErrorHandling(CheckedRunnable checkedRunnable) {
        this.runWithUiErrorHandling(checkedRunnable, () -> true);
    }

    private void runWithUiErrorHandling(CheckedRunnable checkedRunnable, BooleanSupplier publicationAllowed) {
        try {
            checkedRunnable.run();
        }
        catch (Exception exception) {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            String summary = ShelfPointMonitorApp.userVisibleErrorMessage(exception);
            SwingUtilities.invokeLater(() -> {
                if (publicationAllowed.getAsBoolean()) {
                    this.showErrorSummary(summary);
                }
            });
            this.appendStatus("\u6267\u884c\u5931\u8d25\uff1a" + summary, publicationAllowed);
        }
    }

    private void showError(Exception exception) {
        String summary = ShelfPointMonitorApp.userVisibleErrorMessage(exception);
        this.appendStatus("\u9519\u8bef\uff1a" + summary);
        this.showErrorSummary(summary);
    }

    private void showErrorSummary(String summary) {
        String sanitizedSummary = ShelfPointMonitorApp.sanitizeVisibleLog(summary);
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> this.showErrorSummary(sanitizedSummary));
            return;
        }
        JOptionPane.showMessageDialog(this, sanitizedSummary, sanitizedSummary, 0);
    }

    private void appendStatus(String text) {
        this.appendStatus(text, () -> true);
    }

    private void appendStatus(String text, BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        String timestampedText = "[" + TIME_FORMAT.format(LocalDateTime.now()) + "] " + text;
        String sanitizedText = ShelfPointMonitorApp.sanitizeVisibleLog(timestampedText);
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            this.bottomStatusLabel.setText(ShelfPointMonitorApp.sanitizeVisibleLog(text));
            this.statusArea.append(sanitizedText + System.lineSeparator());
            this.statusArea.setCaretPosition(this.statusArea.getDocument().getLength());
        });
        this.enqueueIoOperation(publicationAllowed, "日志写入失败", () -> this.writeLog(sanitizedText));
    }

    private void enqueueIoOperation(
            BooleanSupplier publicationAllowed,
            String failurePrefix,
            CheckedRunnable checkedRunnable) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        try {
            this.ioExecutor.submit(() -> {
                if (!publicationAllowed.getAsBoolean()) {
                    return;
                }
                try {
                    checkedRunnable.run();
                }
                catch (Exception exception) {
                    this.reportNonLoggingFailure(failurePrefix, exception, publicationAllowed);
                }
            });
        }
        catch (Exception exception) {
            if (!this.ioExecutor.isShutdown()) {
                this.reportNonLoggingFailure(failurePrefix, exception, publicationAllowed);
            }
        }
    }

    private void reportNonLoggingFailure(
            String failurePrefix,
            Exception exception,
            BooleanSupplier publicationAllowed) {
        if (!publicationAllowed.getAsBoolean()) {
            return;
        }
        String visibleMessage = failurePrefix + "：" + ShelfPointMonitorApp.userVisibleErrorMessage(exception);
        SwingUtilities.invokeLater(() -> {
            if (!publicationAllowed.getAsBoolean()) {
                return;
            }
            this.bottomStatusLabel.setText(visibleMessage);
            this.statusArea.append("[" + TIME_FORMAT.format(LocalDateTime.now()) + "] " + visibleMessage
                    + System.lineSeparator());
            this.statusArea.setCaretPosition(this.statusArea.getDocument().getLength());
        });
    }

    private void writeLog(String text) throws Exception {
        Files.createDirectories(this.logPath.getParent());
        Files.writeString(
                this.logPath,
                text + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    private void openLogs() {
        try {
            Files.createDirectories(this.logPath.getParent());
            Desktop.getDesktop().open(this.logPath.getParent().toFile());
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception exception) {
            System.err.println("ShelfPointMonitor LOOK_AND_FEEL_FAILED: "
                    + ShelfPointMonitorApp.userVisibleErrorMessage(exception));
        }
        AppTheme.install();
    }

    static interface CheckedRunnable {
        public void run() throws Exception;
    }

    static final class MonitoringSession {
        private final DbConfig config;
        private final char[] passwordSnapshot;
        private final long generation;
        private boolean closed;

        MonitoringSession(DbConfig config, char[] passwordSnapshot, long generation) {
            if (config == null) {
                throw new IllegalArgumentException("config is required");
            }
            this.config = config;
            this.passwordSnapshot = passwordSnapshot == null ? new char[0] : Arrays.copyOf(passwordSnapshot, passwordSnapshot.length);
            this.generation = generation;
        }

        DbConfig config() {
            return this.config;
        }

        synchronized char[] copyPasswordForTask() {
            if (this.closed) {
                return new char[0];
            }
            return Arrays.copyOf(this.passwordSnapshot, this.passwordSnapshot.length);
        }

        long generation() {
            return this.generation;
        }

        synchronized boolean isClosed() {
            return this.closed;
        }

        synchronized void clearPassword() {
            MonitoringSession.clearTaskPassword(this.passwordSnapshot);
        }

        synchronized void close() {
            this.closed = true;
            this.clearPassword();
        }

        static void clearTaskPassword(char[] password) {
            if (password != null) {
                Arrays.fill(password, '\u0000');
            }
        }
    }

    @FunctionalInterface
    static interface GroupPointFetcher {
        public List<PointRecord> fetch(PointGroupDefinition var1) throws Exception;
    }

    static final class GroupCheckRunResult {
        private final int checkedGroups;
        private final int failedGroups;
        private final boolean dialogRequested;
        private final List<GroupEvaluation> evaluations;

        GroupCheckRunResult(int index, int index2, boolean flag, List<GroupEvaluation> list) {
            this.checkedGroups = index;
            this.failedGroups = index2;
            this.dialogRequested = flag;
            this.evaluations = List.copyOf(list);
        }

        int checkedGroups() {
            return this.checkedGroups;
        }

        int failedGroups() {
            return this.failedGroups;
        }

        boolean dialogRequested() {
            return this.dialogRequested;
        }

        List<GroupEvaluation> evaluations() {
            return this.evaluations;
        }
    }

    private static final class AlertCenterEntry {
        private final String groupId;
        private final String areaName;
        private final String groupName;
        private final String eventType;
        private final GroupAlertStatus status;
        private final String occurredAt;
        private final String source;
        private final String description;
        private final GroupEvaluation liveEvaluation;

        AlertCenterEntry(String text, String text2, String text3, String text4, GroupAlertStatus groupAlertStatus, String text5, String text6, String text7, GroupEvaluation groupEvaluation) {
            this.groupId = text;
            this.areaName = text2;
            this.groupName = text3;
            this.eventType = text4;
            this.status = groupAlertStatus;
            this.occurredAt = text5;
            this.source = text6;
            this.description = text7;
            this.liveEvaluation = groupEvaluation;
        }
    }

    private static final class SystemLogFilter {
        private final String type;
        private final String from;
        private final String to;
        private final String groupId;
        private final String keyword;

        SystemLogFilter(String text, String text2, String text3, String text4, String text5) {
            this.type = text;
            this.from = text2;
            this.to = text3;
            this.groupId = text4;
            this.keyword = text5;
        }
    }

    private static final class SystemLogEntry {
        private final String time;
        private final String type;
        private final String level;
        private final String groupId;
        private final String description;
        private final String source;

        SystemLogEntry(String text, String text2, String text3, String text4, String text5, String text6) {
            this.time = text;
            this.type = text2;
            this.level = text3;
            this.groupId = text4;
            this.description = text5;
            this.source = text6;
        }
    }
}
