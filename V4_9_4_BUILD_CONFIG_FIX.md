# v4.9.4 BuildConfig 编译修复

- 修复 `FeedbackPage.kt` 中 `BuildConfig` 未生成导致的 `Unresolved reference 'BuildConfig'`。
- 改为通过 `PackageManager.getPackageInfo()` 动态读取当前安装包的 `versionName` 与 `versionCode`。
- 保留腾讯问卷、设备信息卡片与“复制设备信息”功能。
- 版本保持 `4.9.4` / `versionCode 9`。
