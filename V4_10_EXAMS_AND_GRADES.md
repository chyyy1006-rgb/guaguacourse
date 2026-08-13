# V4.10.0 — 考试安排与成绩查询

## 本次新增

- 学业首页新增“考试与成绩”入口。
- 复用学校统一身份认证 / 翱翔教务 WebView 登录态，不新增账号密码存储。
- 成绩查询改为在已登录 `jwxt.nwpu.edu.cn` WebView 内执行同源只读 `fetch`：
  - `student-portrait/getStdInfo` 获取学生内部 ID；
  - `student-portrait/getMyGpa` 获取 GPA / 排名信息；
  - `student-portrait/getMyGrades` 获取学期列表；
  - `grade/sheet/info/{studentId}?semester={semesterId}` 获取各学期课程成绩。
- 成绩页支持学期切换，展示课程成绩、学分、绩点、必修属性和可用时的教学班排名。
- 考试查询读取官方 `/student/for-std/exam-arrange` 页面，并优先解析 `tr[data-finished]` 结构；另带通用表格解析 fallback。
- 考试页支持“未结束 / 全部”切换，展示课程、考试时间、地点、状态。

## 实现说明

WebView 的异步 `fetch` 不依赖 `evaluateJavascript()` 直接返回 Promise，而是通过只读 JavaScript bridge 将规范化 JSON 回传 Android 层。这样可以保留学校 Cookie 在 WebView 内，不需要在原生层导出 Cookie。

现有课表同步逻辑未改动；考试/成绩作为独立页面接入。

## 当前验证状态

- 两段注入 WebView 的 JavaScript 已通过 Node.js `--check` 语法检查。
- 已做 Kotlin 源码级静态检查，未发现语法解析错误。
- 当前执行环境没有缓存 Gradle 9.1.0，且无法访问 `services.gradle.org`，因此无法在此环境完成 Android Gradle 编译与 APK 产出。
- 仍建议在真实学生账号下各验证一次：成绩接口 JSON 字段、考试页面 DOM，以及 CAS 登录过期后的重登流程。
