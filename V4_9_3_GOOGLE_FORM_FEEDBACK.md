# 瓜瓜课程表 v4.9.3 - Google Form 意见反馈

## 变更

- 意见反馈不再调用 Gmail / 系统邮件客户端。
- `我的 -> 支持与关于 -> 意见反馈` 直接在 App 内 WebView 打开 Google Form：
  `瓜瓜课程表使用反馈`。
- WebView 启用 JavaScript、DOM Storage 和 Cookie，以兼容 Google Forms。
- 顶部提供“浏览器”备用入口。
- 加入加载进度、主页面加载失败提示和“重新加载”。
- 返回键优先返回 WebView 历史；没有历史时退出反馈页面。
- 退出页面时主动销毁 WebView，避免长期占用资源。
- 不再在 App 内收集/拼接反馈正文，也不再读取用户联系方式或调用邮件发送。

## 版本

- versionCode: 8
- versionName: 4.9.3
- applicationId: com.example.npucourse

## 测试建议

1. 从“我的 -> 支持与关于 -> 意见反馈”进入。
2. 确认表单在 App 内加载并可正常滚动/输入/提交。
3. 提交后确认 Google Forms 显示成功页面。
4. 测试系统返回键：表单内部有历史时先返回，最终返回 App。
5. 断网进入反馈页，确认出现失败提示和重新加载按钮。
6. 测试“浏览器”备用入口。
