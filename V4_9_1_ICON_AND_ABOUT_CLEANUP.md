# 瓜瓜课程表 v4.9.1：品牌图标与关于页整理

## 桌面图标

外观设置现在提供 4 套真实品牌图标：

- 西瓜日历（默认）
- 瓜瓜·课
- 清新日程
- 小瓜助手

图标已生成 Android legacy mipmap（mdpi~xxxhdpi）及 Android 8+ Adaptive Icon。设置页预览直接读取实际 PNG，不再使用 Canvas 示意图。

为防止已有 v4.8/v4.9 用户曾选择旧的“靛紫/深蓝/曜石/玫瑰/日落”图标后升级丢失桌面入口，旧 activity-alias 仍保留为兼容入口。应用启动后 LauncherIconManager 会将旧序列化样式映射到新图标并关闭旧 alias。

## 关于与更新

- 删除“关于与更新”内部重复的“意见反馈”，反馈只保留“我的 → 支持与关于 → 意见反馈”一个入口。
- 删除“项目主页”，保留“GitHub 发布页”用于查看版本说明和下载 APK。
- 页面副标题由“版本、更新与支持”精简为“版本与更新”。

## 版本

- versionCode: 6
- versionName: 4.9.1
- applicationId 保持 com.example.npucourse
- 正式覆盖安装仍需使用 guagua-course-release.jks / alias guagua 签名。
