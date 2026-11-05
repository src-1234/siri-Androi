# SiriAssistant-Android

Siri 风格语音助手安卓版（个人学习作品，非苹果官方产品）

点击彩色球体说话，支持语音识别、关键词应答、中文朗读（TTS），还会报时、算数、讲笑话。

## 功能

- 🎙️ 语音识别：调用系统语音识别（需录音权限，无识别服务时自动转为手动输入）
- 💬 智能应答：问候、报时、日期、天气提示、冷笑话、打开相机/设置、简单四则运算
- 🔊 中文朗读：TextToSpeech 语音播报回答
- 🌈 Siri 风格界面：多彩渐变球体 + 语音波形动画，深色沉浸式主题
- 📦 零第三方依赖：纯原生 Java 实现，APK 体积小

## 技术栈

| 项目 | 版本 |
| ---- | ---- |
| Java | 8+（构建环境需 JDK 11） |
| Gradle | 7.5 |
| Android Gradle Plugin | 7.4.2 |
| compileSdk / targetSdk | 33 |
| minSdk | 21（Android 5.0+） |

## 快速构建（生成签名 APK）

```bash
# 1. 生成签名密钥（只需一次，会生成 keystore/ 与 signing.properties）
bash scripts/gen_keystore.sh

# 2. 构建正式版 APK（产物：app/build/outputs/apk/release/app-release.apk）
gradle assembleRelease
```

详细图文步骤见 [BUILD_GUIDE.md](BUILD_GUIDE.md)。

## 项目结构

```
SiriAssistant-Android/
├── app/
│   ├── build.gradle          # 模块构建脚本（含签名配置）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/siriassistant/
│       │   ├── MainActivity.java      # 主逻辑：识别→应答→朗读
│       │   ├── SiriButtonView.java    # 多彩球体按钮（自绘）
│       │   └── WaveView.java          # 语音波形动画（自绘）
│       └── res/                        # 布局 / 主题 / 图标
├── scripts/
│   ├── gen_keystore.sh               # 一键生成签名密钥
│   └── github_release.sh             # 一键发布 APK 到 GitHub Release
├── settings.gradle
├── build.gradle
├── BUILD_GUIDE.md                     # 完整编译 + 发布教程
└── README.md
```

## 安装说明

- APK 必须**签名**才能安装（本项目的正式 APK 已签名）
- 手机需开启「允许安装未知来源应用」
- Android 5.0（API 21）及以上

## 声明

本项目为个人学习 / 演示用途，不包含任何苹果官方代码、商标或素材。
