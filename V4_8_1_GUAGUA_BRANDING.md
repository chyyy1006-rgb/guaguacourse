# 瓜瓜课程表 v4.8.1 品牌更新

## 版本
- applicationId: `com.example.npucourse`（保持不变，保证正式版升级兼容）
- versionCode: `4`
- versionName: `4.8.1`
- App 显示名称：`瓜瓜课程表`

## 本次品牌更新
1. Android 桌面/App 显示名称统一改为“瓜瓜课程表”。
2. 所有 Launcher activity-alias 都继续使用 `@string/app_name`，因此切换不同桌面图标后名称仍为“瓜瓜课程表”。
3. 课表 PNG 图片底部来源标识改为“由瓜瓜课程表生成”。
4. 连续空闲时间图片底部来源标识改为“来自瓜瓜课程表”。
5. 连续空闲时间纯文字分享末尾增加/统一为“来自瓜瓜课程表”。
6. 用户保存的课表 PNG / ICS 建议文件名改为“瓜瓜课程表_...”。
7. App 内临时分享文件使用 `GuaguaCourse_...`，避免部分系统对中文缓存文件名兼容性不佳。
8. ICS PRODID 改为 `GuaguaCourse`。
9. Widget 内可见品牌、Widget 设置说明统一为“瓜瓜课程表”。
10. 完整备份 JSON 内 app 标识改为“瓜瓜课程表”，备份文件名前缀使用 `GuaguaCourse_backup_`。
11. 外观设置中的桌面图标说明同步改为“瓜瓜课程表”。

## 未修改项
- `applicationId = com.example.npucourse`
- Kotlin package 路径 `com.example.npucourse`
- `Theme.NPUcourse` / `NPUcourseTheme` 等内部代码符号

这些内部标识不属于用户可见品牌，保留可降低无意义重构和升级风险。

## 正式覆盖安装
请继续使用原发布密钥：
- keystore: `guagua-course-release.jks`
- alias: `guagua`

不要把 keystore 或密码提交到公开仓库。
