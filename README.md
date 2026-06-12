# MediaRecorder

一款 Android 短视频录制 Demo，实现类似 Vine 的「按住录制、松手暂停」分段录像体验，支持片段回删、进度可视化，并通过 JavaCV / FFmpeg 将多段音视频合成为 MP4 文件。

## 功能特性

- **分段录制**：长按录制按钮开始录像，松手暂停；可多次录制拼接成一条视频
- **时长限制**：最短 2 秒、最长 8 秒（见 `RecorderEnv`）
- **片段回删**：支持标记并删除最近一段录制内容
- **进度条**：自定义 `ProgressView` 实时展示各片段进度与最小录制阈值
- **相机能力**：前后摄像头切换、触摸对焦、闪光灯（UI 已预留）
- **音视频编码**：Camera 预览帧 + `AudioRecord` 采集，结束时由 `FFmpegFrameRecorder` 合成 MP4

## 技术栈

| 类别 | 选型 |
|------|------|
| 平台 | Android（API 14–21） |
| 语言 | Java |
| 相机 | `android.hardware.Camera`（Camera1 API） |
| 音频 | `AudioRecord` |
| 编码 | JavaCV + FFmpeg（`FFmpegFrameRecorder`） |
| 图像处理 | OpenCV（JavaCV `IplImage`）、YUV420 旋转 |
| 构建 | Eclipse ADT / Ant（`project.properties`） |

## 项目结构

```
MediaRecorder/
├── AndroidManifest.xml          # 权限与 Activity 声明
├── src/com/baidu/mediarecorder/
│   ├── RecorderActivity.java    # 主录制界面与核心逻辑
│   ├── PreviewActivity.java     # 预览页（占位实现）
│   ├── ProgressView.java        # 录制进度自定义 View
│   ├── contant/
│   │   └── RecorderEnv.java     # 编码参数、路径、时长常量
│   └── util/
│       ├── FFmpegFrameRecorder.java  # FFmpeg 封装（源自 JavaCV）
│       ├── CameraHelper.java         # 分辨率选择与对焦区域
│       ├── YuvHelper.java            # YUV420 旋转
│       ├── VideoFrame.java           # 视频帧数据结构
│       ├── RecorderHelper.java       # 工具方法与弹窗
│       └── LogHelper.java            # 日志封装
├── res/                         # 布局、样式、资源
└── libs/                        # javacv.jar、javacpp.jar（需自行准备）
```

## 环境要求

- Android SDK（`target=android-20`，见 `project.properties`）
- Eclipse + ADT，或兼容的 Ant 构建环境
- 同级目录下的 `appcompat_v7` 库（`android.library.reference.1=../appcompat_v7`）
- `libs/javacv.jar` 与 `libs/javacpp.jar`，以及对应架构的 FFmpeg 原生 `.so` 库

> **注意**：当前仓库未包含 `libs/` 目录与原生库，克隆后需自行补齐 JavaCV 依赖方可编译运行。

## 权限

应用声明了以下权限（见 `AndroidManifest.xml`）：

- `CAMERA` — 相机预览与采集
- `RECORD_AUDIO` — 麦克风录音
- `WRITE_EXTERNAL_STORAGE` — 视频输出到 `/sdcard/MediaRecorder/`

## 构建与运行

1. 将项目导入 Eclipse（或支持 ADT 的 IDE）
2. 确保 `../appcompat_v7` 存在且已正确引用
3. 将 JavaCV / JavaCPP 的 JAR 及 FFmpeg 原生库放入 `libs/` 与 `jniLibs/`
4. 连接真机（模拟器对 Camera / 原生库支持有限），编译安装 APK
5. 启动 `RecorderActivity` 即可开始录制

## 录制流程简述

```
用户按住录制 → Camera 预览回调采集 YUV 帧 + AudioRecord 采集 PCM
     ↓
用户松手暂停 → 当前片段帧/音频存入 LinkedList
     ↓
可继续录制或回删上一段
     ↓
点击完成 → AsyncTask 按时间戳将各片段写入 FFmpegFrameRecorder → 输出 MP4
```

输出路径：`/sdcard/MediaRecorder/{timestamp}.mp4`（见 `RecorderEnv.VIDEO_DIR`）

## 编码参数（默认）

| 参数 | 值 |
|------|-----|
| 视频编码 | MPEG4（可选 H264，见 `RecorderEnv` 注释） |
| 音频编码 | AAC（API ≥ 10）/ AMR_NB |
| 帧率 | 30 fps |
| 视频码率 | 1 Mbps |
| 音频采样率 | 44100 Hz |
| 输出分辨率 | 480×480 |
| 容器格式 | MP4 |

## 已知限制

- 使用已废弃的 Camera1 API，未适配 Camera2 / CameraX
- 录制帧缓存在内存中，长片段或高分辨率设备可能 OOM
- `PreviewActivity` 尚未实现预览播放逻辑
- 部分机型对焦模式做了硬编码适配（如 Galaxy S4、魅族 MX3）
- 目标 SDK 较低（21），在现代 Android 版本上需额外适配存储与权限模型

## 许可证

`FFmpegFrameRecorder.java` 源自 [JavaCV](https://github.com/bytedeco/javacv)（GPL v2 + Classpath exception），使用时请遵守相应开源协议。

## 相关文档

更完整的架构分析、STAR 描述与技术重难点见 [PROJECT_ANALYSIS.md](./PROJECT_ANALYSIS.md)。
