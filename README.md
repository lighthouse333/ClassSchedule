# ClassSchedule

一个使用 Kotlin 和 Jetpack Compose 开发的大学生课表 Android 应用。

## 项目目标

ClassSchedule 旨在帮助大学生清晰地查看和管理每周课程。项目将按照循序渐进的方式开发，从基础课表展示开始，逐步加入课程管理、本地存储、单双周和课程提醒等功能。

## 当前功能

- 周一至周五课表网格
- 等宽课程列和课程卡片
- 显示课程名称、教师与教室
- 支持连续节次课程
- 添加课程弹窗
- 节次范围校验
- 课程时间冲突检测

> 目前新增课程只保存在内存中，关闭应用后会恢复为示例课程。

## 开发计划

- [x] 固定课表网格和课程卡片
- [x] 支持连续节次课程
- [ ] 编辑和删除课程
- [ ] 使用 Room 保存课程
- [ ] 支持教学周、单周和双周课程
- [ ] 添加课程提醒及其他高级功能

后续工作通过 GitHub Issues 跟踪。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin
- Room（计划引入）

## 本地运行

1. 使用 Android Studio 打开项目。
2. 等待 Gradle 同步完成。
3. 启动 Android 模拟器或连接 Android 设备。
4. 运行 `app` 配置。

也可以使用命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

Debug APK 将生成在 `app/build/outputs/apk/debug/`。

## 当前数据示例

应用默认展示一门课程：

- 课程：高等数学
- 教师：张老师
- 教室：A101
- 时间：周一第 1～2 节

## 说明

这是一个处于早期开发阶段的学习项目，功能和项目结构会随着开发计划逐步完善。
