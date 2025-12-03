# TT-IC Camera (Android)

> PRD 已删除，这里汇总核心需求、功能与运行方式。

## 项目简介
- 轻量图片编辑 App，支持相册选图/相机拍照、基础编辑与保存水印。
- 主要功能：相册（全部/文件夹、空态/权限指引）与大图预览；相机拍照直达编辑；编辑支持手势缩放/平移、裁剪（自由+比例、可拖拽框）、旋转/翻转、亮度/对比度调节、文字叠加（字体/字号/颜色/透明度/旋转、拖拽缩放）、撤销/重做；保存到相册并自动加“训练营”水印。
- 技术栈：Kotlin、Jetpack Navigation/ViewModel/ActivityResult、CameraX、Coil、自定义 View（缩放画布、裁剪/文字叠加）。
- 兼容：Android 10+，默认以 JDK 17 / AGP 8.2.2 / Gradle 8.2 构建。

## 仓库结构
- `TTICCamera/`：Android 工程
  - `app/src/main/java/com/ttic/camera/ui/`：`splash`/`album`/`preview`/`editor` 页面
  - `app/src/main/java/com/ttic/camera/ui/widgets/`：缩放视图、裁剪/文字叠加
  - `app/src/main/java/com/ttic/camera/edit/`：编辑操作模型与处理器
  - `app/src/main/res/layout`：对应布局
  - `local.properties`：SDK 路径（如需请修改）
- `TT-IC-Camera-PRD.md`：旧 PRD（若需参照）
- `作业.md`：作业说明

## 运行
1. 确保本机 Android SDK 位于 `/Users/momo/Library/Android/sdk`（如路径不同，修改 `TTICCamera/local.properties`）。
2. 进入工程目录：`cd TTICCamera`
3. 构建调试包：`GRADLE_USER_HOME=./.gradle ./gradlew assembleDebug`
4. 安装到连接设备/模拟器：`GRADLE_USER_HOME=./.gradle ./gradlew installDebug`

## 交互与体验
- 相册：无权限时给授权/去设置入口；空相册空态；文件夹下拉筛选；点击缩略图进大图预览。
- 编辑：画布 0.5–2x 缩放/平移；裁剪框可拖拽/拉伸，支持比例快捷；旋转 90/180、水平/垂直翻转；亮度(-100~100)/对比度(-50~150) 实时调节并可撤销；文字支持多字体/字号/颜色/透明度/旋转，拖拽/缩放/旋转，撤销/重做。
- 保存：一键保存到相册 `Pictures/TTICamera`，自动加“训练营”水印；成功/失败明确提示。

## 已完成功能
- W1：相册/相机、权限与空态、大图预览、基础手势。
- W2：裁剪/旋转/翻转、亮度对比度、文字叠加、撤销/重做。
- W3：保存+水印、异常提示、裁剪框与显示对齐、文字预设扩充。

## 待优化/后续
- 性能与验证：10 张连续编辑压测、帧率/内存观测、横竖屏适配。
- 异常精细提示：保存失败区分空间/权限/IO 具体原因。
- 埋点与文档：类图/时序图、埋点接入、演示视频。
