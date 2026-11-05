# Siri（安卓版）

Siri 风格安卓语音助手（学习演示，非苹果官方）

## 功能

- 🎙️ 点击主页面 Siri 光球 → 启动麦克风 → 语音识别结果自动传送到输入框
- 💬 液态玻璃输入框（"Ask Siri..." 为水印，空内容发不了，输入后水印消失）
- ⚡ 快捷指令：打开微信/QQ/抖音/相机/相册/设置等应用、查天气、定时/闹钟
- 🕐 时间/日期播报、四则运算、讲笑话
- 🔊 中文朗读（TTS）

## 权限

- 麦克风（语音识别，第一次点击光球时申请）

## 编译（生成签名 APK）

环境：JDK 11 + Android SDK 33 + Gradle 7.5

```bash
export ANDROID_HOME=/你的/Sdk
gradle assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`（已签名，Android 5.0+）

## 下载

在 Releases 页面下载最新 APK，开启「允许安装未知来源应用」即可安装。

## 声明

本项目仅为学习演示，不包含苹果任何官方代码或素材，非苹果官方 Siri。
