# v4.9.4 腾讯问卷附件与设备信息复制修正

- 为反馈 WebView 增加 `WebChromeClient.onShowFileChooser()`。
- 腾讯问卷中的文件上传题会调用 Android 系统文件选择器。
- 使用系统文档选择器，不额外申请存储权限。
- App 版本、Android 版本、设备型号分别提供复制按钮。
- 保留“复制全部”按钮。
- 版本仍为 4.9.4（versionCode 9）。
