# V3.9.5 — 参考 nwpu-edu-plus 修正学业查询

本版重点：

1. 成绩查询不再依赖移动端成绩页面 DOM。
2. 参考 top-tree/nwpu-edu-plus 的实际数据链：
   - getStdInfo 获取 student.id
   - getMyGpa 获取 GPA / 排名
   - getMyGrades 获取学期列表
   - grade/sheet/info/{studentId}?semester={semesterId} 获取每学期成绩
3. 所有请求仍在已经登录的 jwxt WebView 页面内部以同源 fetch 发起，不把 Cookie / 密码交给 Android 原生层。
4. 考试导航参考该项目“直接查找隐藏菜单 a[data-text] / href”的思路，不再要求手机版侧栏必须先可见；优先直接进入“考试信息”真实链接。
5. 保留考试 DOM 读取作为后续字段解析方案。

数据库仍为 V3，无 Migration。
