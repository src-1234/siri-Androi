# BotGroup Chat Android

BotGroup Chat 安卓客户端（WebView 壳应用），主页面直接加载 https://botgroup.chat/

## 功能

- 📱 主页面嵌入 https://botgroup.chat/
- 🔐 站内 OAuth 登录（Google / GitHub / Linux.do），登录会话持久保存，退出应用不丢失
- 🕸️ 所有跳转在应用内完成，登录流程不断链
- 📶 网络异常内置重试页面
- ⬅️ 返回键先回网页历史

## 编译（生成签名 APK）

环境：JDK 11 + Android SDK 33 + Gradle 7.5

```bash
# 签名配置在 signing.properties（本地，不入库）
export ANDROID_HOME=/你的/Sdk
gradle assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`（已签名，Android 5.0+）

## 下载

在 Releases 页面下载最新 APK。

## 声明

本项目是社区站点的安卓客户端封装，仅用于访问 https://botgroup.chat/ 官方页面，与站点无官方合作关系。
