# 瓜瓜课程表 v4.9：更新中心 + 意见反馈

## 版本

- applicationId: `com.example.npucourse`
- versionCode: `5`
- versionName: `4.9`
- 正式签名继续使用原 `guagua-course-release.jks` / alias `guagua`

## 新增：GitHub 更新中心

更新清单地址固定为：

`https://raw.githubusercontent.com/chyyy1006-rgb/guaguacourse/main/update/latest.json`

App 行为：

1. 启动后最多每 6 小时自动检查一次。
2. 如果远端 `versionCode` 大于当前安装版本，显示新版本弹窗。
3. 用户选择“稍后”后，同一版本至少 24 小时后才会再次自动提示。
4. “我的 → 关于与更新”可以随时手动检查，不受 6 小时间隔限制。
5. “立即更新”只打开 `downloadUrl`，不申请未知来源安装权限，不静默下载安装 APK。
6. `forceUpdate` 默认为 false。除非有严重兼容性问题，不建议设为 true。

> 重要：v4.8.1 本身没有更新检查代码，因此 v4.8.1 → v4.9 这一次仍需通过原有渠道通知用户手动升级。从用户安装 v4.9 开始，后续版本可由 App 自动发现。

## 新增：关于与更新

入口：`我的 → 支持与关于 → 关于与更新`

包含：

- 当前版本 / versionCode
- 手动检查更新
- GitHub Releases 发布页
- GitHub 项目主页
- 意见反馈入口

## 新增：意见反馈

入口：`我的 → 支持与关于 → 意见反馈`

收件邮箱：`chyyy1006@gmail.com`

反馈类型：

- 功能建议
- Bug / 异常
- 课表导入问题
- Widget 问题
- DDL / 提醒问题
- 其他

用户可填写：

- 问题/建议描述（必填）
- 联系方式（选填）
- 是否附带基础诊断信息

基础诊断信息只包含：

- App 版本 / versionCode
- Android 版本 / API Level
- 设备厂商和型号
- 应用包名

不会自动附带：

- 课程内容
- 学号
- 教务密码
- Cookie
- 备份文件
- 任何教务登录凭据

点击“发送反馈”后使用 `mailto:` 调起用户自己的邮件客户端。若系统没有邮件 App，会把反馈正文复制到剪贴板并提示手动发送至反馈邮箱。

## GitHub 第一次部署

仓库当前需要至少提交：

- `update/latest.json`

推荐同时将完整 Android 项目推送到该仓库。

发布 v4.9：

1. 用原正式签名生成 v4.9 release APK。
2. 在 GitHub 仓库创建 Release，Tag 建议 `v4.9`。
3. 上传正式 APK，例如 `GuaguaCourse-4.9.apk`。
4. 确认 `update/latest.json` 已位于 `main` 分支。
5. 浏览器打开 raw latest.json，确认能看到 JSON 内容。
6. 在 v4.9 的“关于与更新”中点击检查更新；由于当前也是 versionCode 5，应显示“当前已经是最新版本”。

## 下次发布示例（v4.10）

`app/build.gradle.kts`：

```kotlin
versionCode = 6
versionName = "4.10"
```

然后把 `update/latest.json` 改为：

```json
{
  "versionCode": 6,
  "versionName": "4.10",
  "title": "瓜瓜课程表 4.10",
  "publishedAt": "2026-xx-xx",
  "forceUpdate": false,
  "downloadUrl": "https://github.com/chyyy1006-rgb/guaguacourse/releases/latest",
  "changelog": [
    "更新内容 1",
    "更新内容 2"
  ]
}
```

先发布 Release/APK，再更新 `latest.json`，可以避免用户先看到新版提示但发布页还没有 APK。

## 回归测试建议

P0：

- v4.8.1 使用原签名直接覆盖安装 v4.9，旧课表/DDL/设置仍在。
- 关于页正确显示 `4.9 / versionCode 5`。
- 无网络手动检查更新时不会闪退。
- 仓库没有 `latest.json` 时不会影响 App 正常使用。
- 意见反馈能打开 Gmail/系统邮件客户端，收件人是 `chyyy1006@gmail.com`。

P1：

- 把测试 `latest.json` 临时改成 versionCode 6，确认弹出更新提示。
- “稍后”可关闭普通更新提示。
- 点击“立即更新”打开 GitHub 发布页。
- 联系方式为空时仍能发送反馈。
- 关闭“基础诊断信息”后邮件正文不含设备信息。
- 无邮件客户端时反馈内容进入剪贴板。
