# ClassSchedule

一个使用 Kotlin 和 Jetpack Compose 开发的大学生课表 Android 应用。

当前正式版已经覆盖课程管理、多课表、按周查看和本地 PDF 导入等核心流程。

## v1.0.0 正式版更新

- 新增多课表管理，可新建、切换课表，并分别保存课程和学期设置。
- 连续节次合并为一个课程块，不同课程使用不同颜色；课表顶部、节次栏和课程文字布局更加紧凑。
- 支持左右跟手滑动切换周次，周标题下方显示对应日期。
- 点击空白区域或长按拖动框选节次即可添加课程，框选后可在选区内直接确认或点击其他区域取消。
- 单独添加的课程可设置自定义开始、结束时间，课程块会按实际时间计算显示位置。
- 新增东北师范大学（罗格斯大学纽瓦克学院）多班级课表 PDF 导入，并改进北京化工大学课表 PDF 的旋转页面与课程字段识别。
- 每次打开或重新进入 App 时根据手机本地日期定位教学周，开学前显示第 1 周，结课后显示最后一周。
- 仅在实际当前周突出显示今天对应的星期和日期。
- 北京化工大学 PDF 字段改为独立识别，支持缺少教师信息或字段顺序不同的学生个人课表。
- 启用全新课程表应用图标，并适配圆形、圆角方形和系统主题单色图标。

## 当前功能

- 显示周一至周日课表
- 按教学周翻页，并显示对应日期
- 连续节次课程显示为一个跨节课程块
- 新建和切换多张课表，课程与学期设置相互独立
- 添加、编辑和删除课程
- 使用 Room 在本地保存课程
- 支持连续节次和不连续周次，例如 `1-9,11-18`
- 检测课程时间冲突和重复课程
- 设置学期开始日期、学期总周数和每日节数
- 单独设置每一节课的开始及结束时间
- 从课表文件导入课程
  - 当前支持北京化工大学、东北师范大学课表 PDF
  - 导入前可以预览并选择课程
  - 东北师大多班级总课表支持先选择班级
  - 自动跳过重复课程并提示冲突

课表文件只在设备本地解析，不会上传到服务器。

## 导入北京化工大学课表

1. 打开右上角菜单。
2. 选择“从课表导入”。
3. 选择“北京化工大学”。
4. 选择教务系统导出的课表 PDF。
5. 检查识别结果，勾选课程并确认导入。

当前解析器适用于带文字层的北京化工大学标准课表 PDF。扫描件以及版式不同的文件暂不保证能够识别。

东北师范大学导入适用于罗格斯大学纽瓦克学院的标准总课表 PDF，支持单周、双周、指定周次和多班级选择。

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

## 正式签名构建

正式签名信息从项目根目录的 `keystore.properties` 读取，该文件已被 Git 忽略。文件格式如下：

```properties
storeFile=C:/path/to/classschedule-release.jks
storePassword=本地密钥库密码
keyAlias=classschedule
keyPassword=本地密钥密码
```

配置完成后执行：

```powershell
.\gradlew.bat clean testDebugUnitTest assembleRelease
```

正式签名 APK 将生成在 `app/build/outputs/apk/release/app-release.apk`。签名文件和密码不得提交到 GitHub。

## 当前状态

当前正式版本为 `v1.0.0`。北京化工大学与东北师范大学 PDF 导入、教学周自动定位和新应用图标均已完成真机测试。

后续开发计划和问题通过 GitHub Issues 跟踪。
