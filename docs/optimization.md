# 优化跟踪

更新日期：2026-09-23

## 状态总览

| 类别 | 项 | 状态 |
|------|-----|------|
| 产品 | F1–F6 | ✅ |
| 工程 | E1–E3 抽测 / 备份 / Screen 拆分 | ✅ |
| 工程 | E4b AGP 8.5 + Gradle 8.7 + Kotlin 1.9 + KSP | ✅ |
| 工程 | **E4c** Kotlin **2.0.21** + Compose 插件 + **targetSdk 35** + NDK 29 + 16KB linker | ✅ |
| 工程 | E5–E9 | ✅ |
| 体验 | U1–U4 | ✅ |
| 数据 | SM-2 q0–5 + answer_event + **统计页质量分布** | ✅ |

## E4c 工具链（当前）

| 组件 | 版本 |
|------|------|
| Gradle | 8.9 |
| AGP | 8.7.3 |
| Kotlin | 2.0.21 + `org.jetbrains.kotlin.plugin.compose` |
| KSP | 2.0.21-1.0.28 |
| compileSdk / targetSdk | **35** |
| NDK | **29.0.14033849**（无 local ndk.dir） |
| Compose BOM | 2024.10.01 |
| Room | 2.6.1 + KSP |
| JVM | 17 |
| whisper `.so` | `-Wl,-z,max-page-size=16384` |

上架前仍建议：用 `zipalign -c -P 16` / 16KB 模拟器再验一次 `.so`。

## 统计页 × answer_event

- `data/AnswerAnalytics.kt`：`qualityHistogram` / `summarizeQuality`
- `ProgressStore.qualitySummary(days)` 异步读库
- `StatsScreen`：「回忆质量」卡片 — 合格率 (q≥3)、平均质量、0–5 柱状图

## 验证

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug --no-parallel
```

- [x] 单元测试（含 quality 汇总、SM-2 q0–5）
- [x] assembleDebug（含 native arm64 + x86_64）BUILD SUCCESSFUL
- [ ] 真机 / 16KB 模拟器冒烟
- [ ] assembleRelease（签名）

## 原则

- US/UK 音频随包；Whisper 按需下载  
- SM-2 质量分驱动；对/错 → q=4/1  
- 集合练习不写关卡完成  
- 进度导出不含 Key/PIN；明细默认不进备份 JSON  
- 勿开 `GGML_OPENMP`  
