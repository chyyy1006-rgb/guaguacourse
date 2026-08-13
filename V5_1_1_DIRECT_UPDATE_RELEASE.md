# GuaguaCourse 5.1.1

本版本用于将 5.1.0 已发布版本升级到新的应用内更新机制。

## 版本信息

- versionCode: 15
- versionName: 5.1.1
- GitHub Tag: `v5.1.1`
- APK 文件名: `GuaguaCourse-5.1.1.apk`

## 更新内容

- App 内直接下载新版 APK。
- 下载完成后自动唤起 Android 系统安装界面。
- 安装前校验 APK 包名、版本号和签名。
- `latest.json` 可选支持 SHA-256 校验。
- 下载失败时保留 GitHub 发布页作为备用入口。

## 发布顺序

1. 使用正式签名编译 APK。
2. 将 APK 命名为 `GuaguaCourse-5.1.1.apk`。
3. 在 GitHub 创建 `v5.1.1` Release 并上传 APK。
4. Release 发布成功后，再将本工程中的 `update/latest.json` 提交到 `main`。

注意：不要在 Release APK 尚未上传成功前提前提交 5.1.1 的 `latest.json`，否则 5.1.0 用户可能先检测到更新但无法下载。
