# v4.9.4 编译修复

修复 `FeedbackPage.kt` 中显式导入 `androidx.compose.foundation.layout.weight` 导致的 Compose 编译错误。

`Modifier.weight(1f)` 保持不变，由 `ColumnScope` 作用域直接解析。
版本号保持 `4.9.4` / `versionCode 9`，因为此前版本尚未成功编译发布。
