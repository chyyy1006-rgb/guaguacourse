# GitHub 发布设置（chyyy1006-rgb/guaguacourse）

仓库：`https://github.com/chyyy1006-rgb/guaguacourse`

## 1. 首次推送源码

在本项目根目录初始化 Git（如果还没有）：

```bash
git init
git branch -M main
git remote add origin https://github.com/chyyy1006-rgb/guaguacourse.git
git add .
git commit -m "Release Guagua Course v4.9"
git push -u origin main
```

不要提交正式签名文件或密码。确认 `.gitignore` 覆盖：

- `*.jks`
- `*.keystore`
- `keystore.properties`
- `local.properties`

本交付包不包含你的正式 keystore。

## 2. 创建 GitHub Release

GitHub → Releases → Draft a new release：

- Tag: `v4.9`
- Title: `瓜瓜课程表 4.9`
- 上传使用 `guagua-course-release.jks` 签名后的正式 APK
- 推荐文件名：`GuaguaCourse-4.9.apk`

## 3. 更新清单

App 从下面的文件读取最新版本：

`update/latest.json`

以后每次发版必须提高 `versionCode`，并更新该 JSON。

推荐顺序：

1. 构建并签名 APK
2. 创建 GitHub Release 并上传 APK
3. 最后提交新的 `update/latest.json`

这样旧版本不会在 APK 尚未上传时提前提示升级。
