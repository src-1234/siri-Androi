# QwenWork 千问办公（安卓客户端）

千问办公 WebView 壳应用，主页面直接加载 https://qwenwork.cn/app

## 功能

- 📱 主页面嵌入 https://qwenwork.cn/app
- 🔐 站内登录会话持久保存（Cookie），退出不丢失
- 🕸️ 所有跳转在应用内完成
- 📶 网络异常内置重试页面
- ⬅️ 返回键先回网页历史

## 编译（生成签名 APK）

环境：JDK 11 + Android SDK 33 + Gradle 7.5

```bash
export ANDROID_HOME=/你的/Sdk
gradle assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`（已签名，Android 5.0+）

## 下载

在 Releases 页面下载最新 APK。

## 声明

本应用为站点客户端封装，用于访问 https://qwenwork.cn/app 官方页面，非官方产品。
