# ClassSchedule

一个使用 Kotlin 和 Jetpack Compose 开发的大学生课表 Android 应用。

当前版本已经覆盖课程管理、按周查看和本地 PDF 导入等核心流程，适合作为早期测试版本使用。

## 当前功能

- 显示周一至周日课表
- 按教学周翻页，并显示对应日期
- 添加、编辑和删除课程
- 使用 Room 在本地保存课程
- 支持连续节次和不连续周次，例如 `1-9,11-18`
- 检测课程时间冲突和重复课程
- 设置学期开始日期、学期总周数和每日节数
- 单独设置每一节课的开始及结束时间
- 从课表文件导入课程
  - 当前支持北京化工大学课表 PDF
  - 导入前可以预览并选择课程
  - 自动跳过重复课程并提示冲突

课表文件只在设备本地解析，不会上传到服务器。

## 导入北京化工大学课表

1. 打开右上角菜单。
2. 选择“从课表导入”。
3. 选择“北京化工大学”。
4. 选择教务系统导出的课表 PDF。
5. 检查识别结果，勾选课程并确认导入。

当前解析器适用于带文字层的北京化工大学标准课表 PDF。扫描件以及版式不同的文件暂不保证能够识别。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Preferences DataStore
- PdfBox-Android
- Android Gradle Plugin

## 本地运行

1. 使用 Android Studio 打开项目。
2. 等待 Gradle 同步完成。
3. 启动 Android 模拟器或连接 Android 设备。
4. 运行 `app` 配置。

也可以使用命令行测试并构建 Debug APK：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

APK 将生成在 `app/build/outputs/apk/debug/app-debug.apk`。

## 当前状态

首个测试版本为 `v0.1.0-beta.1`。主要功能已经可以使用，但仍建议先在真机上核对 PDF 导入结果和小屏幕布局。

后续开发计划和问题通过 GitHub Issues 跟踪。
