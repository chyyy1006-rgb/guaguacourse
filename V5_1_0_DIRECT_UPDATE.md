# GuaguaCourse 5.1.0 应用内直接更新

5.1.0 的更新流程改为：

1. App 从 `update/latest.json` 检查版本。
2. 用户点击“下载并安装”后，App 直接下载 `downloadUrl` 指向的 APK 到应用私有缓存。
3. 下载完成后校验 APK 包名、versionCode 和签名；如果 `latest.json` 提供 `sha256`，还会验证 SHA-256。
4. 校验通过后调用 Android 系统安装界面，由用户确认升级，不进行静默安装。
5. Android 8.0+ 如果尚未授权“安装未知应用”，App 会先打开本应用对应的系统授权页，返回后继续安装。
6. 下载失败时，更新弹窗仍提供 GitHub 发布页作为备用入口。

## GitHub Release 要求

为了让 5.1.0 的 `latest.json` 直接下载成功，本次 Release 请固定使用：

- Tag：`v5.1.0`
- APK 文件名：`GuaguaCourse-5.1.0.apk`

因此正式下载地址为：

`https://github.com/chyyy1006-rgb/guaguacourse/releases/download/v5.1.0/GuaguaCourse-5.1.0.apk`

后续版本同理，例如 5.2.0：

- Tag：`v5.2.0`
- APK：`GuaguaCourse-5.2.0.apk`
- `latest.json` 的 `downloadUrl` 同步改为对应地址。

## 可选 SHA-256

发布 APK 后可以在 `latest.json` 增加：

```json
"sha256": "APK 文件的 64 位 SHA-256"
```

App 会在启动系统安装界面前再次校验文件哈希。
