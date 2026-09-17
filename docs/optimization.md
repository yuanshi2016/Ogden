# 优化跟踪

更新日期：2026-09-16

## 已完成

- [x] 删除仅用于首页一句文案的 8.95 MB 字体，改用系统 FontFamily.Cursive。
- [x] minSdk 从 23 升至 24。
- [x] 删除未使用的 Compose tooling、Core KTX 和 Lifecycle 直接依赖声明。
- [x] 删除从未启用的错词练习模式及其手动刷新状态。
- [x] 恢复可移植的 Gradle Wrapper 和 JDK 配置。
- [x] 删除未使用的 nextLevel。
- [x] 内联仅使用一次的 FlowRowCompat。
- [x] 将单方法 OgdenRepository 改为顶层函数。
- [x] 删除未使用的 instrumentation test runner 配置。
- [x] 删除简繁切换功能（含 ICU Transliterator 与偏好项），界面固定简体中文。

## 验证

- [x] 单元测试通过（JDK 17）。
- [x] Debug APK 构建通过（JDK 17）。
- [ ] 在 API 24+ 设备确认音频播放。

当前 Gradle 7.6.4 / AGP 7.4.2 不支持用 JDK 21 运行；本地和 CI 使用 JDK 17。

## 后续原则

- 保留 1700 个 US/UK 音频文件：离线发音属于明确产品功能。
- 不为缩短文件而拆分 MainActivity.kt；出现独立职责或多人修改冲突时再拆分。
