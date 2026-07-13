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
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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
    private final GroupLogWriter groupLogWriter = new GroupLogWriter(Paths.get("logs", new String[0]));
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "shelf-point-monitor-bg");
        thread.setDaemon(true);
        return thread;
    });
    private final Path logPath = Paths.get("logs", "monitor.log");
    private UiPreferences uiPreferences = this.uiPreferencesStore.load();
    private final JList<String> navigationList = new JList<String>(UiPreferences.pageNames().toArray(new String[0]));
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(this.cardLayout);
    private final JLabel currentConnectionLabel = new JLabel("\u5f53\u524d\u8fde\u63a5\uff1a\u672a\u8fde\u63a5");
    private final JLabel monitorStatusLabel = new JLabel("\u76d1\u63a7\u72b6\u6001\uff1a\u672a\u8fd0\u884c");
    private final JLabel lastCheckLabel = new JLabel("\u4e0a\u6b21\u68c0\u6d4b\uff1a--");
    private final JLabel nextCheckLabel = new JLabel("\u4e0b\u6b21\u68c0\u6d4b\uff1a--");
    private final JLabel bottomStatusLabel = new JLabel("\u5c31\u7eea");
    private final DefaultListModel<String> profileListModel = new DefaultListModel();
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
    private final DefaultComboBoxModel<String> schemaModel = new DefaultComboBoxModel();
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
        public boolean isCellEditable(int n, int n2) {
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
        public boolean isCellEditable(int n, int n2) {
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
        public boolean isCellEditable(int n, int n2) {
            return false;
        }
    };
    private final JTable dataQueryTable = new JTable(this.dataQueryModel);
    private final JTextArea dataQueryDetailArea = new JTextArea();
    private final DefaultTableModel systemLogModel = new DefaultTableModel(new Object[]{"\u65f6\u95f4", "\u4e8b\u4ef6\u7c7b\u578b", "\u7ea7\u522b", "\u70b9\u4f4d\u7ec4", "\u63cf\u8ff0", "\u6765\u6e90"}, 0){

        @Override
        public boolean isCellEditable(int n, int n2) {
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
    private String latestDetectionHealth = "\u672a\u68c0\u6d4b";
    private String latestSelfTestHealth = "\u672a\u6267\u884c";
    private final JComboBox<String> settingsDefaultPageBox = new JComboBox<String>(UiPreferences.pageNames().toArray(new String[0]));
    private final JSpinner settingsOverviewRefreshSpinner = new JSpinner(new SpinnerNumberModel(10, 5, 3600, 5));
    private final JCheckBox settingsAlertPopupBox = new JCheckBox("\u62a5\u8b66\u5f39\u7a97\u542f\u7528");
    private final JCheckBox settingsAlertSoundBox = new JCheckBox("\u62a5\u8b66\u58f0\u97f3\u63d0\u793a");
    private final JSpinner settingsLogRetentionSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 3650, 1));
    private final JComboBox<String> settingsDensityBox = new JComboBox<String>(UiPreferences.densities().toArray(new String[0]));
    private final JCheckBox settingsStartupSelfTestBox = new JCheckBox("\u542f\u52a8\u65f6\u6267\u884c\u81ea\u68c0");
    private final JCheckBox settingsAutoCleanupLogsBox = new JCheckBox("\u65e5\u5fd7\u81ea\u52a8\u6e05\u7406");
    private final JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(10, 10, 86400, 10));
    private final DefaultTableModel pointModel = new DefaultTableModel(new Object[]{"\u522b\u540d", "\u70b9\u4f4d\u7f16\u7801", "\u76d1\u6d4b\u5468\u671f(\u5206\u949f)"}, 0){

        @Override
        public Class<?> getColumnClass(int n) {
            return n == 2 ? Integer.class : String.class;
        }
    };
    private final JTable pointTable = new JTable(this.pointModel);
    private final JButton startButton = new JButton("\u5f00\u59cb\u76d1\u63a7");
    private final JButton stopButton = new JButton("\u505c\u6b62");
    private final JButton checkButton = new JButton("\u7acb\u5373\u68c0\u6d4b");
    private final JTextArea statusArea = new JTextArea();
    private final DefaultListModel<String> groupListModel = new DefaultListModel();
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
        public Class<?> getColumnClass(int n) {
            return n == 3 ? Boolean.class : String.class;
        }
    };
    private final JTable groupPointTable = new JTable(this.groupPointModel);
    private final JPanel pointStatusPanel = new JPanel(new GridBagLayout());
    private final JLabel groupSummaryLabel = new JLabel("\u5f53\u524d\u5224\u65ad\uff1a\u672a\u68c0\u6d4b");
    private final JTextArea groupRuntimeArea = new JTextArea();
    private List<ConnectionProfile> profiles = new ArrayList<ConnectionProfile>();
    private List<PointGroupDefinition> pointGroups = new ArrayList<PointGroupDefinition>();
    private volatile List<PointGroupDefinition> monitoredGroups = List.of();
    private final Object groupMonitorLock = new Object();
    private final Map<String, GroupRuntimeState> groupStates = new LinkedHashMap<String, GroupRuntimeState>();
    private final Map<String, GroupAlertStatus> lastGroupStatuses = new LinkedHashMap<String, GroupAlertStatus>();
    private final Map<String, GroupEvaluation> lastGroupEvaluations = new LinkedHashMap<String, GroupEvaluation>();
    private ConnectionProfile currentProfile;
    private char[] currentPassword = new char[0];
    private ScheduledFuture<?> scheduledTask;
    private Timer overviewRefreshTimer;
    private JDialog activeDialog;
    private String activeDialogGroupId = "";

    public static void main(String[] stringArray) {
        if (stringArray.length > 0 && "--self-test".equals(stringArray[0])) {
            try {
                ShelfPointMonitorApp.runSelfTest(ShelfPointMonitorApp.resolveSelfTestAppRoot());
                System.out.println("ShelfPointMonitor SELF_TEST_OK");
            }
            catch (Exception exception) {
                System.err.println("ShelfPointMonitor SELF_TEST_FAILED: " + exception.getMessage());
                exception.printStackTrace(System.err);
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
        this.setDefaultCloseOperation(3);
        this.setMinimumSize(new Dimension(1180, 760));
        this.addWindowListener(new WindowAdapter(){

            @Override
            public void windowClosing(WindowEvent windowEvent) {
                ShelfPointMonitorApp.this.executor.shutdownNow();
                if (ShelfPointMonitorApp.this.overviewRefreshTimer != null) {
                    ShelfPointMonitorApp.this.overviewRefreshTimer.stop();
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
            this.runOnceInBackground(() -> {
                ShelfPointMonitorApp.runSelfTest(ShelfPointMonitorApp.resolveSelfTestAppRoot());
                SwingUtilities.invokeLater(() -> this.appendStatus("\u542f\u52a8\u81ea\u68c0\u901a\u8fc7\u3002"));
            });
        }
    }

    @Override
    public void dispose() {
        this.executor.shutdownNow();
        if (this.overviewRefreshTimer != null) {
            this.overviewRefreshTimer.stop();
        }
        super.dispose();
    }

    static void runSelfTestForTest(Path path) throws Exception {
        ShelfPointMonitorApp.runSelfTest(path);
    }

    private static Path resolveSelfTestAppRoot() throws Exception {
        String string = System.getProperty("shelf.monitor.appRoot");
        if (string != null && !string.isBlank()) {
            return Path.of(string, new String[0]).toAbsolutePath().normalize();
        }
        Path path = Path.of(ShelfPointMonitorApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isRegularFile(path, new LinkOption[0])) {
            return path.getParent().toAbsolutePath().normalize();
        }
        return Path.of("", new String[0]).toAbsolutePath().normalize();
    }

    private static void runSelfTest(Path path) throws Exception {
        Path path2 = path.toAbsolutePath().normalize();
        ShelfPointMonitorApp.requireFile(path2.resolve("ShelfPointMonitor.jar"), "packaged application jar");
        String string = Files.readString(ShelfPointMonitorApp.requireFile(path2.resolve("VERSION"), "VERSION"), StandardCharsets.UTF_8).trim();
        if (!EXPECTED_SELF_TEST_VERSION.equals(string)) {
            throw new IllegalStateException("VERSION must be 0.4.0, actual=" + string);
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
        boolean bl = false;
        boolean bl2 = false;
        for (ConnectionProfile object2 : storedProfiles.profiles()) {
            if ("__SITE_HOST__".equals(object2.host()) && "__SITE_USER__".equals(object2.user())) {
                bl = true;
            }
            if (!"h2".equals(object2.dbType())) continue;
            bl2 = true;
        }
        if (!bl || !bl2) {
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

    private static Path requireFile(Path path, String string) {
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            throw new IllegalStateException(string + " is missing: " + String.valueOf(path));
        }
        return path;
    }

    private static Path requireDirectory(Path path, String string) {
        if (!Files.isDirectory(path, new LinkOption[0])) {
            throw new IllegalStateException(string + " is missing: " + String.valueOf(path));
        }
        return path;
    }

    private static Properties loadSelfTestProperties(Path path) throws Exception {
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path, new OpenOption[0]);){
            properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        }
        return properties;
    }

    private static void assertNoPasswordProperty(Properties properties, String string) {
        for (String string2 : properties.stringPropertyNames()) {
            if (!string2.toLowerCase(Locale.ROOT).contains("password")) continue;
            throw new IllegalStateException(string + " must not contain password property: " + string2);
        }
    }

    private static void assertNoSensitiveProperties(Properties properties, String string) {
        for (String string2 : properties.stringPropertyNames()) {
            ShelfPointMonitorApp.assertNotSensitive(string + "." + string2, properties.getProperty(string2, ""));
        }
    }

    private static void assertNotSensitive(String string, String string2) {
        if (PRIVATE_10_NET_PATTERN.matcher(string2).find()) {
            throw new IllegalStateException(string + " contains private 10.x address");
        }
        if (REAL_POINT_CODE_PATTERN.matcher(string2).find()) {
            throw new IllegalStateException(string + " contains real-looking point code");
        }
    }

    private static void assertSamplePointDefinitions(List<PointDefinition> list, String string) {
        if (list.isEmpty()) {
            throw new IllegalStateException(string + " must not be empty");
        }
        for (PointDefinition pointDefinition : list) {
            ShelfPointMonitorApp.assertSamplePointCode(string, pointDefinition.code());
        }
    }

    private static void assertSampleGroupPoints(List<GroupMonitorPoint> list, String string) {
        if (list.isEmpty()) {
            throw new IllegalStateException(string + " must not be empty");
        }
        for (GroupMonitorPoint groupMonitorPoint : list) {
            ShelfPointMonitorApp.assertSamplePointCode(string, groupMonitorPoint.code());
        }
    }

    private static void assertSamplePointCode(String string, String string2) {
        if (!string2.startsWith("USE_POINT_") && !string2.startsWith("BACKUP_POINT_")) {
            throw new IllegalStateException(string + " must use sample point codes, actual=" + string2);
        }
        ShelfPointMonitorApp.assertNotSensitive(string, string2);
    }

    private void buildUi() {
        JPanel jPanel = new JPanel(new BorderLayout(0, 0));
        jPanel.setBackground(new Color(245, 247, 250));
        this.setContentPane(jPanel);
        jPanel.add((Component)this.buildTopStatusBar(), "North");
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
        JSplitPane jSplitPane = new JSplitPane(1, new JScrollPane(this.navigationList), this.cardPanel);
        jSplitPane.setDividerLocation(210);
        jSplitPane.setResizeWeight(0.0);
        jPanel.add((Component)jSplitPane, "Center");
        jPanel.add((Component)this.buildBottomStatusBar(), "South");
    }

    private JPanel buildTopStatusBar() {
        JPanel jPanel = new JPanel(new GridLayout(1, 4, 12, 0));
        jPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 225, 232)), BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        jPanel.setBackground(Color.WHITE);
        this.currentConnectionLabel.setFont(this.currentConnectionLabel.getFont().deriveFont(1));
        jPanel.add(this.currentConnectionLabel);
        jPanel.add(this.monitorStatusLabel);
        jPanel.add(this.lastCheckLabel);
        jPanel.add(this.nextCheckLabel);
        return jPanel;
    }

    private JPanel buildBottomStatusBar() {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setPreferredSize(new Dimension(100, 28));
        jPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 232)), BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        jPanel.setBackground(Color.WHITE);
        jPanel.add((Component)this.bottomStatusLabel, "West");
        jPanel.add((Component)new JLabel("\u7248\u672c\uff1a0.4.0"), "East");
        return jPanel;
    }

    private JPanel buildOverviewPage() {
        JPanel jPanel = new JPanel(new BorderLayout(12, 12));
        jPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        jPanel.setBackground(new Color(245, 247, 250));
        JPanel jPanel2 = new JPanel(new GridLayout(1, 4, 12, 0));
        jPanel2.setOpaque(false);
        jPanel2.add(this.statCard("\u76d1\u63a7\u70b9\u4f4d\u7ec4", this.overviewGroupCountLabel, new Color(38, 105, 210)));
        jPanel2.add(this.statCard("\u7f3a\u6599\u62a5\u8b66", this.overviewAlertCountLabel, new Color(190, 48, 48)));
        jPanel2.add(this.statCard("\u89c2\u5bdf\u4e2d", this.overviewPendingCountLabel, new Color(190, 120, 30)));
        jPanel2.add(this.statCard("\u6570\u636e\u5f02\u5e38", this.overviewDataErrorCountLabel, new Color(160, 70, 120)));
        jPanel.add((Component)jPanel2, "North");
        this.overviewTable.setSelectionMode(0);
        this.overviewTable.setRowHeight(28);
        this.overviewTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                this.updateOverviewDetail();
            }
        });
        this.overviewDetailArea.setEditable(false);
        this.overviewDetailArea.setLineWrap(true);
        this.overviewDetailArea.setWrapStyleWord(true);
        this.overviewDetailArea.setText("\u8bf7\u9009\u62e9\u5de6\u4fa7\u70b9\u4f4d\u7ec4\u67e5\u770b\u8be6\u60c5\u3002");
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 8, 0));
        JButton jButton = new JButton("\u5f00\u59cb\u76d1\u63a7");
        JButton jButton2 = new JButton("\u505c\u6b62\u76d1\u63a7");
        JButton jButton3 = new JButton("\u7acb\u5373\u68c0\u6d4b");
        JButton jButton4 = new JButton("\u67e5\u770b\u62a5\u8b66\u8be6\u60c5");
        JButton jButton5 = new JButton("\u5df2\u5173\u6ce8");
        jButton.addActionListener(actionEvent -> this.startMonitoring());
        jButton2.addActionListener(actionEvent -> this.stopMonitoring());
        jButton3.addActionListener(actionEvent -> this.checkNow());
        jButton4.addActionListener(actionEvent -> this.navigationList.setSelectedValue(PAGE_ALERT_CENTER, true));
        jButton5.addActionListener(actionEvent -> this.acknowledgeSelectedOverviewAlert());
        jPanel3.add(jButton);
        jPanel3.add(jButton2);
        jPanel3.add(jButton3);
        jPanel3.add(jButton4);
        jPanel3.add(jButton5);
        JPanel jPanel4 = new JPanel(new BorderLayout(8, 8));
        jPanel4.setBorder(BorderFactory.createTitledBorder("\u70b9\u4f4d\u7ec4\u8be6\u60c5"));
        jPanel4.add((Component)new JScrollPane(this.overviewDetailArea), "Center");
        jPanel4.add((Component)jPanel3, "South");
        JSplitPane jSplitPane = new JSplitPane(1, new JScrollPane(this.overviewTable), jPanel4);
        jSplitPane.setDividerLocation(690);
        jSplitPane.setResizeWeight(0.68);
        jPanel.add((Component)jSplitPane, "Center");
        return jPanel;
    }

    private JPanel statCard(String string, JLabel jLabel, Color color) {
        JPanel jPanel = new JPanel(new BorderLayout(6, 6));
        jPanel.setBackground(Color.WHITE);
        jPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 225, 232)), BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel jLabel2 = new JLabel(string);
        jLabel2.setForeground(new Color(80, 88, 100));
        jLabel.setFont(jLabel.getFont().deriveFont(1, 28.0f));
        jLabel.setForeground(color);
        jPanel.add((Component)jLabel2, "North");
        jPanel.add((Component)jLabel, "Center");
        return jPanel;
    }

    private JPanel buildConnectionPage() {
        JPanel jPanel = new JPanel(new BorderLayout(10, 10));
        jPanel.setBorder(BorderFactory.createTitledBorder(PAGE_CONNECTIONS));
        this.profileList.setSelectionMode(0);
        this.profileList.addListSelectionListener(listSelectionEvent -> {
            int n;
            if (!listSelectionEvent.getValueIsAdjusting() && (n = this.profileList.getSelectedIndex()) >= 0 && n < this.profiles.size()) {
                this.populateProfileForm(this.profiles.get(n));
            }
        });
        JScrollPane jScrollPane = new JScrollPane(this.profileList);
        jScrollPane.setPreferredSize(new Dimension(220, 260));
        jPanel.add((Component)jScrollPane, "West");
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        int n = 0;
        this.addField(jPanel2, n, 0, "\u8fde\u63a5ID", this.profileIdField);
        this.addField(jPanel2, n, 2, "\u8fde\u63a5\u540d\u79f0", this.profileNameField);
        this.addField(jPanel2, n, 4, "\u6570\u636e\u5e93\u7c7b\u578b", this.profileDbTypeBox);
        this.addField(jPanel2, ++n, 0, "\u670d\u52a1\u5668\u5730\u5740/IP", this.profileHostField);
        this.addField(jPanel2, n, 2, "\u7aef\u53e3", this.profilePortSpinner);
        this.addField(jPanel2, n, 4, "\u6570\u636e\u5e93\u540d", this.profileDatabaseField);
        this.addField(jPanel2, ++n, 0, "\u6570\u636e\u5e93\u7a7a\u95f4/Schema", this.profileSchemaField);
        this.addField(jPanel2, n, 2, "\u7528\u6237\u540d", this.profileUserField);
        this.addField(jPanel2, n, 4, "SSL\u6a21\u5f0f", this.profileSslModeBox);
        this.addField(jPanel2, ++n, 0, "\u672c\u5730\u6d4b\u8bd5\u5e93\u8def\u5f84", this.profileLocalPathField);
        this.addField(jPanel2, n, 2, "\u5bc6\u7801", this.profilePasswordField);
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 8, 0));
        JButton jButton = new JButton("\u65b0\u5efa\u8fde\u63a5");
        JButton jButton2 = new JButton("\u4fdd\u5b58\u8fde\u63a5");
        JButton jButton3 = new JButton("\u5220\u9664\u8fde\u63a5");
        JButton jButton4 = new JButton("\u6d4b\u8bd5\u8fde\u63a5");
        JButton jButton5 = new JButton("\u8bbe\u4e3a\u5f53\u524d\u8fde\u63a5");
        jPanel3.add(jButton);
        jPanel3.add(jButton2);
        jPanel3.add(jButton3);
        jPanel3.add(jButton4);
        jPanel3.add(jButton5);
        GridBagConstraints gridBagConstraints = this.gbc(0, ++n);
        gridBagConstraints.gridwidth = 6;
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add((Component)jPanel3, gridBagConstraints);
        jButton.addActionListener(actionEvent -> this.newProfileForm());
        jButton2.addActionListener(actionEvent -> this.saveProfile());
        jButton3.addActionListener(actionEvent -> this.deleteProfile());
        jButton4.addActionListener(actionEvent -> this.testSelectedProfile());
        jButton5.addActionListener(actionEvent -> this.useProfileWithoutTest());
        this.profileDbTypeBox.addActionListener(actionEvent -> this.updateProfileTypeEnabled());
        JPanel jPanel4 = new JPanel(new GridLayout(0, 1, 4, 4));
        jPanel4.setBorder(BorderFactory.createTitledBorder("\u5b89\u5168\u8bf4\u660e"));
        jPanel4.add(new JLabel("\u6570\u636e\u5e93\u8bbf\u95ee\u6a21\u5f0f\uff1a\u53ea\u8bfb"));
        jPanel4.add(new JLabel("\u5bc6\u7801\u4ec5\u4fdd\u5b58\u5728\u5f53\u524d\u8fd0\u884c\u5185\u5b58\uff0c\u4e0d\u5199\u5165\u914d\u7f6e\u6587\u4ef6"));
        jPanel4.add(new JLabel("\u73b0\u573a\u5fc5\u987b\u4f7f\u7528\u6570\u636e\u5e93\u53ea\u8bfb\u8d26\u53f7"));
        jPanel4.add(new JLabel("\u4e0d\u5c55\u793a\u5b8c\u6574 JDBC URL\u3001\u5bc6\u7801\u6216\u5386\u53f2\u5bc6\u7801"));
        jPanel.add((Component)jPanel2, "Center");
        jPanel.add((Component)jPanel4, "East");
        return jPanel;
    }

    private JPanel buildBrowserPage() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder("\u6570\u636e\u6e90\u6d4f\u89c8\u5668\uff08\u53ea\u8bfb\uff09"));
        JPanel jPanel2 = new JPanel(new GridLayout(1, 4, 8, 0));
        jPanel2.add(this.browserStatCard(this.schemaCountLabel));
        jPanel2.add(this.browserStatCard(this.objectCountLabel));
        jPanel2.add(this.browserStatCard(this.objectTypeLabel));
        jPanel2.add(this.browserStatCard(this.browserModeLabel));
        JPanel jPanel3 = new JPanel(new FlowLayout(0));
        JButton jButton = new JButton("\u5237\u65b0 Schema");
        JButton jButton2 = new JButton("\u52a0\u8f7d\u8868/\u89c6\u56fe");
        JButton jButton3 = new JButton("\u9884\u89c8\u524d100\u884c");
        jPanel3.add(new JLabel("Schema\uff1a"));
        jPanel3.add(this.schemaBox);
        jPanel3.add(jButton);
        jPanel3.add(jButton2);
        jPanel3.add(jButton3);
        JPanel jPanel4 = new JPanel(new BorderLayout());
        jPanel4.add((Component)jPanel2, "North");
        jPanel4.add((Component)jPanel3, "South");
        jPanel.add((Component)jPanel4, "North");
        this.browserTable.setSelectionMode(0);
        this.browserTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                this.loadSelectedTableColumns();
            }
        });
        this.columnTable.setSelectionMode(0);
        JPanel jPanel5 = new JPanel(new BorderLayout(6, 6));
        jPanel5.setBorder(BorderFactory.createTitledBorder("Schema / \u8868 / \u89c6\u56fe\u5bf9\u8c61\u6811"));
        JSplitPane jSplitPane = new JSplitPane(0, new JScrollPane(this.dataSourceTree), new JScrollPane(this.browserTable));
        jSplitPane.setDividerLocation(180);
        jPanel5.add((Component)jSplitPane, "Center");
        JPanel jPanel6 = new JPanel(new BorderLayout(6, 6));
        jPanel6.setBorder(BorderFactory.createTitledBorder("\u5bf9\u8c61\u5143\u6570\u636e\u4e0e\u5217\u4fe1\u606f"));
        jPanel6.add((Component)new JScrollPane(this.columnTable), "Center");
        JPanel jPanel7 = new JPanel(new BorderLayout(6, 6));
        jPanel7.setBorder(BorderFactory.createTitledBorder("\u524d 100 \u884c\u53ea\u8bfb\u9884\u89c8"));
        jPanel7.add((Component)new JScrollPane(this.previewTable), "Center");
        JSplitPane jSplitPane2 = new JSplitPane(1, jPanel6, jPanel7);
        jSplitPane2.setDividerLocation(430);
        jSplitPane2.setResizeWeight(0.45);
        JSplitPane jSplitPane3 = new JSplitPane(1, jPanel5, jSplitPane2);
        jSplitPane3.setDividerLocation(320);
        jSplitPane3.setResizeWeight(0.25);
        jPanel.add((Component)jSplitPane3, "Center");
        jButton.addActionListener(actionEvent -> this.runOnceInBackground(this::refreshSchemas));
        jButton2.addActionListener(actionEvent -> this.runOnceInBackground(this::loadTablesForSelectedSchema));
        jButton3.addActionListener(actionEvent -> this.runOnceInBackground(this::previewSelectedTable));
        return jPanel;
    }

    private JPanel browserStatCard(JLabel jLabel) {
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.setBackground(Color.WHITE);
        jPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 225, 232)), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        jPanel.add((Component)jLabel, "Center");
        return jPanel;
    }

    private JPanel buildDataQueryPage() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder("\u6570\u636e\u67e5\u8be2\uff08\u7ed3\u6784\u5316\u53ea\u8bfb\uff09"));
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        int n = 0;
        this.addField(jPanel2, n, 0, "\u70b9\u4f4d\u7f16\u7801\u5173\u952e\u5b57", this.queryPointKeywordField);
        this.addField(jPanel2, n, 2, "\u8d27\u67b6\u7f16\u53f7\u5173\u952e\u5b57", this.queryShelfKeywordField);
        this.addField(jPanel2, n, 4, "\u533a\u57df\u7f16\u7801", this.queryAreaCodeField);
        this.addField(jPanel2, ++n, 0, "\u5173\u8054\u533a\u57df\u7f16\u7801", this.queryRelateAreaCodeField);
        this.addField(jPanel2, n, 2, "\u66f4\u65b0\u65f6\u95f4\u8d77", this.queryUpdatedFromField);
        this.addField(jPanel2, n, 4, "\u66f4\u65b0\u65f6\u95f4\u6b62", this.queryUpdatedToField);
        this.addField(jPanel2, ++n, 0, "\u884c\u6570\u4e0a\u9650", this.queryLimitSpinner);
        JButton jButton = new JButton("\u6267\u884c\u53ea\u8bfb\u67e5\u8be2");
        jButton.addActionListener(actionEvent -> this.startPointDataQuery(true));
        GridBagConstraints gridBagConstraints = this.gbc(2, n);
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = 2;
        jPanel2.add((Component)jButton, gridBagConstraints);
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 8, 0));
        jPanel3.add(this.queryPrevButton);
        jPanel3.add(this.queryNextButton);
        jPanel3.add(this.queryPageLabel);
        jPanel3.add(this.queryTotalLabel);
        GridBagConstraints gridBagConstraints2 = this.gbc(4, n);
        gridBagConstraints2.gridwidth = 2;
        gridBagConstraints2.fill = 2;
        jPanel2.add((Component)jPanel3, gridBagConstraints2);
        this.queryPrevButton.setEnabled(false);
        this.queryNextButton.setEnabled(false);
        this.queryPrevButton.addActionListener(actionEvent -> {
            if (this.queryCurrentPage > 1) {
                --this.queryCurrentPage;
                this.startPointDataQuery(false);
            }
        });
        this.queryNextButton.addActionListener(actionEvent -> {
            ++this.queryCurrentPage;
            this.startPointDataQuery(false);
        });
        this.dataQueryTable.setSelectionMode(0);
        this.dataQueryTable.setRowHeight(26);
        this.dataQueryTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                this.updateDataQueryDetail();
            }
        });
        this.dataQueryDetailArea.setEditable(false);
        this.dataQueryDetailArea.setLineWrap(true);
        this.dataQueryDetailArea.setWrapStyleWord(true);
        this.dataQueryDetailArea.setText("\u53ea\u8bfb\u67e5\u8be2\n\u4e0d\u652f\u6301 SQL \u7f16\u8f91\n\u4e0d\u652f\u6301\u6570\u636e\u4fee\u6539");
        JSplitPane jSplitPane = new JSplitPane(1, new JScrollPane(this.dataQueryTable), new JScrollPane(this.dataQueryDetailArea));
        jSplitPane.setDividerLocation(760);
        jSplitPane.setResizeWeight(0.75);
        JPanel jPanel4 = new JPanel(new GridLayout(0, 1, 4, 4));
        jPanel4.setBorder(BorderFactory.createTitledBorder("\u5b89\u5168\u8fb9\u754c"));
        jPanel4.add(new JLabel("\u53ea\u8bfb\u67e5\u8be2"));
        jPanel4.add(new JLabel("\u4e0d\u652f\u6301 SQL \u7f16\u8f91"));
        jPanel4.add(new JLabel("\u4e0d\u652f\u6301\u6570\u636e\u4fee\u6539"));
        jPanel4.add(new JLabel("\u56fa\u5b9a\u6765\u6e90\u8868\uff1atcs_map_data"));
        jPanel.add((Component)jPanel2, "North");
        jPanel.add((Component)jSplitPane, "Center");
        jPanel.add((Component)jPanel4, "South");
        return jPanel;
    }

    private JPanel buildGroupManagementPage() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder(PAGE_GROUPS));
        JPanel jPanel2 = new JPanel(new FlowLayout(0, 8, 0));
        JButton jButton = new JButton("\u65b0\u589e\u7ec4");
        JButton jButton2 = new JButton("\u5220\u9664\u7ec4");
        JButton jButton3 = new JButton("\u65b0\u589e\u70b9\u4f4d");
        JButton jButton4 = new JButton("\u5220\u9664\u70b9\u4f4d");
        JButton jButton5 = new JButton("\u4fdd\u5b58\u914d\u7f6e");
        JButton jButton6 = new JButton("\u653e\u5f03\u4fee\u6539");
        JButton jButton7 = new JButton("\u9a8c\u8bc1\u914d\u7f6e");
        jPanel2.add(jButton);
        jPanel2.add(jButton2);
        jPanel2.add(jButton3);
        jPanel2.add(jButton4);
        jPanel2.add(jButton5);
        jPanel2.add(jButton6);
        jPanel2.add(jButton7);
        jPanel2.add(this.startButton);
        jPanel2.add(this.stopButton);
        jPanel2.add(this.checkButton);
        jPanel.add((Component)jPanel2, "North");
        this.groupList.setSelectionMode(0);
        this.groupList.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                this.populateSelectedGroup();
            }
        });
        JScrollPane jScrollPane = new JScrollPane(this.groupList);
        jScrollPane.setPreferredSize(new Dimension(240, 360));
        JPanel jPanel3 = new JPanel(new BorderLayout(8, 8));
        jPanel3.add((Component)this.buildGroupDetailForm(), "North");
        this.groupPointTable.setRowHeight(26);
        this.groupPointTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        this.groupPointTable.getColumnModel().getColumn(1).setPreferredWidth(160);
        this.groupPointTable.getColumnModel().getColumn(2).setPreferredWidth(260);
        this.groupPointTable.getColumnModel().getColumn(3).setPreferredWidth(60);
        this.groupPointTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JComboBox<String>(new String[]{PointRole.USE.name(), PointRole.BACKUP.name()})));
        JPanel jPanel4 = new JPanel(new BorderLayout(8, 8));
        jPanel4.setBorder(BorderFactory.createTitledBorder("\u70b9\u4f4d\u914d\u7f6e"));
        jPanel4.add((Component)new JScrollPane(this.groupPointTable), "Center");
        JPanel jPanel5 = new JPanel(new BorderLayout(8, 8));
        jPanel5.setBorder(BorderFactory.createTitledBorder("\u70b9\u4f4d\u72b6\u6001\u770b\u677f"));
        this.groupSummaryLabel.setFont(this.groupSummaryLabel.getFont().deriveFont(1, 15.0f));
        this.pointStatusPanel.setBackground(Color.WHITE);
        this.pointStatusPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        jPanel5.add((Component)this.groupSummaryLabel, "North");
        jPanel5.add((Component)new JScrollPane(this.pointStatusPanel), "Center");
        this.groupRuntimeArea.setEditable(false);
        this.groupRuntimeArea.setRows(4);
        this.groupRuntimeArea.setFont(new Font("Monospaced", 0, 13));
        jPanel5.add((Component)new JScrollPane(this.groupRuntimeArea), "South");
        JSplitPane jSplitPane = new JSplitPane(0, jPanel5, jPanel4);
        jSplitPane.setResizeWeight(0.65);
        jSplitPane.setDividerLocation(360);
        jPanel3.add((Component)jSplitPane, "Center");
        JSplitPane jSplitPane2 = new JSplitPane(1, jScrollPane, jPanel3);
        jSplitPane2.setDividerLocation(240);
        jPanel.add((Component)jSplitPane2, "Center");
        jButton.addActionListener(actionEvent -> this.addPointGroup());
        jButton2.addActionListener(actionEvent -> this.removeSelectedGroup());
        jButton3.addActionListener(actionEvent -> this.groupPointModel.addRow(new Object[]{PointRole.BACKUP.name(), "\u5907\u7528\u4f4d", "", Boolean.TRUE}));
        jButton4.addActionListener(actionEvent -> this.removeSelectedGroupPointRows());
        jButton5.addActionListener(actionEvent -> this.saveGroupConfig());
        jButton6.addActionListener(actionEvent -> this.loadGroupConfig());
        jButton7.addActionListener(actionEvent -> this.validateGroupConfigFromUi());
        this.startButton.addActionListener(actionEvent -> this.startMonitoring());
        this.stopButton.addActionListener(actionEvent -> this.stopMonitoring());
        this.checkButton.addActionListener(actionEvent -> this.checkNow());
        this.stopButton.setEnabled(false);
        return jPanel;
    }

    private JPanel buildGroupDetailForm() {
        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.setBorder(BorderFactory.createTitledBorder("\u57fa\u672c\u4fe1\u606f"));
        int n = 0;
        this.addField(jPanel, n, 0, "\u7ec4ID", this.groupIdField);
        this.addField(jPanel, n, 2, "\u533a\u57df", this.groupAreaField);
        this.addCheckBox(jPanel, n, 4, this.groupEnabledBox);
        this.addField(jPanel, ++n, 0, "\u7ec4\u540d", this.groupNameField);
        this.addField(jPanel, n, 2, "\u7269\u6599", this.groupMaterialField);
        this.addCheckBox(jPanel, n, 4, this.ruleEnabledBox);
        this.addField(jPanel, ++n, 0, "\u68c0\u6d4b\u5468\u671f(\u5206\u949f)", this.groupCheckIntervalMinutesSpinner);
        this.addField(jPanel, n, 2, "\u62a5\u8b66\u6301\u7eed(\u5206\u949f)", this.durationMinutesSpinner);
        this.addCheckBox(jPanel, n, 4, this.requireUseEmptyBox);
        this.addField(jPanel, ++n, 0, "\u6700\u5c11\u5907\u7528\u4f4d\u6709\u6599", this.minBackupAvailableSpinner);
        this.addCheckBox(jPanel, n, 2, this.backupThresholdParticipatesBox);
        GridBagConstraints gridBagConstraints = this.gbc(4, n);
        gridBagConstraints.fill = 2;
        jPanel.add((Component)new JLabel("\u62a5\u8b66\u89c4\u5219"), gridBagConstraints);
        return jPanel;
    }

    private JPanel buildAlertCenterPage() {
        AbstractButton abstractButton;
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder(PAGE_ALERT_CENTER));
        JPanel jPanel2 = new JPanel(new FlowLayout(0, 8, 0));
        jPanel2.add(new JLabel("\u6d3b\u8dc3\u62a5\u8b66"));
        jPanel2.add(new JLabel("\u5df2\u5173\u6ce8"));
        jPanel2.add(new JLabel("\u89c2\u5bdf\u4e2d"));
        jPanel2.add(new JLabel("\u67e5\u8be2\u5931\u8d25"));
        jPanel2.add(new JLabel("\u5df2\u6062\u590d"));
        jPanel2.add(new JLabel("\u7b5b\u9009\uff1a"));
        jPanel2.add(this.alertCenterFilterBox);
        ButtonGroup buttonGroup = new ButtonGroup();
        for (String object2 : new String[]{"\u6d3b\u8dc3\u62a5\u8b66", "\u5df2\u5173\u6ce8", "\u89c2\u5bdf\u4e2d", "\u67e5\u8be2\u5931\u8d25", "\u5df2\u6062\u590d"}) {
            abstractButton = new JToggleButton(object2);
            buttonGroup.add(abstractButton);
            jPanel2.add(abstractButton);
            abstractButton.addActionListener(actionEvent -> {
                this.alertCenterFilterBox.setSelectedItem(object2);
                this.refreshAlertCenterPage();
            });
            if (!object2.equals(String.valueOf(this.alertCenterFilterBox.getSelectedItem()))) continue;
            abstractButton.setSelected(true);
        }
        JButton jButton = new JButton("\u5237\u65b0\u62a5\u8b66");
        JButton jButton2 = new JButton("\u6807\u8bb0\u5df2\u5173\u6ce8");
        JButton jButton3 = new JButton("\u67e5\u770b\u70b9\u4f4d\u8be6\u60c5");
        JButton jButton4 = new JButton("\u7acb\u5373\u68c0\u6d4b\u8be5\u7ec4");
        abstractButton = new JButton("\u67e5\u770b\u8fde\u63a5\u72b6\u6001");
        jPanel2.add(jButton);
        jPanel2.add(jButton2);
        jPanel2.add(jButton3);
        jPanel2.add(jButton4);
        jPanel2.add(abstractButton);
        jPanel.add((Component)jPanel2, "North");
        this.alertCenterTable.setSelectionMode(0);
        this.alertCenterTable.setRowHeight(26);
        this.alertCenterTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                this.updateAlertCenterDetail();
            }
        });
        this.alertCenterDetailArea.setEditable(false);
        this.alertCenterDetailArea.setLineWrap(true);
        this.alertCenterDetailArea.setWrapStyleWord(true);
        this.alertCenterDetailArea.setText("\u5f53\u524d\u65e0\u62a5\u8b66\u4e8b\u4ef6\u3002\u67e5\u8be2\u5931\u8d25\u5c5e\u4e8e\u7cfb\u7edf\u6570\u636e\u5f02\u5e38\uff0c\u4e0d\u5c5e\u4e8e\u7f3a\u6599\u62a5\u8b66\u3002");
        JSplitPane jSplitPane = new JSplitPane(1, new JScrollPane(this.alertCenterTable), new JScrollPane(this.alertCenterDetailArea));
        jSplitPane.setDividerLocation(720);
        jSplitPane.setResizeWeight(0.7);
        jPanel.add((Component)jSplitPane, "Center");
        this.alertCenterFilterBox.addActionListener(actionEvent -> this.refreshAlertCenterPage());
        jButton.addActionListener(actionEvent -> this.refreshAlertCenterPage());
        jButton2.addActionListener(actionEvent -> this.acknowledgeSelectedAlertCenterGroup());
        jButton3.addActionListener(actionEvent -> this.showSelectedAlertCenterGroupInOverview());
        jButton4.addActionListener(actionEvent -> this.checkSelectedAlertCenterGroup());
        abstractButton.addActionListener(actionEvent -> this.navigationList.setSelectedValue(PAGE_CONNECTIONS, true));
        return jPanel;
    }

    private JPanel buildLogsSystemPage() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder(PAGE_LOGS));
        JPanel jPanel2 = new JPanel(new GridLayout(1, 6, 8, 0));
        jPanel2.add(this.browserStatCard(this.schedulerHealthLabel));
        jPanel2.add(this.browserStatCard(this.connectionHealthLabel));
        jPanel2.add(this.browserStatCard(this.detectionHealthLabel));
        jPanel2.add(this.browserStatCard(this.configHealthLabel));
        jPanel2.add(this.browserStatCard(this.logDirHealthLabel));
        jPanel2.add(this.browserStatCard(this.selfTestHealthLabel));
        jPanel2.add(this.browserStatCard(new JLabel("\u76d1\u63a7\u8c03\u5ea6\u5668\uff1a" + (this.scheduledTask == null ? "\u672a\u8fd0\u884c" : "\u8fd0\u884c\u4e2d"))));
        jPanel2.add(this.browserStatCard(new JLabel("\u5f53\u524d\u8fde\u63a5\uff1a" + (this.currentProfile == null ? "\u672a\u8fde\u63a5" : this.currentProfile.name()))));
        jPanel2.add(this.browserStatCard(new JLabel(this.lastCheckLabel.getText())));
        jPanel2.add(this.browserStatCard(new JLabel("\u914d\u7f6e\u6587\u4ef6\uff1adata")));
        jPanel2.add(this.browserStatCard(new JLabel("\u65e5\u5fd7\u76ee\u5f55\uff1alogs")));
        jPanel2.add(this.browserStatCard(new JLabel("\u81ea\u68c0\u72b6\u6001\uff1a\u53ef\u6267\u884c")));
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 8, 0));
        jPanel3.add(new JLabel("\u4e8b\u4ef6\u7c7b\u578b\uff1a"));
        jPanel3.add(this.systemLogTypeFilterBox);
        jPanel3.add(new JLabel("\u65f6\u95f4\u8d77\uff1a"));
        jPanel3.add(this.systemLogFromField);
        jPanel3.add(new JLabel("\u65f6\u95f4\u6b62\uff1a"));
        jPanel3.add(this.systemLogToField);
        jPanel3.add(new JLabel("\u70b9\u4f4d\u7ec4\uff1a"));
        jPanel3.add(this.systemLogGroupField);
        jPanel3.add(new JLabel("\u5173\u952e\u5b57\uff1a"));
        jPanel3.add(this.systemLogKeywordField);
        JButton jButton = new JButton("\u5237\u65b0\u65e5\u5fd7");
        JButton jButton2 = new JButton("\u6253\u5f00\u65e5\u5fd7\u76ee\u5f55");
        JButton jButton3 = new JButton("\u6267\u884c\u81ea\u68c0");
        JButton jButton4 = new JButton("\u5bfc\u51fa\u8bca\u65ad\u4fe1\u606f");
        jPanel3.add(jButton);
        jPanel3.add(jButton2);
        jPanel3.add(jButton3);
        jPanel3.add(jButton4);
        JPanel jPanel4 = new JPanel(new BorderLayout(8, 8));
        jPanel4.add((Component)jPanel2, "North");
        jPanel4.add((Component)jPanel3, "South");
        jPanel.add((Component)jPanel4, "North");
        this.systemLogTable.setSelectionMode(0);
        this.systemLogTable.setRowHeight(26);
        this.systemLogTable.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                this.updateSystemLogDetail();
            }
        });
        this.systemLogDetailArea.setEditable(false);
        this.systemLogDetailArea.setLineWrap(true);
        this.systemLogDetailArea.setWrapStyleWord(true);
        JSplitPane jSplitPane = new JSplitPane(1, new JScrollPane(this.systemLogTable), new JScrollPane(this.systemLogDetailArea));
        jSplitPane.setDividerLocation(720);
        jSplitPane.setResizeWeight(0.7);
        JSplitPane jSplitPane2 = new JSplitPane(0, jSplitPane, new JScrollPane(this.statusArea));
        jSplitPane2.setDividerLocation(430);
        jSplitPane2.setResizeWeight(0.75);
        jPanel.add((Component)jSplitPane2, "Center");
        jButton.addActionListener(actionEvent -> this.loadSystemLogs());
        this.systemLogTypeFilterBox.addActionListener(actionEvent -> this.loadSystemLogs());
        this.systemLogFromField.addActionListener(actionEvent -> this.loadSystemLogs());
        this.systemLogToField.addActionListener(actionEvent -> this.loadSystemLogs());
        this.systemLogGroupField.addActionListener(actionEvent -> this.loadSystemLogs());
        this.systemLogKeywordField.addActionListener(actionEvent -> this.loadSystemLogs());
        jButton2.addActionListener(actionEvent -> this.openLogs());
        jButton3.addActionListener(actionEvent -> this.runOnceInBackground(this::executeSelfTestFromUi));
        jButton4.addActionListener(actionEvent -> this.runOnceInBackground(this::exportDiagnostics));
        return jPanel;
    }

    private JPanel buildSystemSettingsPage() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder(PAGE_SETTINGS));
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        int n = 0;
        this.addField(jPanel2, n, 0, "\u9ed8\u8ba4\u9996\u9875", this.settingsDefaultPageBox);
        this.addField(jPanel2, n, 2, "\u76d1\u63a7\u603b\u89c8\u81ea\u52a8\u5237\u65b0\u95f4\u9694(\u79d2)", this.settingsOverviewRefreshSpinner);
        this.addCheckBox(jPanel2, ++n, 0, this.settingsAlertPopupBox);
        this.addCheckBox(jPanel2, n, 2, this.settingsAlertSoundBox);
        this.addField(jPanel2, ++n, 0, "\u65e5\u5fd7\u4fdd\u7559\u5929\u6570", this.settingsLogRetentionSpinner);
        this.addField(jPanel2, n, 2, "\u754c\u9762\u663e\u793a\u5bc6\u5ea6", this.settingsDensityBox);
        this.addCheckBox(jPanel2, ++n, 0, this.settingsStartupSelfTestBox);
        this.addCheckBox(jPanel2, n, 2, this.settingsAutoCleanupLogsBox);
        JPanel jPanel3 = new JPanel(new GridLayout(0, 1, 6, 6));
        jPanel3.setBorder(BorderFactory.createTitledBorder("\u56fa\u5b9a\u5b89\u5168\u9879"));
        jPanel3.add(new JLabel("\u654f\u611f\u4fe1\u606f\u8131\u654f\uff1a\u5f3a\u5236\u542f\u7528"));
        jPanel3.add(new JLabel("\u6570\u636e\u5e93\u8bbf\u95ee\u6a21\u5f0f\uff1a\u53ea\u8bfb"));
        jPanel3.add(new JLabel("SQL \u7f16\u8f91\u80fd\u529b\uff1a\u672a\u63d0\u4f9b"));
        jPanel3.add(new JLabel("\u5bc6\u7801\u6301\u4e45\u5316\uff1a\u7981\u6b62"));
        JPanel jPanel4 = new JPanel(new FlowLayout(2, 8, 0));
        JButton jButton = new JButton("\u4fdd\u5b58\u8bbe\u7f6e");
        JButton jButton2 = new JButton("\u6062\u590d\u9ed8\u8ba4");
        JButton jButton3 = new JButton("\u91cd\u65b0\u52a0\u8f7d\u914d\u7f6e");
        jPanel4.add(jButton);
        jPanel4.add(jButton2);
        jPanel4.add(jButton3);
        jButton.addActionListener(actionEvent -> this.saveUiPreferences());
        jButton2.addActionListener(actionEvent -> this.restoreUiPreferences());
        jButton3.addActionListener(actionEvent -> this.reloadUiPreferences());
        jPanel.add((Component)jPanel2, "North");
        jPanel.add((Component)jPanel3, "Center");
        jPanel.add((Component)jPanel4, "South");
        return jPanel;
    }

    private void addCheckBox(JPanel jPanel, int n, int n2, JCheckBox jCheckBox) {
        GridBagConstraints gridBagConstraints = this.gbc(n2, n);
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = 17;
        jPanel.add((Component)jCheckBox, gridBagConstraints);
    }

    private JPanel buildLegacyAlertPage() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder("\u70b9\u4f4d\u7f3a\u6599\u62a5\u8b66"));
        JPanel jPanel2 = new JPanel(new GridBagLayout());
        this.addField(jPanel2, 0, 0, "\u5168\u5c40\u626b\u63cf(\u79d2)", this.intervalSpinner);
        JPanel jPanel3 = new JPanel(new FlowLayout(0, 8, 0));
        JButton jButton = new JButton("\u4fdd\u5b58\u70b9\u4f4d\u914d\u7f6e");
        jPanel3.add(jButton);
        jPanel3.add(this.startButton);
        jPanel3.add(this.stopButton);
        jPanel3.add(this.checkButton);
        GridBagConstraints gridBagConstraints = this.gbc(2, 0);
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = 2;
        gridBagConstraints.weightx = 1.0;
        jPanel2.add((Component)jPanel3, gridBagConstraints);
        jPanel.add((Component)jPanel2, "North");
        this.pointTable.setRowHeight(26);
        this.pointTable.getColumnModel().getColumn(0).setPreferredWidth(140);
        this.pointTable.getColumnModel().getColumn(1).setPreferredWidth(360);
        this.pointTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        jPanel.add((Component)new JScrollPane(this.pointTable), "Center");
        JPanel jPanel4 = new JPanel(new FlowLayout(0));
        JButton jButton2 = new JButton("\u6dfb\u52a0\u70b9\u4f4d");
        JButton jButton3 = new JButton("\u5220\u9664\u9009\u4e2d");
        jPanel4.add(jButton2);
        jPanel4.add(jButton3);
        jPanel.add((Component)jPanel4, "South");
        jButton2.addActionListener(actionEvent -> this.pointModel.addRow(new Object[]{"\u65b0\u70b9\u4f4d", "", 5}));
        jButton3.addActionListener(actionEvent -> this.removeSelectedPointRows());
        jButton.addActionListener(actionEvent -> this.savePointConfig());
        this.startButton.addActionListener(actionEvent -> this.startMonitoring());
        this.stopButton.addActionListener(actionEvent -> this.stopMonitoring());
        this.checkButton.addActionListener(actionEvent -> this.checkNow());
        this.stopButton.setEnabled(false);
        return jPanel;
    }

    private JPanel buildStatusPanel() {
        JPanel jPanel = new JPanel(new BorderLayout(8, 8));
        jPanel.setBorder(BorderFactory.createTitledBorder("\u8fd0\u884c\u65e5\u5fd7"));
        this.statusArea.setEditable(false);
        this.statusArea.setRows(8);
        this.statusArea.setFont(new Font("Monospaced", 0, 13));
        jPanel.add((Component)new JScrollPane(this.statusArea), "Center");
        JButton jButton = new JButton("\u6253\u5f00\u65e5\u5fd7\u76ee\u5f55");
        jButton.addActionListener(actionEvent -> this.openLogs());
        JPanel jPanel2 = new JPanel(new FlowLayout(2));
        jPanel2.add(jButton);
        jPanel.add((Component)jPanel2, "South");
        return jPanel;
    }

    private void addField(JPanel jPanel, int n, int n2, String string, Component component) {
        GridBagConstraints gridBagConstraints = this.gbc(n2, n);
        gridBagConstraints.anchor = 13;
        jPanel.add((Component)new JLabel(string + "\uff1a"), gridBagConstraints);
        GridBagConstraints gridBagConstraints2 = this.gbc(n2 + 1, n);
        gridBagConstraints2.fill = 2;
        gridBagConstraints2.weightx = 1.0;
        jPanel.add(component, gridBagConstraints2);
    }

    private GridBagConstraints gbc(int n, int n2) {
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = n;
        gridBagConstraints.gridy = n2;
        gridBagConstraints.insets = new Insets(4, 4, 4, 4);
        return gridBagConstraints;
    }

    private void loadProfiles() {
        ConnectionProfileStore.StoredProfiles storedProfiles = this.profileStore.load();
        this.profiles = new ArrayList<ConnectionProfile>(storedProfiles.profiles());
        this.refreshProfileList(storedProfiles.currentId());
    }

    private void refreshProfileList(String string) {
        this.profileListModel.clear();
        int n = 0;
        for (int i = 0; i < this.profiles.size(); ++i) {
            ConnectionProfile connectionProfile = this.profiles.get(i);
            this.profileListModel.addElement(connectionProfile.name() + " [" + connectionProfile.id() + "]");
            if (!connectionProfile.id().equals(string)) continue;
            n = i;
        }
        if (!this.profiles.isEmpty()) {
            this.profileList.setSelectedIndex(n);
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
        this.profileList.clearSelection();
        this.updateProfileTypeEnabled();
    }

    private void updateProfileTypeEnabled() {
        boolean bl = "h2".equals(this.profileDbTypeBox.getSelectedItem());
        this.profileHostField.setEnabled(!bl);
        this.profilePortSpinner.setEnabled(!bl);
        this.profileDatabaseField.setEnabled(!bl);
        this.profileSchemaField.setEnabled(true);
        this.profileUserField.setEnabled(!bl);
        this.profileSslModeBox.setEnabled(!bl);
        this.profileLocalPathField.setEnabled(bl);
        if (bl) {
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
            ConnectionProfile connectionProfile;
            this.currentProfile = connectionProfile = this.readProfileForm();
            Arrays.fill(this.currentPassword, '\u0000');
            this.currentPassword = (char[])this.profilePasswordField.getPassword().clone();
            this.updateCurrentConnectionLabel();
            this.appendStatus("\u5df2\u8bbe\u4e3a\u5f53\u524d\u8fde\u63a5\uff1a" + connectionProfile.name() + "\u3002\u5efa\u8bae\u6267\u884c\u6d4b\u8bd5\u8fde\u63a5\u786e\u8ba4\u8d26\u53f7\u53ef\u7528\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private ConnectionProfile readProfileForm() {
        Object object = this.profileIdField.getText().trim();
        if (((String)object).isEmpty()) {
            object = "profile" + System.currentTimeMillis();
            this.profileIdField.setText((String)object);
        }
        return new ConnectionProfile((String)object, this.profileNameField.getText(), String.valueOf(this.profileDbTypeBox.getSelectedItem()), this.profileHostField.getText(), (Integer)this.profilePortSpinner.getValue(), this.profileDatabaseField.getText(), this.profileSchemaField.getText(), this.profileUserField.getText(), String.valueOf(this.profileSslModeBox.getSelectedItem()), this.profileLocalPathField.getText());
    }

    private void saveProfile() {
        try {
            ConnectionProfile connectionProfile = this.readProfileForm();
            int n = this.findProfileIndex(connectionProfile.id());
            if (n >= 0) {
                this.profiles.set(n, connectionProfile);
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
        int n = this.profileList.getSelectedIndex();
        if (n < 0 || n >= this.profiles.size()) {
            this.showError(new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u8981\u5220\u9664\u7684\u8fde\u63a5"));
            return;
        }
        if (this.profiles.size() == 1) {
            this.showError(new IllegalArgumentException("\u81f3\u5c11\u4fdd\u7559\u4e00\u4e2a\u8fde\u63a5\u914d\u7f6e"));
            return;
        }
        ConnectionProfile connectionProfile = this.profiles.remove(n);
        try {
            String string = this.profiles.get(0).id();
            this.profileStore.save(string, this.profiles);
            if (this.currentProfile != null && this.currentProfile.id().equals(connectionProfile.id())) {
                this.currentProfile = null;
                Arrays.fill(this.currentPassword, '\u0000');
                this.currentPassword = new char[0];
                this.updateCurrentConnectionLabel();
            }
            this.refreshProfileList(string);
            this.appendStatus("\u8fde\u63a5\u914d\u7f6e\u5df2\u5220\u9664\uff1a" + connectionProfile.name());
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void testSelectedProfile() {
        try {
            ConnectionProfile connectionProfile = this.readProfileForm();
            char[] cArray = this.profilePasswordField.getPassword();
            this.runOnceInBackground(() -> {
                String string = this.pointRepository.testConnection(connectionProfile.toDbConfig(10), cArray);
                this.currentProfile = connectionProfile;
                Arrays.fill(this.currentPassword, '\u0000');
                this.currentPassword = (char[])cArray.clone();
                SwingUtilities.invokeLater(() -> {
                    this.updateCurrentConnectionLabel();
                    this.appendStatus("\u6d4b\u8bd5\u8fde\u63a5\u6210\u529f\u5e76\u8bbe\u4e3a\u5f53\u524d\u8fde\u63a5\uff1a" + string);
                });
            });
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private int findProfileIndex(String string) {
        for (int i = 0; i < this.profiles.size(); ++i) {
            if (!this.profiles.get(i).id().equals(string)) continue;
            return i;
        }
        return -1;
    }

    private void updateCurrentConnectionLabel() {
        if (this.currentProfile == null) {
            this.currentConnectionLabel.setText("\u5f53\u524d\u8fde\u63a5\uff1a\u672a\u8fde\u63a5");
            return;
        }
        this.currentConnectionLabel.setText("\u5f53\u524d\u8fde\u63a5\uff1a" + this.currentProfile.name() + " / " + this.currentProfile.dbType() + " / " + this.currentProfile.schema());
        this.loadSystemLogs();
        this.refreshSystemHealthStatus();
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

    private void selectSchema(String string) {
        for (int i = 0; i < this.schemaModel.getSize(); ++i) {
            if (!this.schemaModel.getElementAt(i).equalsIgnoreCase(string)) continue;
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
            for (SchemaInfo object : list) {
                this.dataSourceTreeRoot.add(new DefaultMutableTreeNode(object.name()));
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
        String string = this.requireSelectedSchema();
        List<TableInfo> list = this.metadataRepository.listTables(dbConfig, this.currentPassword, string);
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
            this.appendStatus("\u5df2\u52a0\u8f7d " + string + " \u4e0b\u8868/\u89c6\u56fe\uff1a" + list.size() + " \u4e2a");
        });
    }

    private void loadSelectedTableColumns() {
        int n = this.browserTable.getSelectedRow();
        if (n < 0) {
            return;
        }
        String string = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(n), 0));
        String string2 = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(n), 1));
        this.runOnceInBackground(() -> {
            DbConfig dbConfig = this.requireCurrentConfig(10);
            List<ColumnInfo> list = this.metadataRepository.listColumns(dbConfig, this.currentPassword, string, string2);
            SwingUtilities.invokeLater(() -> {
                this.columnModel.setRowCount(0);
                for (ColumnInfo columnInfo : list) {
                    this.columnModel.addRow(new Object[]{columnInfo.name(), columnInfo.typeName(), columnInfo.size(), columnInfo.nullable() ? "\u662f" : "\u5426", this.blankToDash(columnInfo.defaultValue()), this.blankToDash(columnInfo.remarks())});
                }
                this.objectTypeLabel.setText("\u5f53\u524d\u5bf9\u8c61\u7c7b\u578b\uff1a" + String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(n), 2)));
                this.appendStatus("\u5df2\u52a0\u8f7d\u5b57\u6bb5\uff1a" + string + "." + string2 + " / " + list.size() + " \u4e2a");
            });
        });
    }

    private void previewSelectedTable() throws Exception {
        int n = this.browserTable.getSelectedRow();
        if (n < 0) {
            throw new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u8981\u9884\u89c8\u7684\u8868\u6216\u89c6\u56fe");
        }
        String string = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(n), 0));
        String string2 = String.valueOf(this.browserTableModel.getValueAt(this.browserTable.convertRowIndexToModel(n), 1));
        DbConfig dbConfig = this.requireCurrentConfig(10);
        TablePreview tablePreview = this.metadataRepository.previewTable(dbConfig, this.currentPassword, string, string2, 100);
        SwingUtilities.invokeLater(() -> {
            this.previewModel.setColumnIdentifiers(tablePreview.columnNames().toArray());
            this.previewModel.setRowCount(0);
            for (List<String> list : tablePreview.rows()) {
                this.previewModel.addRow(list.toArray());
            }
            this.objectTypeLabel.setText("\u5f53\u524d\u5bf9\u8c61\u7c7b\u578b\uff1a\u9884\u89c8 " + tablePreview.rows().size() + " \u884c");
            this.appendStatus("\u5df2\u9884\u89c8\uff1a" + string + "." + string2 + " / " + tablePreview.rows().size() + " \u884c");
        });
    }

    private String requireSelectedSchema() {
        Object object = this.schemaBox.getSelectedItem();
        if (object == null || String.valueOf(object).isBlank()) {
            throw new IllegalArgumentException("\u8bf7\u5148\u5237\u65b0\u5e76\u9009\u62e9 Schema");
        }
        return String.valueOf(object);
    }

    private void loadGroupConfig() {
        this.pointGroups = new ArrayList<PointGroupDefinition>(this.groupConfigStore.load());
        this.refreshGroupList(this.pointGroups.isEmpty() ? "" : this.pointGroups.get(0).id());
        this.refreshOverviewPage();
        this.refreshAlertCenterPage();
    }

    private void refreshGroupList(String string) {
        this.groupListModel.clear();
        int n = -1;
        for (int i = 0; i < this.pointGroups.size(); ++i) {
            PointGroupDefinition pointGroupDefinition = this.pointGroups.get(i);
            this.groupListModel.addElement(pointGroupDefinition.areaName() + " / " + pointGroupDefinition.groupName() + " [" + pointGroupDefinition.id() + "]");
            if (!pointGroupDefinition.id().equals(string)) continue;
            n = i;
        }
        if (n < 0 && !this.pointGroups.isEmpty()) {
            n = 0;
        }
        if (n >= 0) {
            this.groupList.setSelectedIndex(n);
        }
    }

    private void populateSelectedGroup() {
        int n = this.groupList.getSelectedIndex();
        if (n < 0 || n >= this.pointGroups.size()) {
            return;
        }
        PointGroupDefinition pointGroupDefinition = this.pointGroups.get(n);
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
            String string = "group-" + System.currentTimeMillis();
            this.pointGroups.add(new PointGroupDefinition(string, "\u533a\u57df", "\u7269\u6599\u7ec4", "\u7269\u6599", true, 60, List.of(new GroupMonitorPoint(string + "-use", "USE_POINT_001", "\u4f7f\u7528\u4f4d", PointRole.USE, true, 1), new GroupMonitorPoint(string + "-backup-1", "BACKUP_POINT_001", "\u5907\u7528\u4f4d1", PointRole.BACKUP, true, 2), new GroupMonitorPoint(string + "-backup-2", "BACKUP_POINT_002", "\u5907\u7528\u4f4d2", PointRole.BACKUP, true, 3), new GroupMonitorPoint(string + "-backup-3", "BACKUP_POINT_003", "\u5907\u7528\u4f4d3", PointRole.BACKUP, true, 4), new GroupMonitorPoint(string + "-backup-4", "BACKUP_POINT_004", "\u5907\u7528\u4f4d4", PointRole.BACKUP, true, 5)), new GroupAlertRule(true, true, 3, 5, true)));
            this.refreshGroupList(string);
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void removeSelectedGroup() {
        int n = this.groupList.getSelectedIndex();
        if (n < 0 || n >= this.pointGroups.size()) {
            this.showError(new IllegalArgumentException("\u8bf7\u5148\u9009\u62e9\u70b9\u4f4d\u7ec4"));
            return;
        }
        PointGroupDefinition pointGroupDefinition = this.pointGroups.remove(n);
        Object object = this.groupMonitorLock;
        synchronized (object) {
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
        int n = this.groupList.getSelectedIndex();
        if (n < 0 || n >= this.pointGroups.size()) {
            return;
        }
        this.stopGroupTableEditing();
        PointGroupDefinition pointGroupDefinition = new PointGroupDefinition(this.groupIdField.getText(), this.groupAreaField.getText(), this.groupNameField.getText(), this.groupMaterialField.getText(), this.groupEnabledBox.isSelected(), (Integer)this.groupCheckIntervalMinutesSpinner.getValue() * 60, this.readGroupPoints(), new GroupAlertRule(this.ruleEnabledBox.isSelected(), this.requireUseEmptyBox.isSelected(), (Integer)this.minBackupAvailableSpinner.getValue(), (Integer)this.durationMinutesSpinner.getValue(), this.backupThresholdParticipatesBox.isSelected()));
        this.pointGroups.set(n, pointGroupDefinition);
    }

    private List<GroupMonitorPoint> readGroupPoints() {
        ArrayList<GroupMonitorPoint> arrayList = new ArrayList<GroupMonitorPoint>();
        for (int i = 0; i < this.groupPointModel.getRowCount(); ++i) {
            String string = this.groupCellText(i, 0);
            String string2 = this.groupCellText(i, 1);
            String string3 = this.groupCellText(i, 2);
            boolean bl = this.groupCellBoolean(i, 3);
            if (string.isEmpty() && string2.isEmpty() && string3.isEmpty()) continue;
            if (string.isEmpty() || string2.isEmpty() || string3.isEmpty()) {
                throw new IllegalArgumentException("\u70b9\u4f4d\u89d2\u8272\u3001\u522b\u540d\u3001\u7f16\u7801\u5fc5\u987b\u540c\u65f6\u586b\u5199");
            }
            arrayList.add(new GroupMonitorPoint(this.groupIdField.getText().trim() + "-point-" + (i + 1), string3, string2, PointRole.valueOf(string), bl, i + 1));
        }
        return arrayList;
    }

    private void stopGroupTableEditing() {
        if (this.groupPointTable.isEditing() && this.groupPointTable.getCellEditor() != null) {
            this.groupPointTable.getCellEditor().stopCellEditing();
        }
    }

    private String groupCellText(int n, int n2) {
        Object object = this.groupPointModel.getValueAt(n, n2);
        return object == null ? "" : String.valueOf(object).trim();
    }

    private boolean groupCellBoolean(int n, int n2) {
        Object object = this.groupPointModel.getValueAt(n, n2);
        return object instanceof Boolean ? (Boolean)object : Boolean.parseBoolean(String.valueOf(object));
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
            char[] cArray = Arrays.copyOf(this.currentPassword, this.currentPassword.length);
            List<PointGroupDefinition> list = List.copyOf(this.readGroups());
            this.groupConfigStore.save(list);
            this.runOnceInBackground(() -> this.checkGroups(dbConfig, cArray, list, LocalDateTime.now(), "\u624b\u52a8\u68c0\u6d4b"));
        }
        catch (Exception exception) {
            this.showError(exception);
            this.appendStatus("\u6267\u884c\u5931\u8d25\uff1a" + exception.getMessage());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void checkDueGroups() throws Exception {
        List<PointGroupDefinition> list;
        DbConfig dbConfig = this.requireCurrentConfig(60);
        List<PointGroupDefinition> list2 = this.monitoredGroups;
        LocalDateTime localDateTime = LocalDateTime.now();
        Object object = this.groupMonitorLock;
        synchronized (object) {
            list = GroupCheckPlanner.dueGroups(list2, this.groupStates, localDateTime);
        }
        if (list.isEmpty()) {
            return;
        }
        this.checkGroups(dbConfig, list, localDateTime, "\u81ea\u52a8\u68c0\u6d4b");
    }

    private void checkGroups(DbConfig dbConfig, List<PointGroupDefinition> list, LocalDateTime localDateTime, String string) {
        this.checkGroups(dbConfig, this.currentPassword, list, localDateTime, string);
    }

    private void checkGroups(DbConfig dbConfig, char[] cArray, List<PointGroupDefinition> list, LocalDateTime localDateTime, String string) {
        this.checkGroupsWithFetcher(list, localDateTime, string, pointGroupDefinition -> this.pointRepository.fetch(dbConfig, cArray, this.pointDefinitions(pointGroupDefinition)));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    GroupCheckRunResult checkGroupsWithFetcher(List<PointGroupDefinition> list, LocalDateTime localDateTime, String string2, GroupPointFetcher groupPointFetcher) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl = false;
        int n = 0;
        int n2 = 0;
        ArrayList<GroupEvaluation> arrayList = new ArrayList<GroupEvaluation>();
        for (PointGroupDefinition pointGroupDefinition : list) {
            GroupEvaluation groupEvaluation;
            Object object;
            GroupRuntimeState groupRuntimeState;
            Object lock = this.groupMonitorLock;
            synchronized (lock) {
                groupRuntimeState = this.groupStates.computeIfAbsent(pointGroupDefinition.id(), string -> new GroupRuntimeState());
                groupRuntimeState.markChecked(localDateTime);
            }
            List<PointRecord> list2;
            try {
                list2 = groupPointFetcher.fetch(pointGroupDefinition);
            }
            catch (Exception exception) {
                GroupEvaluation groupEvaluation2;
                ++n2;
                object = ShelfPointMonitorApp.queryFailureMessage(exception);
                Object object2 = this.groupMonitorLock;
                synchronized (object2) {
                    groupEvaluation2 = GroupMonitorLogic.queryFailed(pointGroupDefinition, groupRuntimeState, localDateTime, (String)object);
                }
                this.appendCheckLog(localDateTime, groupEvaluation2);
                this.appendGroupEvents(localDateTime, groupEvaluation2);
                arrayList.add(groupEvaluation2);
                this.recordLatestEvaluation(groupEvaluation2);
                this.closeActiveGroupAlertDialogIfOwnedBy(pointGroupDefinition.id());
                this.updateSelectedGroupBoard(groupEvaluation2);
                stringBuilder.append(this.formatGroupCheckResult(string2, List.of(), groupEvaluation2)).append(System.lineSeparator());
                this.appendStatus(string2 + "\u5931\u8d25\uff0c\u70b9\u4f4d\u7ec4 " + pointGroupDefinition.id() + " " + (String)object);
                continue;
            }
            object = this.groupMonitorLock;
            synchronized (object) {
                groupEvaluation = GroupMonitorLogic.evaluate(pointGroupDefinition, list2, groupRuntimeState, localDateTime);
            }
            this.appendCheckLog(localDateTime, groupEvaluation);
            this.appendGroupEvents(localDateTime, groupEvaluation);
            arrayList.add(groupEvaluation);
            this.recordLatestEvaluation(groupEvaluation);
            this.updateSelectedGroupBoard(groupEvaluation);
            stringBuilder.append(this.formatGroupCheckResult(string2, list2, groupEvaluation)).append(System.lineSeparator());
            ++n;
            if (!groupEvaluation.shouldShowDialog() || bl) continue;
            if (this.uiPreferences.alertPopupEnabled()) {
                this.showGroupAlertDialog(groupEvaluation);
                bl = true;
                continue;
            }
            this.appendStatus("\u62a5\u8b66\u5f39\u7a97\u5df2\u5728\u7cfb\u7edf\u8bbe\u7f6e\u4e2d\u5173\u95ed\uff0c\u672c\u6b21\u53ea\u8bb0\u5f55\u72b6\u6001\u548c\u4e8b\u4ef6\uff1a" + groupEvaluation.groupId());
        }
        String string3 = stringBuilder.toString();
        SwingUtilities.invokeLater(() -> {
            this.groupRuntimeArea.setText(string3);
            this.lastCheckLabel.setText("\u4e0a\u6b21\u68c0\u6d4b\uff1a" + TIME_FORMAT.format(localDateTime));
            this.nextCheckLabel.setText((String)(this.scheduledTask == null ? "\u4e0b\u6b21\u68c0\u6d4b\uff1a--" : "\u4e0b\u6b21\u68c0\u6d4b\uff1a" + TIME_FORMAT.format(localDateTime.plusSeconds(10L))));
            this.refreshOverviewPage();
            this.refreshAlertCenterPage();
        });
        this.appendStatus(string2 + "\u5b8c\u6210\uff0c\u70b9\u4f4d\u7ec4 " + n + " \u4e2a" + (String)(n2 > 0 ? "\uff0c\u5931\u8d25 " + n2 + " \u4e2a" : "") + "\u3002");
        this.latestDetectionHealth = n2 > 0 ? "\u67e5\u8be2\u5931\u8d25" : "\u6210\u529f";
        this.refreshSystemHealthStatus();
        return new GroupCheckRunResult(n, n2, bl, arrayList);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void recordLatestEvaluation(GroupEvaluation groupEvaluation) {
        Object object = this.groupMonitorLock;
        synchronized (object) {
            this.lastGroupEvaluations.put(groupEvaluation.groupId(), groupEvaluation);
        }
    }

    private void updateSelectedGroupBoard(GroupEvaluation groupEvaluation) {
        SwingUtilities.invokeLater(() -> {
            if (!groupEvaluation.groupId().equals(this.selectedGroupId())) {
                return;
            }
            String string = groupEvaluation.message();
            if (string == null || string.isBlank()) {
                string = GroupStatusText.statusText(groupEvaluation.status());
            }
            this.groupSummaryLabel.setText("\u5f53\u524d\u5224\u65ad\uff1a" + string);
            this.renderPointStatusBoard(groupEvaluation);
        });
    }

    private String selectedGroupId() {
        int n = this.groupList.getSelectedIndex();
        if (n < 0 || n >= this.pointGroups.size()) {
            return "";
        }
        return this.pointGroups.get(n).id();
    }

    private List<PointDefinition> pointDefinitions(PointGroupDefinition pointGroupDefinition) {
        ArrayList<PointDefinition> arrayList = new ArrayList<PointDefinition>();
        for (GroupMonitorPoint groupMonitorPoint : pointGroupDefinition.points()) {
            if (!groupMonitorPoint.enabled()) continue;
            arrayList.add(new PointDefinition(groupMonitorPoint.code(), groupMonitorPoint.alias()));
        }
        return arrayList;
    }

    private void appendCheckLog(LocalDateTime localDateTime, GroupEvaluation groupEvaluation) {
        try {
            this.groupLogWriter.appendCheck(localDateTime, groupEvaluation);
        }
        catch (Exception exception) {
            this.appendStatus("CSV\u68c0\u6d4b\u65e5\u5fd7\u5199\u5165\u5931\u8d25\uff1a" + exception.getMessage());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void appendGroupEvents(LocalDateTime localDateTime, GroupEvaluation groupEvaluation) {
        GroupAlertStatus groupAlertStatus;
        Object object = this.groupMonitorLock;
        synchronized (object) {
            groupAlertStatus = this.lastGroupStatuses.getOrDefault(groupEvaluation.groupId(), GroupAlertStatus.NORMAL);
            this.lastGroupStatuses.put(groupEvaluation.groupId(), groupEvaluation.status());
        }
        try {
            if (groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED) {
                if (groupAlertStatus != GroupAlertStatus.QUERY_FAILED) {
                    this.groupLogWriter.appendEvent(localDateTime, "QUERY_FAILED", groupEvaluation);
                }
            } else {
                if (groupAlertStatus == GroupAlertStatus.QUERY_FAILED) {
                    this.groupLogWriter.appendEvent(localDateTime, "QUERY_RECOVERED", groupEvaluation);
                }
                if (groupEvaluation.status() == GroupAlertStatus.ACTIVE_ALERT && groupAlertStatus != GroupAlertStatus.ACTIVE_ALERT && groupAlertStatus != GroupAlertStatus.ACKED_ALERT) {
                    this.groupLogWriter.appendEvent(localDateTime, "ALERT_OPEN", groupEvaluation);
                } else if (groupEvaluation.status() == GroupAlertStatus.NORMAL && groupAlertStatus != GroupAlertStatus.NORMAL && groupAlertStatus != GroupAlertStatus.QUERY_FAILED) {
                    this.groupLogWriter.appendEvent(localDateTime, "RECOVERED", groupEvaluation);
                }
            }
        }
        catch (Exception exception) {
            this.appendStatus("CSV\u4e8b\u4ef6\u65e5\u5fd7\u5199\u5165\u5931\u8d25\uff1a" + exception.getMessage());
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
            JPanel jPanel = new JPanel(new GridLayout(0, 1, 4, 4));
            jPanel.setBackground(new Color(255, 250, 230));
            jPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(190, 120, 30), 2), BorderFactory.createEmptyBorder(12, 12, 12, 12)));
            JLabel jLabel = new JLabel("\u67e5\u8be2\u5931\u8d25");
            jLabel.setFont(new Font("SansSerif", 1, 22));
            jLabel.setForeground(new Color(160, 92, 20));
            jPanel.add(jLabel);
            jPanel.add(new JLabel(groupEvaluation.message()));
            jPanel.add(new JLabel("\u672c\u6b21\u672a\u83b7\u5f97\u70b9\u4f4d\u72b6\u6001\uff0c\u4e0d\u6309\u65e0\u6599\u5904\u7406\u3002"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 4;
            this.pointStatusPanel.add((Component)jPanel, gridBagConstraints);
            this.pointStatusPanel.revalidate();
            this.pointStatusPanel.repaint();
            return;
        }
        ArrayList<PointStatusView> arrayList = new ArrayList<PointStatusView>();
        ArrayList<PointStatusView> arrayList2 = new ArrayList<PointStatusView>();
        for (PointStatusView object : groupEvaluation.pointStatuses()) {
            if (object.role() == PointRole.USE) {
                arrayList.add(object);
                continue;
            }
            arrayList2.add(object);
        }
        int n = 0;
        for (Object object : arrayList) {
            Object object2 = this.pointStatusCard((PointStatusView)object);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = n++;
            gridBagConstraints.gridwidth = 4;
            this.pointStatusPanel.add((Component)object2, gridBagConstraints);
        }
        gridBagConstraints.gridwidth = 1;
        boolean bl = false;
        int n2 = 0;
        for (Object object2 : arrayList2) {
            JPanel jPanel = this.pointStatusCard((PointStatusView)object2);
            gridBagConstraints.gridx = n2++;
            gridBagConstraints.gridy = n++;
            this.pointStatusPanel.add((Component)jPanel, gridBagConstraints);
            if (n2 < 4) continue;
            n2 = 0;
        }
        this.pointStatusPanel.revalidate();
        this.pointStatusPanel.repaint();
    }

    private JPanel pointStatusCard(PointStatusView pointStatusView) {
        JPanel jPanel = new JPanel(new GridLayout(0, 1, 4, 4));
        Color color = this.statusColor(pointStatusView.status());
        jPanel.setBackground(Color.WHITE);
        jPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, 2), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel jLabel = new JLabel((pointStatusView.role() == PointRole.USE ? "\u4f7f\u7528\u4f4d\uff1a" : "\u5907\u7528\u4f4d\uff1a") + pointStatusView.alias());
        jLabel.setFont(new Font("SansSerif", 1, pointStatusView.role() == PointRole.USE ? 16 : 14));
        JLabel jLabel2 = new JLabel(pointStatusView.statusText());
        jLabel2.setFont(new Font("SansSerif", 1, pointStatusView.role() == PointRole.USE ? 26 : 22));
        jLabel2.setForeground(color);
        String string = pointStatusView.shelfCode() == null || pointStatusView.shelfCode().isBlank() ? "--" : pointStatusView.shelfCode();
        String string2 = pointStatusView.reason() == null || pointStatusView.reason().isBlank() ? "--" : pointStatusView.reason();
        jPanel.add(jLabel);
        jPanel.add(jLabel2);
        jPanel.add(new JLabel("\u70b9\u4f4d\uff1a" + pointStatusView.pointCode()));
        jPanel.add(new JLabel("\u8d27\u67b6\uff1a" + string));
        jPanel.add(new JLabel("\u539f\u56e0\uff1a" + string2));
        return jPanel;
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

    private String formatGroupCheckResult(String string, List<PointRecord> list, GroupEvaluation groupEvaluation) {
        String string2 = groupEvaluation.status() == GroupAlertStatus.QUERY_FAILED && groupEvaluation.message() != null && !groupEvaluation.message().isBlank() ? groupEvaluation.message() : GroupStatusText.summary(groupEvaluation.areaName(), groupEvaluation.groupName(), groupEvaluation.materialName(), groupEvaluation.status(), groupEvaluation.usePointEmpty(), groupEvaluation.backupTotal(), groupEvaluation.backupAvailableCount(), groupEvaluation.continuousMatchedSeconds(), groupEvaluation.alertDurationSeconds(), groupEvaluation.pointStatuses());
        return TIME_FORMAT.format(LocalDateTime.now()) + " " + GroupStatusText.statusText(groupEvaluation.status()) + "\uff1a" + string2;
    }

    private static String queryFailureMessage(Exception exception) {
        return "\u67e5\u8be2\u5931\u8d25\uff1a" + ShelfPointMonitorApp.sanitizedExceptionSummary(exception);
    }

    private static String sanitizedExceptionSummary(Exception exception) {
        Object object;
        Object object2 = object = exception == null ? "" : exception.getMessage();
        if (object == null || ((String)object).isBlank()) {
            object = exception == null ? "\u672a\u77e5\u9519\u8bef" : exception.getClass().getSimpleName();
        } else if (exception != null) {
            object = exception.getClass().getSimpleName() + ": " + (String)object;
        }
        Object object3 = ((String)object).replace('\r', ' ').replace('\n', ' ');
        object3 = ((String)object3).replaceAll("(?i)jdbc:[^\\s,;]+", "jdbc:***");
        object3 = ((String)object3).replaceAll("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "***.***.***.***");
        object3 = ((String)object3).replaceAll("(?i)((?:password|passwd|pwd|token|access_token|secret)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        object3 = ((String)object3).replaceAll("(?i)((?:user\\s*id|uid|user(?:name)?|databaseUser|db_user|role)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        object3 = ((String)object3).replaceAll("(?i)((?:host|port)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        object3 = ((String)object3).replaceAll("(?i)user\\s+\"[^\"]+\"", "user ***");
        object3 = ((String)object3).replaceAll("(?i)user\\s+'[^']+'", "user ***");
        object3 = ((String)object3).replaceAll("(?i)for\\s+user\\s+\"[^\"]+\"", "for user ***");
        object3 = ((String)object3).replaceAll("(?i)for\\s+user\\s+'[^']+'", "for user ***");
        object3 = ((String)object3).replaceAll("(?i)role\\s+\"[^\"]+\"", "role ***");
        object3 = ((String)object3).replaceAll("(?i)role\\s+'[^']+'", "role ***");
        object3 = ((String)object3).replaceAll("\\bat\\s+[A-Za-z0-9_.$]+\\([^)]*\\)", "");
        object3 = ((String)object3).replaceAll("[A-Za-z0-9_.$-]+\\.java:\\d+", "***.java:***");
        object3 = ((String)object3).replaceAll("\\s+", " ").trim();
        if (((String)object3).length() > 180) {
            object3 = ((String)object3).substring(0, 177) + "...";
        }
        return ((String)object3).isBlank() ? "\u672a\u77e5\u9519\u8bef" : (String)object3;
    }

    private void closeActiveGroupAlertDialogIfOwnedBy(String string) {
        SwingUtilities.invokeLater(() -> {
            if (this.activeDialog == null || string == null || !string.equals(this.activeDialogGroupId)) {
                return;
            }
            this.activeDialog.dispose();
            this.activeDialog = null;
            this.activeDialogGroupId = "";
        });
    }

    private void startMonitoring() {
        try {
            DbConfig dbConfig = this.requireCurrentConfig(60);
            List<PointGroupDefinition> list = this.readGroups();
            this.groupConfigStore.save(list);
            this.stopMonitoring();
            this.captureMonitoredGroups(list);
            this.clearGroupMonitorState();
            this.scheduledTask = this.executor.scheduleWithFixedDelay(() -> this.runWithUiErrorHandling(this::checkDueGroups), 0L, 10L, TimeUnit.SECONDS);
            this.startButton.setEnabled(false);
            this.stopButton.setEnabled(true);
            this.monitorStatusLabel.setText("\u76d1\u63a7\u72b6\u6001\uff1a\u8fd0\u884c\u4e2d");
            this.nextCheckLabel.setText("\u4e0b\u6b21\u68c0\u6d4b\uff1a" + TIME_FORMAT.format(LocalDateTime.now().plusSeconds(10L)));
            this.appendStatus("\u5df2\u5f00\u59cb\u70b9\u4f4d\u7ec4\u76d1\u63a7\u3002\u7cfb\u7edf\u6bcf 10 \u79d2\u626b\u63cf\u5230\u671f\u70b9\u4f4d\u7ec4\uff0c\u5404\u7ec4\u6309\u81ea\u8eab\u68c0\u6d4b\u5468\u671f\u67e5\u8be2\u6570\u636e\u5e93\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void stopMonitoring() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel(false);
            this.scheduledTask = null;
        }
        this.monitoredGroups = List.of();
        this.clearGroupMonitorState();
        this.startButton.setEnabled(true);
        this.stopButton.setEnabled(false);
        this.monitorStatusLabel.setText("\u76d1\u63a7\u72b6\u6001\uff1a\u672a\u8fd0\u884c");
        this.nextCheckLabel.setText("\u4e0b\u6b21\u68c0\u6d4b\uff1a--");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void clearGroupMonitorState() {
        Object object = this.groupMonitorLock;
        synchronized (object) {
            this.groupStates.clear();
            this.lastGroupStatuses.clear();
            this.lastGroupEvaluations.clear();
        }
        this.refreshOverviewPage();
        this.refreshAlertCenterPage();
    }

    private void showGroupAlertDialog(GroupEvaluation groupEvaluation) {
        SwingUtilities.invokeLater(() -> {
            if (this.activeDialog != null && this.activeDialog.isShowing()) {
                return;
            }
            if (this.uiPreferences.alertSoundEnabled()) {
                Toolkit.getDefaultToolkit().beep();
            }
            JDialog jDialog = new JDialog(this, "\u70b9\u4f4d\u7ec4\u7f3a\u6599\u62a5\u8b66", false);
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
            jDialog.add((Component)this.buildGroupAlertButtons(groupEvaluation, () -> {
                jDialog.dispose();
                this.activeDialog = null;
                this.activeDialogGroupId = "";
            }), "South");
            jDialog.setSize(640, 420);
            jDialog.setLocationRelativeTo(this);
            this.activeDialog = jDialog;
            this.activeDialogGroupId = groupEvaluation.groupId();
            jDialog.setVisible(true);
        });
    }

    private String groupAlertText(GroupEvaluation groupEvaluation) {
        String string = System.lineSeparator();
        return "\u68c0\u6d4b\u65f6\u95f4\uff1a" + TIME_FORMAT.format(LocalDateTime.now()) + string + groupEvaluation.areaName() + " / " + groupEvaluation.groupName() + " " + this.alertHeadlineStatusText(groupEvaluation.status()) + string + "\u7269\u6599\uff1a" + groupEvaluation.materialName() + string + "\u4f7f\u7528\u4f4d\uff1a" + (groupEvaluation.usePointEmpty() ? "\u65e0\u6599" : "\u6709\u6599") + string + "\u5907\u7528\u4f4d\uff1a" + groupEvaluation.backupAvailableCount() + "/" + groupEvaluation.backupTotal() + " \u6709\u6599" + string + "\u6301\u7eed\uff1a" + groupEvaluation.continuousMatchedMinutes() + " \u5206\u949f" + string + string + "\u5f02\u5e38\u70b9\u4f4d\u5217\u8868\uff1a" + string + this.abnormalPointText(groupEvaluation) + string + string + "\u62a5\u8b66\u6761\u4ef6\u5df2\u8fbe\u5230\u62a5\u8b66\u65f6\u95f4\uff0c\u8bf7\u73b0\u573a\u786e\u8ba4\u8865\u6599\u6216\u8c03\u5ea6\u72b6\u6001\u3002";
    }

    private String abnormalPointText(GroupEvaluation groupEvaluation) {
        StringBuilder stringBuilder = new StringBuilder();
        for (PointStatusView pointStatusView : groupEvaluation.pointStatuses()) {
            if (pointStatusView.status() != PointMaterialStatus.EMPTY && pointStatusView.status() != PointMaterialStatus.MISSING) continue;
            if (stringBuilder.length() > 0) {
                stringBuilder.append(System.lineSeparator());
            }
            stringBuilder.append(this.roleText(pointStatusView.role())).append(" ").append(pointStatusView.pointCode()).append(" ").append(pointStatusView.statusText()).append(" \u539f\u56e0\uff1a").append(pointStatusView.reason() == null || pointStatusView.reason().isBlank() ? "\u672a\u586b\u5199\u539f\u56e0" : pointStatusView.reason());
        }
        if (stringBuilder.length() == 0) {
            return "\u65e0\u5f02\u5e38\u70b9\u4f4d\u660e\u7ec6";
        }
        return stringBuilder.toString();
    }

    private String alertHeadlineStatusText(GroupAlertStatus groupAlertStatus) {
        if (groupAlertStatus == GroupAlertStatus.ACTIVE_ALERT) {
            return "\u9700\u8981\u5173\u6ce8";
        }
        return GroupStatusText.statusText(groupAlertStatus);
    }

    private JPanel buildGroupAlertButtons(GroupEvaluation groupEvaluation, Runnable runnable) {
        return this.buildGroupAlertButtons(groupEvaluation, this::openLogs, runnable);
    }

    JPanel buildGroupAlertButtons(GroupEvaluation groupEvaluation, Runnable runnable, Runnable runnable2) {
        JButton jButton = new JButton("\u6253\u5f00\u65e5\u5fd7\u76ee\u5f55");
        jButton.addActionListener(actionEvent -> {
            if (runnable != null) {
                runnable.run();
            }
        });
        JButton jButton2 = new JButton("\u5df2\u5173\u6ce8");
        jButton2.addActionListener(actionEvent -> {
            this.acknowledgeGroupAlert(groupEvaluation);
            if (runnable2 != null) {
                runnable2.run();
            }
        });
        JPanel jPanel = new JPanel(new FlowLayout(2));
        jPanel.add(jButton);
        jPanel.add(jButton2);
        return jPanel;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void acknowledgeGroupAlert(GroupEvaluation groupEvaluation) {
        Object object = this.groupMonitorLock;
        synchronized (object) {
            GroupRuntimeState groupRuntimeState = this.groupStates.get(groupEvaluation.groupId());
            if (groupRuntimeState != null) {
                groupRuntimeState.acknowledge();
            }
            this.lastGroupStatuses.put(groupEvaluation.groupId(), GroupAlertStatus.ACKED_ALERT);
        }
        try {
            this.groupLogWriter.appendEvent(LocalDateTime.now(), "ACKNOWLEDGED", groupEvaluation);
        }
        catch (Exception exception) {
            this.appendStatus("CSV\u4e8b\u4ef6\u65e5\u5fd7\u5199\u5165\u5931\u8d25\uff1a" + exception.getMessage());
        }
        this.appendStatus("\u7528\u6237\u5df2\u5173\u6ce8\u70b9\u4f4d\u7ec4\u62a5\u8b66\uff1a" + groupEvaluation.groupId());
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
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        this.overviewModel.setRowCount(0);
        this.overviewRowGroupIds.clear();
        for (PointGroupDefinition pointGroupDefinition2 : arrayList) {
            GroupEvaluation groupEvaluation = this.latestEvaluation(pointGroupDefinition2.id());
            GroupAlertStatus groupAlertStatus = this.groupStatus(pointGroupDefinition2);
            if (groupAlertStatus == GroupAlertStatus.ACTIVE_ALERT || groupAlertStatus == GroupAlertStatus.ACKED_ALERT) {
                ++n;
            } else if (groupAlertStatus == GroupAlertStatus.PENDING_ALERT) {
                ++n2;
            } else if (groupAlertStatus == GroupAlertStatus.QUERY_FAILED) {
                ++n3;
            }
            this.overviewModel.addRow(new Object[]{this.statusTextForOverview(pointGroupDefinition2, groupAlertStatus), pointGroupDefinition2.areaName(), pointGroupDefinition2.groupName(), this.useStateText(groupEvaluation), this.backupStateText(groupEvaluation, pointGroupDefinition2), this.durationText(groupEvaluation), this.lastCheckedText(pointGroupDefinition2.id())});
            this.overviewRowGroupIds.add(pointGroupDefinition2.id());
        }
        this.overviewGroupCountLabel.setText(String.valueOf(arrayList.size()));
        this.overviewAlertCountLabel.setText(String.valueOf(n));
        this.overviewPendingCountLabel.setText(String.valueOf(n2));
        this.overviewDataErrorCountLabel.setText(String.valueOf(n3));
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private GroupEvaluation latestEvaluation(String string) {
        Object object = this.groupMonitorLock;
        synchronized (object) {
            return this.lastGroupEvaluations.get(string);
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
            return "\u67e5\u8be2\u5931\u8d25\uff0c\u6570\u636e\u4e0d\u53ef\u7528";
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
            int n = 0;
            for (GroupMonitorPoint groupMonitorPoint : pointGroupDefinition.points()) {
                if (!groupMonitorPoint.enabled() || groupMonitorPoint.role() != PointRole.BACKUP) continue;
                ++n;
            }
            return "--/" + n;
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private String lastCheckedText(String string) {
        Object object = this.groupMonitorLock;
        synchronized (object) {
            GroupRuntimeState groupRuntimeState = this.groupStates.get(string);
            if (groupRuntimeState == null || groupRuntimeState.lastCheckedAt() == null) {
                return "--";
            }
            return TIME_FORMAT.format(groupRuntimeState.lastCheckedAt());
        }
    }

    private void updateOverviewDetail() {
        int n = this.overviewTable.getSelectedRow();
        if (n < 0) {
            this.overviewDetailArea.setText("\u8bf7\u9009\u62e9\u70b9\u4f4d\u7ec4\u3002");
            return;
        }
        PointGroupDefinition pointGroupDefinition = this.findGroupByOverviewRow(n);
        if (pointGroupDefinition == null) {
            this.overviewDetailArea.setText("\u672a\u627e\u5230\u5bf9\u5e94\u70b9\u4f4d\u7ec4\u3002");
            return;
        }
        GroupEvaluation groupEvaluation = this.latestEvaluation(pointGroupDefinition.id());
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\u533a\u57df / \u7269\u6599\u7ec4\uff1a").append(pointGroupDefinition.areaName()).append(" / ").append(pointGroupDefinition.groupName()).append(System.lineSeparator());
        stringBuilder.append("\u5f53\u524d\u72b6\u6001\uff1a").append(this.statusTextForOverview(pointGroupDefinition, this.groupStatus(pointGroupDefinition))).append(System.lineSeparator());
        stringBuilder.append("\u89c4\u5219\u6458\u8981\uff1a\u4f7f\u7528\u4f4d\u65e0\u8d27\u67b6=").append(pointGroupDefinition.rule().requireUsePointEmpty() ? "\u662f" : "\u5426").append("\uff0c\u6700\u5c11\u5907\u7528\u4f4d\u6709\u6599 ").append(pointGroupDefinition.rule().minBackupAvailable()).append("\uff0c\u6301\u7eed ").append(pointGroupDefinition.rule().durationMinutes()).append(" \u5206\u949f").append(System.lineSeparator());
        stringBuilder.append("\u6301\u7eed\u65f6\u95f4\uff1a").append(this.durationText(groupEvaluation)).append(System.lineSeparator());
        stringBuilder.append("\u4e0a\u6b21\u68c0\u6d4b\uff1a").append(this.lastCheckedText(pointGroupDefinition.id())).append(System.lineSeparator());
        stringBuilder.append(System.lineSeparator()).append("\u70b9\u4f4d\u660e\u7ec6\uff1a").append(System.lineSeparator());
        if (groupEvaluation != null && !groupEvaluation.pointStatuses().isEmpty()) {
            for (PointStatusView pointStatusView : groupEvaluation.pointStatuses()) {
                stringBuilder.append(this.roleText(pointStatusView.role())).append(" / ").append(pointStatusView.alias()).append(" / ").append(pointStatusView.pointCode()).append(" / ").append(pointStatusView.statusText()).append(" / ").append(pointStatusView.updatedAt() == null ? "--" : TIME_FORMAT.format(pointStatusView.updatedAt())).append(System.lineSeparator());
            }
        } else {
            for (GroupMonitorPoint groupMonitorPoint : pointGroupDefinition.points()) {
                stringBuilder.append(this.roleText(groupMonitorPoint.role())).append(" / ").append(groupMonitorPoint.alias()).append(" / ").append(groupMonitorPoint.code()).append(" / \u672a\u68c0\u6d4b / --").append(System.lineSeparator());
            }
        }
        this.overviewDetailArea.setText(stringBuilder.toString());
    }

    private PointGroupDefinition findGroupByOverviewRow(int n) {
        int n2 = this.overviewTable.convertRowIndexToModel(n);
        if (n2 >= 0 && n2 < this.overviewRowGroupIds.size()) {
            return this.findGroupById(this.overviewRowGroupIds.get(n2));
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
        String string = String.valueOf(this.alertCenterFilterBox.getSelectedItem());
        this.alertCenterModel.setRowCount(0);
        this.alertCenterEntries = List.of();
        if (this.isRecoveredAlertFilter(string)) {
            this.runOnceInBackground(() -> {
                List<AlertCenterEntry> list = this.recoveredAlertCenterEntries();
                SwingUtilities.invokeLater(() -> this.applyAlertCenterEntries(list));
            });
            this.updateAlertCenterDetail();
            return;
        }
        this.applyAlertCenterEntries(this.liveAlertCenterEntries(string));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private List<AlertCenterEntry> liveAlertCenterEntries(String string) {
        ArrayList<AlertCenterEntry> arrayList = new ArrayList<AlertCenterEntry>();
        Object object = this.groupMonitorLock;
        synchronized (object) {
            for (GroupEvaluation groupEvaluation : this.lastGroupEvaluations.values()) {
                if (!this.matchesAlertFilter(string, groupEvaluation.status())) continue;
                arrayList.add(new AlertCenterEntry(groupEvaluation.groupId(), groupEvaluation.areaName(), groupEvaluation.groupName(), "LIVE", groupEvaluation.status(), this.lastCheckedText(groupEvaluation.groupId()), "\u5f53\u524d\u8fd0\u884c\u72b6\u6001", this.alertReason(groupEvaluation), groupEvaluation));
            }
        }
        return arrayList;
    }

    private List<AlertCenterEntry> recoveredAlertCenterEntries() {
        Path path = Paths.get("logs", "event-log.csv");
        if (!Files.exists(path, new LinkOption[0])) {
            return List.of();
        }
        ArrayList<AlertCenterEntry> arrayList = new ArrayList<AlertCenterEntry>();
        try {
            for (String string : ShelfPointMonitorApp.tailLines(path, 1000)) {
                String string2;
                List<String> list;
                if (string.startsWith("event_at,") || (list = ShelfPointMonitorApp.parseCsvLine(string)).size() < 8 || !"RECOVERED".equals(string2 = list.get(1))) continue;
                arrayList.add(new AlertCenterEntry(list.get(2), list.get(3), list.get(4), string2, this.parseStatus(list.get(6)), list.get(0), "event-log.csv", this.eventTypeText(string2) + "\uff1a" + ShelfPointMonitorApp.sanitizeVisibleLog(list.get(7)), null));
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

    private boolean isRecoveredAlertFilter(String string) {
        return "\u5df2\u6062\u590d".equals(string) || "\u5bb8\u53c9\u4eee\u6fb6?".equals(string);
    }

    private boolean matchesAlertFilter(String string, GroupAlertStatus groupAlertStatus) {
        if ("\u6d3b\u8dc3\u62a5\u8b66".equals(string)) {
            return groupAlertStatus == GroupAlertStatus.ACTIVE_ALERT;
        }
        if ("\u5df2\u5173\u6ce8".equals(string)) {
            return groupAlertStatus == GroupAlertStatus.ACKED_ALERT;
        }
        if ("\u89c2\u5bdf\u4e2d".equals(string)) {
            return groupAlertStatus == GroupAlertStatus.PENDING_ALERT;
        }
        if ("\u67e5\u8be2\u5931\u8d25".equals(string)) {
            return groupAlertStatus == GroupAlertStatus.QUERY_FAILED;
        }
        return !"\u5df2\u6062\u590d".equals(string);
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

    private String safeMessage(String string) {
        return string == null || string.isBlank() ? GroupStatusText.statusText(GroupAlertStatus.NORMAL) : string;
    }

    private GroupAlertStatus parseStatus(String string) {
        try {
            return GroupAlertStatus.valueOf(string);
        }
        catch (Exception exception) {
            return GroupAlertStatus.NORMAL;
        }
    }

    private void updateAlertCenterDetail() {
        int n = this.alertCenterTable.getSelectedRow();
        if (n < 0) {
            this.alertCenterDetailArea.setText("\u5f53\u524d\u65e0\u62a5\u8b66\u4e8b\u4ef6\u3002\u67e5\u8be2\u5931\u8d25\u5c5e\u4e8e\u7cfb\u7edf\u6570\u636e\u5f02\u5e38\uff0c\u4e0d\u5c5e\u4e8e\u7f3a\u6599\u62a5\u8b66\u3002");
            return;
        }
        int n2 = this.alertCenterTable.convertRowIndexToModel(n);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\u5f53\u524d\u72b6\u6001\uff1a").append(this.alertCenterModel.getValueAt(n2, 5)).append(System.lineSeparator());
        stringBuilder.append("\u89e6\u53d1\u65f6\u95f4\uff1a").append(this.alertCenterModel.getValueAt(n2, 0)).append(System.lineSeparator());
        stringBuilder.append("\u6301\u7eed\u65f6\u95f4\uff1a").append(this.alertCenterModel.getValueAt(n2, 4)).append(System.lineSeparator());
        stringBuilder.append("\u89c4\u5219\u547d\u4e2d\u5185\u5bb9\uff1a").append(this.alertCenterModel.getValueAt(n2, 3)).append(System.lineSeparator());
        stringBuilder.append("\u70b9\u4f4d\u660e\u7ec6\uff1a\u8bf7\u5728\u76d1\u63a7\u603b\u89c8\u67e5\u770b\u5f53\u524d\u70b9\u4f4d\u72b6\u6001\u3002").append(System.lineSeparator());
        stringBuilder.append("\u5904\u7406\u65f6\u95f4\u7ebf\uff1a\u4e8b\u4ef6\u6765\u81ea\u672c\u5730 event-log.csv \u548c\u672c\u6b21\u8fd0\u884c\u68c0\u6d4b\u7ed3\u679c\u3002");
        this.alertCenterDetailArea.setText(stringBuilder.toString());
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
            char[] cArray = Arrays.copyOf(this.currentPassword, this.currentPassword.length);
            this.runOnceInBackground(() -> this.checkGroups(dbConfig, cArray, List.of(pointGroupDefinition), LocalDateTime.now(), "\u624b\u52a8\u68c0\u6d4b"));
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
        int n = this.alertCenterTable.getSelectedRow();
        if (n < 0) {
            return null;
        }
        int n2 = this.alertCenterTable.convertRowIndexToModel(n);
        if (n2 >= 0 && n2 < this.alertCenterEntries.size()) {
            return this.alertCenterEntries.get(n2);
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

    private void selectOverviewGroup(String string) {
        this.refreshOverviewPage();
        for (int i = 0; i < this.overviewRowGroupIds.size(); ++i) {
            if (!this.overviewRowGroupIds.get(i).equals(string)) continue;
            int n = this.overviewTable.convertRowIndexToView(i);
            if (n >= 0) {
                this.overviewTable.setRowSelectionInterval(n, n);
                this.overviewTable.scrollRectToVisible(this.overviewTable.getCellRect(n, 0, true));
            }
            return;
        }
    }

    private PointGroupDefinition findGroupById(String string) {
        for (PointGroupDefinition pointGroupDefinition : this.pointGroups) {
            if (!pointGroupDefinition.id().equals(string)) continue;
            return pointGroupDefinition;
        }
        return null;
    }

    private void startPointDataQuery(boolean bl) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> this.startPointDataQuery(bl));
            return;
        }
        try {
            PointDataQuery pointDataQuery;
            DbConfig dbConfig = this.requireCurrentConfig(30);
            if (bl) {
                this.queryCurrentPage = 1;
            }
            int n = (Integer)this.queryLimitSpinner.getValue();
            this.lastPointDataQuery = pointDataQuery = new PointDataQuery(this.queryPointKeywordField.getText(), this.queryShelfKeywordField.getText(), this.queryAreaCodeField.getText(), this.queryRelateAreaCodeField.getText(), this.queryUpdatedFromField.getText(), this.queryUpdatedToField.getText(), n, (this.queryCurrentPage - 1) * n);
            char[] cArray = Arrays.copyOf(this.currentPassword, this.currentPassword.length);
            this.runOnceInBackground(() -> this.executePointDataQuery(dbConfig, pointDataQuery, cArray));
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void executePointDataQuery(DbConfig dbConfig, PointDataQuery pointDataQuery, char[] cArray) throws Exception {
        try {
            PointDataQueryResult pointDataQueryResult = this.pointDataQueryRepository.query(dbConfig, cArray, pointDataQuery);
            SwingUtilities.invokeLater(() -> this.applyPointDataQueryResult(pointDataQueryResult));
        }
        finally {
            Arrays.fill(cArray, '\u0000');
        }
    }

    private void applyPointDataQueryResult(PointDataQueryResult pointDataQueryResult) {
        this.dataQueryModel.setRowCount(0);
        for (PointRecord pointRecord : pointDataQueryResult.records()) {
            this.dataQueryModel.addRow(new Object[]{pointRecord.mapDataCode(), this.blankToDash(pointRecord.podCode()), this.blankToDash(pointRecord.podStatus()), this.materialText(pointRecord), pointRecord.indLock() == 0 ? "\u672a\u9501\u5b9a" : "\u9501\u5b9a", this.blankToDash(pointRecord.areaCode()), this.blankToDash(pointRecord.relateAreaCode()), pointRecord.dateChg() == null ? "--" : TIME_FORMAT.format(pointRecord.dateChg()), "\u6210\u529f"});
        }
        this.queryTotalCount = pointDataQueryResult.totalCount();
        int n = Math.max(1, pointDataQueryResult.limit());
        int n2 = this.queryTotalCount == 0 ? 0 : (this.queryTotalCount + n - 1) / n;
        this.queryCurrentPage = n2 == 0 ? 1 : Math.min(n2, pointDataQueryResult.offset() / n + 1);
        this.queryPrevButton.setEnabled(n2 > 0 && this.queryCurrentPage > 1);
        this.queryNextButton.setEnabled(n2 > 0 && this.queryCurrentPage < n2);
        this.queryPageLabel.setText("\u7b2c " + (n2 == 0 ? 0 : this.queryCurrentPage) + " / " + n2 + " \u9875");
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

    private String blankToDash(String string) {
        return string == null || string.isBlank() ? "--" : string;
    }

    private void updateDataQueryDetail() {
        int n = this.dataQueryTable.getSelectedRow();
        if (n < 0) {
            return;
        }
        int n2 = this.dataQueryTable.convertRowIndexToModel(n);
        StringBuilder stringBuilder = new StringBuilder("\u9009\u4e2d\u8bb0\u5f55\u5b57\u6bb5\uff1a").append(System.lineSeparator());
        for (int i = 0; i < this.dataQueryModel.getColumnCount(); ++i) {
            stringBuilder.append(this.dataQueryModel.getColumnName(i)).append("\uff1a").append(this.dataQueryModel.getValueAt(n2, i)).append(System.lineSeparator());
        }
        stringBuilder.append(System.lineSeparator()).append("\u53ea\u8bfb\u67e5\u8be2").append(System.lineSeparator()).append("\u4e0d\u652f\u6301 SQL \u7f16\u8f91").append(System.lineSeparator()).append("\u4e0d\u652f\u6301\u6570\u636e\u4fee\u6539");
        this.dataQueryDetailArea.setText(stringBuilder.toString());
    }

    private void loadSystemLogs() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::loadSystemLogs);
            return;
        }
        SystemLogFilter systemLogFilter = new SystemLogFilter(String.valueOf(this.systemLogTypeFilterBox.getSelectedItem()), this.systemLogFromField.getText().trim(), this.systemLogToField.getText().trim(), this.systemLogGroupField.getText().trim(), this.systemLogKeywordField.getText().trim());
        this.runOnceInBackground(() -> {
            List<SystemLogEntry> list = this.readSystemLogEntries(systemLogFilter);
            SwingUtilities.invokeLater(() -> this.applySystemLogEntries(list));
        });
    }

    private List<SystemLogEntry> readSystemLogEntries(SystemLogFilter systemLogFilter) {
        ArrayList<SystemLogEntry> arrayList = new ArrayList<SystemLogEntry>();
        this.appendSystemLogEntries(arrayList, systemLogFilter, Paths.get("logs", "event-log.csv"), "\u4e8b\u4ef6\u65e5\u5fd7", 1000);
        this.appendSystemLogEntries(arrayList, systemLogFilter, Paths.get("logs", "check-log.csv"), "\u68c0\u6d4b\u65e5\u5fd7", 1000);
        this.appendMonitorLogEntries(arrayList, systemLogFilter);
        return arrayList;
    }

    private void appendSystemLogEntries(List<SystemLogEntry> list, SystemLogFilter systemLogFilter, Path path, String string, int n) {
        if (!Files.exists(path, new LinkOption[0])) {
            return;
        }
        try {
            for (String string2 : ShelfPointMonitorApp.tailLines(path, n)) {
                List<String> list2;
                if (string2.startsWith("event_at,") || string2.startsWith("checked_at,") || (list2 = ShelfPointMonitorApp.parseCsvLine(string2)).size() < 2) continue;
                String string3 = string.equals("\u4e8b\u4ef6\u65e5\u5fd7") ? this.eventTypeText(list2.get(1)) : "\u68c0\u6d4b\u5b8c\u6210";
                String string4 = list2.size() > 2 ? list2.get(2) : "";
                String string5 = list2.isEmpty() ? "" : list2.get(list2.size() - 1);
                this.addSystemLogEntry(list, systemLogFilter, new SystemLogEntry(list2.get(0), string3, "\u4fe1\u606f", string4, ShelfPointMonitorApp.sanitizeVisibleLog(string5), string));
            }
        }
        catch (Exception exception) {
            this.addSystemLogEntry(list, systemLogFilter, new SystemLogEntry(TIME_FORMAT.format(LocalDateTime.now()), "\u65e5\u5fd7\u8bfb\u53d6\u5931\u8d25", "\u8b66\u544a", "", ShelfPointMonitorApp.sanitizedExceptionSummary(exception), string));
        }
    }

    private void appendMonitorLogEntries(List<SystemLogEntry> list, SystemLogFilter systemLogFilter) {
        if (!Files.exists(this.logPath, new LinkOption[0])) {
            return;
        }
        try {
            for (String string : ShelfPointMonitorApp.tailLines(this.logPath, 200)) {
                this.addSystemLogEntry(list, systemLogFilter, new SystemLogEntry("", "\u8fd0\u884c\u65e5\u5fd7", "\u4fe1\u606f", "", ShelfPointMonitorApp.sanitizeVisibleLog(string), "monitor.log"));
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
        String string = systemLogEntry.time + " " + systemLogEntry.type + " " + systemLogEntry.groupId + " " + systemLogEntry.description + " " + systemLogEntry.source;
        if (!systemLogFilter.keyword.isBlank() && !string.contains(systemLogFilter.keyword)) {
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

    private static List<String> tailLines(Path path, int n) throws Exception {
        ArrayDeque<String> arrayDeque = new ArrayDeque<String>();
        try (BufferedReader bufferedReader = Files.newBufferedReader(path, StandardCharsets.UTF_8);){
            String string;
            while ((string = bufferedReader.readLine()) != null) {
                arrayDeque.addLast(string);
                while (arrayDeque.size() > n) {
                    arrayDeque.removeFirst();
                }
            }
        }
        return new ArrayList<String>(arrayDeque);
    }

    private String eventTypeText(String string) {
        if ("ALERT_OPEN".equals(string)) {
            return "\u62a5\u8b66\u89e6\u53d1";
        }
        if ("ACKNOWLEDGED".equals(string)) {
            return "\u5df2\u5173\u6ce8";
        }
        if ("RECOVERED".equals(string)) {
            return "\u6062\u590d";
        }
        if ("QUERY_FAILED".equals(string)) {
            return "\u67e5\u8be2\u5931\u8d25";
        }
        if ("QUERY_RECOVERED".equals(string)) {
            return "\u67e5\u8be2\u6062\u590d";
        }
        return string == null || string.isBlank() ? "\u68c0\u6d4b\u5b8c\u6210" : string;
    }

    private void updateSystemLogDetail() {
        int n = this.systemLogTable.getSelectedRow();
        if (n < 0) {
            this.systemLogDetailArea.setText("\u6682\u65e0\u65e5\u5fd7\u8bb0\u5f55\u3002");
            return;
        }
        int n2 = this.systemLogTable.convertRowIndexToModel(n);
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < this.systemLogModel.getColumnCount(); ++i) {
            stringBuilder.append(this.systemLogModel.getColumnName(i)).append("\uff1a").append(this.systemLogModel.getValueAt(n2, i)).append(System.lineSeparator());
        }
        this.systemLogDetailArea.setText(stringBuilder.toString());
    }

    private static List<String> parseCsvLine(String string) {
        ArrayList<String> arrayList = new ArrayList<String>();
        StringBuilder stringBuilder = new StringBuilder();
        boolean bl = false;
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c == '\"') {
                if (bl && i + 1 < string.length() && string.charAt(i + 1) == '\"') {
                    stringBuilder.append('\"');
                    ++i;
                    continue;
                }
                bl = !bl;
                continue;
            }
            if (c == ',' && !bl) {
                arrayList.add(stringBuilder.toString());
                stringBuilder.setLength(0);
                continue;
            }
            stringBuilder.append(c);
        }
        arrayList.add(stringBuilder.toString());
        return arrayList;
    }

    private static String sanitizeVisibleLog(String string) {
        if (string == null) {
            return "";
        }
        String string2 = string.replaceAll("(?i)jdbc:[^\\s,;]+", "jdbc:***");
        string2 = string2.replaceAll("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b", "***.***.***.***");
        string2 = string2.replaceAll("[A-Za-z]:\\\\[^\\s,;]+", "<\u672c\u5730\u8def\u5f84>");
        string2 = string2.replaceAll("(?i)((?:password|passwd|pwd|token|access_token|secret)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        string2 = string2.replaceAll("(?i)((?:user\\s*id|uid|user(?:name)?|databaseUser|db_user|role)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        string2 = string2.replaceAll("(?i)((?:host|port)\\s*[=:]\\s*)[^\\s,;]+", "$1***");
        string2 = string2.replaceAll("(?i)user\\s+\"[^\"]+\"", "user ***");
        string2 = string2.replaceAll("(?i)user\\s+'[^']+'", "user ***");
        string2 = string2.replaceAll("(?i)for\\s+user\\s+\"[^\"]+\"", "for user ***");
        string2 = string2.replaceAll("(?i)for\\s+user\\s+'[^']+'", "for user ***");
        string2 = string2.replaceAll("(?i)role\\s+\"[^\"]+\"", "role ***");
        string2 = string2.replaceAll("(?i)role\\s+'[^']+'", "role ***");
        string2 = string2.replaceAll("\\bat\\s+[A-Za-z0-9_.$]+\\([^)]*\\)", "");
        string2 = string2.replaceAll("[A-Za-z0-9_.$-]+\\.java:\\d+", "***.java:***");
        return string2.replaceAll("\\s+", " ").trim();
    }

    private void executeSelfTestFromUi() throws Exception {
        ShelfPointMonitorApp.runSelfTest(ShelfPointMonitorApp.resolveSelfTestAppRoot());
        this.latestSelfTestHealth = "\u901a\u8fc7";
        this.refreshSystemHealthStatus();
        SwingUtilities.invokeLater(() -> this.appendStatus("\u81ea\u68c0\u901a\u8fc7\u3002"));
    }

    private void exportDiagnostics() {
        try {
            Files.createDirectories(Paths.get("diagnostics", new String[0]), new FileAttribute[0]);
            Path path = Paths.get("diagnostics", "diagnostic-" + System.currentTimeMillis() + ".txt");
            String string = "\u53ea\u8bfb\u4e1a\u52a1\u6570\u636e\u5e93\u5de5\u5177\u8bca\u65ad\u4fe1\u606f" + System.lineSeparator() + "time=" + TIME_FORMAT.format(LocalDateTime.now()) + System.lineSeparator() + "monitor=" + this.monitorStatusLabel.getText() + System.lineSeparator() + "connection=" + ShelfPointMonitorApp.sanitizeVisibleLog(this.currentConnectionLabel.getText()) + System.lineSeparator() + "groups=" + this.pointGroups.size() + System.lineSeparator();
            Files.writeString(path, (CharSequence)string, StandardCharsets.UTF_8, new OpenOption[0]);
            this.appendStatus("\u8bca\u65ad\u4fe1\u606f\u5df2\u5bfc\u51fa\uff1a" + String.valueOf(path.getFileName()));
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void refreshSystemHealthStatus() {
        if (SwingUtilities.isEventDispatchThread()) {
            this.runOnceInBackground(this::refreshSystemHealthStatus);
            return;
        }
        String string = this.scheduledTask == null || this.scheduledTask.isCancelled() ? "\u672a\u8fd0\u884c" : "\u8fd0\u884c\u4e2d";
        String string2 = this.currentProfile == null ? "\u672a\u8fde\u63a5" : this.currentProfile.name();
        String string3 = this.readableStatus(Paths.get("data", "config.properties")) + " / " + this.readableStatus(Paths.get("data", "group-config.properties"));
        Path path = this.logPath.getParent();
        String string4 = Files.isDirectory(path, new LinkOption[0]) && Files.isWritable(path) ? "\u53ef\u5199" : "\u4e0d\u53ef\u5199";
        String string5 = this.latestDetectionHealth;
        String string6 = this.latestSelfTestHealth;
        SwingUtilities.invokeLater(() -> {
            this.schedulerHealthLabel.setText("\u76d1\u63a7\u8c03\u5ea6\u5668\uff1a" + string);
            this.connectionHealthLabel.setText("\u5f53\u524d\u8fde\u63a5\uff1a" + string2);
            this.detectionHealthLabel.setText("\u6700\u8fd1\u4e00\u6b21\u68c0\u6d4b\uff1a" + string5);
            this.configHealthLabel.setText("\u914d\u7f6e\u6587\u4ef6\uff1a" + ("\u6b63\u5e38 / \u6b63\u5e38".equals(string3) ? "\u6b63\u5e38" : string3));
            this.logDirHealthLabel.setText("\u65e5\u5fd7\u76ee\u5f55\uff1a" + string4);
            this.selfTestHealthLabel.setText("\u81ea\u68c0\u72b6\u6001\uff1a" + string6);
        });
    }

    private String readableStatus(Path path) {
        return !Files.exists(path, new LinkOption[0]) || Files.isReadable(path) ? "\u6b63\u5e38" : "\u8bfb\u53d6\u5931\u8d25";
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
        int n = "\u7d27\u51d1".equals(this.uiPreferences.density()) ? 22 : ("\u8212\u9002".equals(this.uiPreferences.density()) ? 32 : 26);
        for (JTable jTable : List.of(this.overviewTable, this.alertCenterTable, this.dataQueryTable, this.browserTable, this.columnTable, this.previewTable, this.groupPointTable, this.systemLogTable)) {
            jTable.setRowHeight(n);
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
            Path path2 = Paths.get("logs", new String[0]);
            if (!Files.isDirectory(path2, new LinkOption[0])) {
                return;
            }
            long l = System.currentTimeMillis() - (long)this.uiPreferences.logRetentionDays() * 24L * 60L * 60L * 1000L;
            try (Stream<Path> stream = Files.list(path2);){
                stream.forEach(path -> {
                    try {
                        if (Files.isRegularFile(path, new LinkOption[0]) && Files.getLastModifiedTime(path, new LinkOption[0]).toMillis() < l) {
                            Files.deleteIfExists(path);
                        }
                    }
                    catch (Exception exception) {
                        // empty catch block
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
            String string = this.cellText(i, 0);
            String string2 = this.cellText(i, 1);
            String string3 = this.cellText(i, 2);
            if (string.isEmpty() && string2.isEmpty()) continue;
            if (string2.isEmpty() || string.isEmpty()) {
                throw new IllegalArgumentException("\u70b9\u4f4d\u522b\u540d\u548c\u7f16\u7801\u5fc5\u987b\u540c\u65f6\u586b\u5199");
            }
            arrayList.add(new PointDefinition(string2, string, this.parseIntervalMinutes(string3)));
        }
        if (arrayList.isEmpty()) {
            throw new IllegalArgumentException("\u81f3\u5c11\u6dfb\u52a0\u4e00\u4e2a\u70b9\u4f4d");
        }
        return arrayList;
    }

    private String cellText(int n, int n2) {
        Object object = this.pointModel.getValueAt(n, n2);
        return object == null ? "" : String.valueOf(object).trim();
    }

    private int parseIntervalMinutes(String string) {
        if (string == null || string.isBlank()) {
            return 5;
        }
        try {
            return Integer.parseInt(string.trim());
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
        List<PointDefinition> list = this.pointSchedule.forceAll(this.readPoints());
        this.checkPointsLegacy(dbConfig, list, false, LocalDateTime.now(), "\u624b\u52a8\u68c0\u6d4b");
    }

    private void checkDuePointsLegacy() throws Exception {
        LocalDateTime localDateTime;
        DbConfig dbConfig = this.requireCurrentConfig((Integer)this.intervalSpinner.getValue());
        List<PointDefinition> list = this.readPoints();
        List<PointDefinition> list2 = this.pointSchedule.duePoints(list, localDateTime = LocalDateTime.now());
        if (list2.isEmpty()) {
            return;
        }
        this.checkPointsLegacy(dbConfig, list2, true, localDateTime, "\u81ea\u52a8\u68c0\u6d4b");
    }

    private void checkPointsLegacy(DbConfig dbConfig, List<PointDefinition> list, boolean bl, LocalDateTime localDateTime, String string) throws Exception {
        List<PointRecord> list2 = this.pointRepository.fetch(dbConfig, this.currentPassword, list);
        MonitorEvaluation monitorEvaluation = MonitorLogic.evaluate(list, list2, this.alertState);
        this.appendStatus(this.formatCheckResultLegacy(list, list2, monitorEvaluation, string));
        if (bl) {
            this.pointSchedule.markChecked(list, localDateTime);
        }
        if (monitorEvaluation.hasActiveAlert()) {
            this.showAlertDialog(monitorEvaluation);
        }
    }

    private String formatCheckResultLegacy(List<PointDefinition> list, List<PointRecord> list2, MonitorEvaluation monitorEvaluation, String string) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(string).append("\u5b8c\u6210\uff0c\u68c0\u67e5\u70b9\u4f4d ").append(list.size()).append(" \u4e2a\uff0c\u8fd4\u56de\u8bb0\u5f55 ").append(list2.size()).append(" \u6761");
        if (monitorEvaluation.hasActiveAlert()) {
            stringBuilder.append("\uff0c\u53d1\u73b0\u5f02\u5e38\uff1a");
            for (PointAlert object : monitorEvaluation.alerts()) {
                stringBuilder.append(" ").append(object.alias()).append("(").append(object.code()).append(") ").append(object.message()).append(";");
            }
        } else if (monitorEvaluation.suppressedByAck()) {
            stringBuilder.append("\uff0c\u5f02\u5e38\u5df2\u5173\u6ce8\uff0c\u672c\u8f6e\u4e0d\u91cd\u590d\u5f39\u7a97");
        } else {
            stringBuilder.append("\uff0c\u65e0\u62a5\u8b66");
        }
        for (PointRecord pointRecord : list2) {
            stringBuilder.append(System.lineSeparator()).append("  ").append(pointRecord.mapDataCode()).append(" shelf_code=").append(pointRecord.podCode()).append(" status=").append(pointRecord.status()).append(" lock_state=").append(pointRecord.indLock()).append(" updated_at=").append(pointRecord.dateChg());
        }
        return stringBuilder.toString();
    }

    private void startMonitoringLegacy() {
        try {
            DbConfig dbConfig = this.requireCurrentConfig((Integer)this.intervalSpinner.getValue());
            List<PointDefinition> list = this.readPoints();
            this.configStore.save(dbConfig, list);
            this.stopMonitoringLegacy();
            this.pointSchedule.clear();
            this.scheduledTask = this.executor.scheduleWithFixedDelay(() -> this.runWithUiErrorHandling(this::checkDuePointsLegacy), 0L, dbConfig.intervalSeconds(), TimeUnit.SECONDS);
            this.startButton.setEnabled(false);
            this.stopButton.setEnabled(true);
            this.appendStatus("\u5df2\u5f00\u59cb\u76d1\u63a7\u3002\u5168\u5c40\u626b\u63cf\u95f4\u9694 " + dbConfig.intervalSeconds() + " \u79d2\uff1b\u70b9\u4f4d\u6309\u5404\u81ea\u5206\u949f\u5468\u671f\u5230\u671f\u67e5\u8be2\u3002");
        }
        catch (Exception exception) {
            this.showError(exception);
        }
    }

    private void stopMonitoringLegacy() {
        if (this.scheduledTask != null) {
            this.scheduledTask.cancel(false);
            this.scheduledTask = null;
        }
        this.pointSchedule.clear();
        this.startButton.setEnabled(true);
        this.stopButton.setEnabled(false);
    }

    private DbConfig requireCurrentConfig(int n) {
        if (this.currentProfile == null) {
            throw new IllegalStateException("\u8bf7\u5148\u5728\u201c\u8fde\u63a5\u7ba1\u7406\u201d\u4e2d\u6d4b\u8bd5\u5e76\u4f7f\u7528\u4e00\u4e2a\u8fde\u63a5");
        }
        return this.currentProfile.toDbConfig(n);
    }

    private void showAlertDialog(MonitorEvaluation monitorEvaluation) {
        SwingUtilities.invokeLater(() -> {
            if (this.activeDialog != null && this.activeDialog.isShowing()) {
                return;
            }
            JDialog jDialog = new JDialog(this, "\u70b9\u4f4d\u8d27\u67b6\u5f02\u5e38", false);
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
            JButton jButton = new JButton("\u5df2\u5173\u6ce8");
            jButton.addActionListener(actionEvent -> {
                this.alertState.acknowledge(monitorEvaluation.alertKey());
                this.appendStatus("\u7528\u6237\u5df2\u5173\u6ce8\u62a5\u8b66\uff1a" + monitorEvaluation.alertKey());
                jDialog.dispose();
                this.activeDialog = null;
                this.activeDialogGroupId = "";
            });
            JPanel jPanel = new JPanel(new FlowLayout(2));
            jPanel.add(jButton);
            jDialog.add((Component)jPanel, "South");
            jDialog.setSize(520, 320);
            jDialog.setLocationRelativeTo(this);
            this.activeDialog = jDialog;
            this.activeDialogGroupId = "";
            jDialog.setVisible(true);
        });
    }

    private String alertText(MonitorEvaluation monitorEvaluation) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\u68c0\u6d4b\u65f6\u95f4\uff1a").append(TIME_FORMAT.format(LocalDateTime.now())).append(System.lineSeparator()).append(System.lineSeparator());
        for (PointAlert pointAlert : monitorEvaluation.alerts()) {
            stringBuilder.append(pointAlert.alias()).append("\uff1a").append(pointAlert.code()).append(System.lineSeparator()).append("\u72b6\u6001\uff1a").append(pointAlert.message()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        stringBuilder.append("\u8bf7\u73b0\u573a\u786e\u8ba4\u540e\u70b9\u51fb\u201c\u5df2\u5173\u6ce8\u201d\u3002\u5728\u70b9\u4f4d\u6062\u590d\u6b63\u5e38\u524d\uff0c\u672c\u6b21\u76f8\u540c\u62a5\u8b66\u4e0d\u4f1a\u91cd\u590d\u5f39\u51fa\u3002");
        return stringBuilder.toString();
    }

    private void runOnceInBackground(CheckedRunnable checkedRunnable) {
        try {
            this.executor.submit(() -> this.runWithUiErrorHandling(checkedRunnable));
        }
        catch (RejectedExecutionException rejectedExecutionException) {
            // empty catch block
        }
    }

    private void runWithUiErrorHandling(CheckedRunnable checkedRunnable) {
        try {
            checkedRunnable.run();
        }
        catch (Exception exception) {
            SwingUtilities.invokeLater(() -> this.showError(exception));
            this.appendStatus("\u6267\u884c\u5931\u8d25\uff1a" + exception.getMessage());
        }
    }

    private void showError(Exception exception) {
        JOptionPane.showMessageDialog(this, exception.getMessage(), "\u9519\u8bef", 0);
    }

    private void appendStatus(String string) {
        String string2 = "[" + TIME_FORMAT.format(LocalDateTime.now()) + "] " + string;
        String string3 = ShelfPointMonitorApp.sanitizeVisibleLog(string2);
        SwingUtilities.invokeLater(() -> {
            this.bottomStatusLabel.setText(ShelfPointMonitorApp.sanitizeVisibleLog(string));
            this.statusArea.append(string3 + System.lineSeparator());
            this.statusArea.setCaretPosition(this.statusArea.getDocument().getLength());
            this.loadSystemLogs();
        });
        this.writeLog(string3);
    }

    private void writeLog(String string) {
        try {
            Files.createDirectories(this.logPath.getParent(), new FileAttribute[0]);
            OpenOption[] openOptionArray = new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            Files.writeString(this.logPath, (CharSequence)(string + System.lineSeparator()), StandardCharsets.UTF_8, openOptionArray);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private void openLogs() {
        try {
            Files.createDirectories(this.logPath.getParent(), new FileAttribute[0]);
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
            // empty catch block
        }
    }

    private static interface CheckedRunnable {
        public void run() throws Exception;
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

        GroupCheckRunResult(int n, int n2, boolean bl, List<GroupEvaluation> list) {
            this.checkedGroups = n;
            this.failedGroups = n2;
            this.dialogRequested = bl;
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

        AlertCenterEntry(String string, String string2, String string3, String string4, GroupAlertStatus groupAlertStatus, String string5, String string6, String string7, GroupEvaluation groupEvaluation) {
            this.groupId = string;
            this.areaName = string2;
            this.groupName = string3;
            this.eventType = string4;
            this.status = groupAlertStatus;
            this.occurredAt = string5;
            this.source = string6;
            this.description = string7;
            this.liveEvaluation = groupEvaluation;
        }
    }

    private static final class SystemLogFilter {
        private final String type;
        private final String from;
        private final String to;
        private final String groupId;
        private final String keyword;

        SystemLogFilter(String string, String string2, String string3, String string4, String string5) {
            this.type = string;
            this.from = string2;
            this.to = string3;
            this.groupId = string4;
            this.keyword = string5;
        }
    }

    private static final class SystemLogEntry {
        private final String time;
        private final String type;
        private final String level;
        private final String groupId;
        private final String description;
        private final String source;

        SystemLogEntry(String string, String string2, String string3, String string4, String string5, String string6) {
            this.time = string;
            this.type = string2;
            this.level = string3;
            this.groupId = string4;
            this.description = string5;
            this.source = string6;
        }
    }
}
