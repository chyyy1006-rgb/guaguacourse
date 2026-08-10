# 瓜瓜课程表 v4.9.2 图标安全区修复

## 修复内容

- 修复 Android 8+ Adaptive Icon 在圆形、圆角方形等 Launcher Mask 下主体被放大裁切的问题。
- 四套图标不再把完整成品图作为 Adaptive Icon foreground；改为以完整图作为 background，由系统只做最终 Mask 裁切，避免 foreground 的额外缩放/视差安全区裁切。
- 四套图标整体增加约 5% 外围安全边距，日历、西瓜、文字和吉祥物主体不会贴边。
- 清理生成图四角的黑色区域，Adaptive Icon 外围改为与图标背景一致的绿色延展。
- 同步重新生成 mdpi / hdpi / xhdpi / xxhdpi / xxxhdpi legacy 图标与设置页真实预览图。

## 版本

- versionCode: 7
- versionName: 4.9.2

## 测试建议

重点测试圆形、圆角方形、方形/水滴形等桌面图标 Mask，并在“我的 → 外观设置 → 桌面图标”依次切换四套图标。
部分 Launcher 有图标缓存，切换后可能需要返回桌面等待数秒。
