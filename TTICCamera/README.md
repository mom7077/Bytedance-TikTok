# TT-IC Camera (Android)

基于 `TT-IC-Camera-PRD.md` 的 Android MVP 实现，当前已完成 W1 流程。

## 项目结构
- `app/src/main/java/com/ttic/camera`
  - `MainActivity` 容器 + `NavHostFragment`
  - `ui/splash`：启动页 400ms 自动跳转相册
  - `ui/album`：相册页（全部/文件夹 Tab、权限指引、空态、缩略图网格、文件夹筛选、拍照入口）
  - `ui/preview`：大图预览，支持 0.5–2x 缩放/平移，单击进入编辑
  - `ui/editor`：编辑页，支持 0.5–2x 缩放/平移预览，亮度/对比度实时调节，旋转/翻转，中心裁剪（多比例），文字叠加（字体/字号/颜色/透明度/旋转，可拖拽缩放），撤销/重做
- `app/src/main/res/navigation/nav_graph.xml`：启动→相册→预览→编辑的导航流
- `app/build.gradle`：Kotlin + Navigation + CameraX + Coil 依赖，minSdk 29 / targetSdk 34

## 开发运行
1. 用 Android Studio Hedgehog/Koala 或命令行导入根目录 `TTICCamera`
2. 确保本地 JDK 17；若缺失 `./gradlew` 可在 IDE 里生成 Gradle Wrapper 或用本地 Gradle 8.2+
3. 运行后流程：启动 → 相册（授权相册权限后自动加载，Tab 切换全部/文件夹，点击缩略图进预览；右下拍照需要相机权限，拍后直达编辑）→ 预览 → 编辑

## 下一步（按里程碑）
- W2：已完成基础编辑核心（调节/旋转/翻转/裁剪比例/文字叠加/撤销重做/画布手势），后续补充自由裁剪框、更多字体/颜色和手势优化。
- W3：保存合成（含“训练营”水印）、异常提示、性能压测与 Android 10+ 真机验证。
