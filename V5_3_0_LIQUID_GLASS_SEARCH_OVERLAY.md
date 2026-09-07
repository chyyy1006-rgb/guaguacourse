# NPUCourse v5.3.0 更新报告

## 本轮完成内容

- Today 首页继续采用当前状态驱动：正在上课、下一节课、今日已结束、全天无课；新增全局搜索入口与课表/待办/考试成绩快捷入口，主状态容器接入 Liquid Glass。
- BottomBar 从纵向布局中的独立底座改为覆盖在页面内容上的悬浮层，内容可延伸到玻璃后方；Today、课表和“我的”补足可滚动的末尾空间。
- 修复底栏滑动回“今天”不提交页面切换的问题。
- 新增 `GlassTokens` / `GlassDefaults` / `LiquidGlassSurface` / `LiquidGlassCard` / `LiquidGlassDialogContainer` / `LiquidGlassButton`。
- 新增全局搜索 / Command Palette：本地实时搜索课程名称、教师、教室、备注、待办、考试、功能与设置，并执行对应导航命令。
- “我的”重分为账户与校园、学习、个性化、数据、App，分组容器使用统一玻璃组件。
- 快捷悬浮窗升级为双击、上/下/左/右滑五种独立动作，支持单独绑定、禁用和清除；加入首次教学、触感开关与更严格的防误触。

## 文件清单

### 新增

- `ui/components/LiquidGlass.kt`：可复用玻璃设计令牌与组件。
- `ui/screens/GlobalSearchPalette.kt`：全局搜索与命令面板。
- `OverlayGestureClassifierTest.kt`：四向手势和误触取消测试。
- 本报告。

### 修改

- `MainActivity.kt`：BottomBar 覆盖布局、统一导航状态、搜索命令分发、页面跳转。
- `LiquidGlassBottomBar.kt`：主题令牌、透明度和手势状态捕获修复。
- `TodayPage.kt`：玻璃主卡、搜索入口、快速入口、底部滚动空间。
- `TimetablePage.kt`：允许课程内容经过底栏并保证最后一项可滚到其上方。
- `MinePage.kt`：信息架构分组、玻璃分组卡、搜索命令直达设置。
- `QuickOverlayPreferences.kt`：五动作模型、JSON 持久化和旧配置迁移。
- `QuickOverlayService.kt`：多手势识别、方向/距离/速度判定、拖动隔离、按动作启动 App。
- `QuickOverlaySettingsPage.kt`：五动作设置、应用选择、清除/禁用、教学和触感设置。
- `AppUpdateManager.kt`、`TodayScheduleWidget.kt`：修复 minSdk 23 的既有 lint API 兼容问题。
- `app/build.gradle.kts`：版本升级到 5.3.0（versionCode 18）。

### 删除

- 无。

## 根因

### BottomBar 白色矩形底座

原结构是 `Scaffold -> Column -> 页面 Box(weight=1) -> BottomBar`。BottomBar 占有独立布局高度，页面在它上方结束，所谓透明只能透出父级背景，无法透出课程内容。v5.3 改为同一全屏 `Box` 内先画页面、再把 BottomBar 对齐到底部覆盖绘制；Scaffold 不再为底部预留不透明容器。

### 滑动回“今天”不跳转

`LiquidGlassBottomBar` 的 `pointerInput` key 只有 items 和测量数量，没有 `selectedItem`。手势协程长期捕获首次组合时的“今天”；从其他页面滑到“今天”时，代码仍认为目标与已选项相同，跳过 `onItemSelected`。现在 `selectedItem` 进入 key，手势协程随唯一导航状态更新；第 0 项与其他项完全同等处理。

## 悬浮窗迁移与设置

- Room schema 无变化，数据库版本无变化。
- SharedPreferences 增加 `gesture_actions_v2` JSON 和迁移标记。
- 首次读取新版配置时，旧 `target_package` / `target_label` 复制到“双击”动作；其余四个动作为空。旧键不删除。
- 新增设置：五个手势动作、逐动作禁用/清除、锁定位置、触感反馈、首次多手势教学；大小、透明度、贴边和暂时隐藏继续保留。

## 手工验收

1. 在“课表/学业/服务/我的”分别拖动底栏高光到“今天”，确认松手立即切页；交替点击与拖动，并快速连续操作。
2. 在课表纵向滚到末尾，确认课程块能进入底栏后方且仍可继续滚到玻璃上方；分别测试手势导航和三键导航。
3. 切换浅色、深色、动态配色，检查 BottomBar 后方内容可见、文字清晰且没有矩形底座。
4. 调整系统字体和窄屏/横屏，检查底栏、Today 快捷入口与搜索结果不崩溃。
5. 在 Today 验证上课中、下一节、课程结束和全天无课状态；检查 DDL、考试和下一教学日。
6. 点击 Today 的搜索，分别搜索课程名/简称、教师、教室、备注、DDL、考试、“备份”“悬浮窗”，点击结果验证直达页面。
7. 从旧版已有单目标配置升级，确认原 App 出现在“双击动作”；分别绑定五个 App，逐一验证。
8. 悬浮窗短划、斜划、滑到一半取消应不启动；长按出现反馈后拖动，松手只移动不启动；锁定后不能拖动；双击间隔过长或位移过大不启动。
9. 杀进程并重启，确认默认 Today、搜索/设置可用且五个绑定仍在。

## 构建验证

- `testDebugUnitTest`：通过。
- `assembleDebug`：通过。
- `lintDebug`：通过（已修复两个既有 minSdk API lint error）。

> 2026-09-07 实机截图修正：修复玻璃卡片内容重叠、收敛白色高光/阴影，并在教务登录 WebView 打开时完全移除主 BottomBar。应用户要求，此次截图修正后未重新编译；上述构建结果对应截图修正前的 v5.3 主体改动。

> 同版本可读性修正：全局搜索属于高密度文字界面，改用不透明 `MaterialTheme.surface` 承载，只保留圆角、层级和轻量阴影，不再让 Today 与 BottomBar 内容穿透。版本继续保持 5.3.0（versionCode 18），未重新编译。

> 同版本 BottomBar 磨砂修正：页面内容作为 Haze source，底栏只采样自身区域并使用 24dp 局部背景模糊；Android 12+ 显示真实模糊色块，旧系统使用 90% 主题色 fallback 遮罩，避免底层文字与导航文字同时清晰。搜索页等高密度界面仍保持实色。版本未变化，未重新编译。
