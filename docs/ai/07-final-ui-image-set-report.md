# 点位缺料报警系统 8 页高保真 UI 设计图交付报告

## 1. 本次交付

依据 `docs/superpowers/specs/2026-07-14-v050-rc1-field-delivery-design.md` 的最终八页功能，生成并保存以下 8 张独立桌面 UI 设计图：

- `design/ui/v0.5.0-rc.1/01-监控总览-1440x900.png`
- `design/ui/v0.5.0-rc.1/02-点位组管理-1440x900.png`
- `design/ui/v0.5.0-rc.1/03-报警中心-1440x900.png`
- `design/ui/v0.5.0-rc.1/04-连接管理-1440x900.png`
- `design/ui/v0.5.0-rc.1/05-数据查询-1440x900.png`
- `design/ui/v0.5.0-rc.1/06-数据源浏览器-1440x900.png`
- `design/ui/v0.5.0-rc.1/07-日志与系统-1440x900.png`
- `design/ui/v0.5.0-rc.1/08-系统设置-1440x900.png`

## 2. 设计依据与统一规则

- 统一使用 1440×900 Windows 桌面窗口比例。
- 统一保留 Windows 标题栏、顶部运行状态栏、210px 深蓝左导航、页面内容区和底部状态栏。
- 使用现代浅色企业监控风格，页面背景、卡片、表格、分栏和表单均可由 Java Swing 实现。
- 统一主色与状态色：蓝色主色、绿色正常、橙色观察中、红色缺料、紫红色查询失败、蓝灰色已关注、青色已恢复。
- `QUERY_FAILED` 始终作为独立状态展示，不解释为无料、正常或恢复。
- 连接、日志和诊断页面仅使用本地样例与脱敏文本，不展示真实 IP、账号、JDBC URL、绝对路径或堆栈位置。

## 3. 生成与后处理

- 使用内置 ImageGen 生成第一张统一母版，再以该母版为参考逐页替换中心内容和导航选中项。
- 将全部成品统一缩放到精确 1440×900 PNG。
- 对标题栏、顶部状态栏、导航品牌区和底部状态栏做统一母版合成，确保八张图的公共窗口框架一致。
- 原始生成图保留在 Codex 默认生成目录，项目只保存最终 1440×900 成品。

## 4. 验证结果

- 8 个 PNG 均验证为 `1440x900`。
- 标题栏/顶部状态栏、导航品牌区、底部状态栏与母版的平均像素差均小于 `0.01/255`，公共框架一致。
- 已逐张检查中文可读性、页面完整性、表格密度、边距、边框、状态色和底部操作区，无明显裁切、重叠或拼图式输出。
- 未运行 Java 构建：本次仅新增设计图片和交付报告，未修改 Java 源码、测试或构建脚本。

## 5. 测试与检查

- 图片尺寸检查：通过。
- 公共框架像素一致性检查：通过。
- 逐张视觉检查：通过。
- `git diff --check`：退出码 0；仅输出既有文件的 LF/CRLF 换行提示，无 whitespace error。
- `git status --short`：

  ```text
   M build.ps1
  ?? design/
  ?? docs/ai/07-final-ui-image-set-report.md
  ?? src/com/local/monitor/DiagnosticBundleTool.java
  ?? src/com/local/monitor/FieldDeploymentPreflight.java
  ?? test/com/local/monitor/FieldDeploymentPreflightTest.java
  ?? test/com/local/monitor/WindowsLauncherScriptTest.java
  ```

  本任务只新增 `design/ui/v0.5.0-rc.1/` 和本报告；`build.ps1`、上列源码与测试文件为工作区并行出现的已有内容，本任务未改动。

## 6. 剩余风险

- 设计图用于 UI 实现参考，不等同于 Swing 实机截图；最终字体度量、表格列宽和高 DPI 表现仍需在 Java Swing 中按目标 Windows 缩放比例验收。
- 图中示例数据均为本地演示数据，不代表现场生产数据。
