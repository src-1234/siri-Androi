#!/bin/bash
# 手动构建 APK（无 Gradle）：aapt2 -> javac -> d8 -> zip -> zipalign -> apksigner
set -e
SDK=/tmp/qwenwork/android-sdk
BT=$SDK/build-tools/35.0.0
PLATFORM=$SDK/platforms/android-34/android.jar
ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD=$ROOT/build

rm -rf "$BUILD"
mkdir -p "$BUILD/gen" "$BUILD/classes" "$BUILD/dex" "$ROOT/keystore"

echo "[1/6] aapt2 compile + link"
"$BT/aapt2" compile --dir "$ROOT/res" -o "$BUILD/res.zip"
"$BT/aapt2" link \
  -o "$BUILD/base.apk" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -I "$PLATFORM" \
  --java "$BUILD/gen" \
  --min-sdk-version 24 --target-sdk-version 34 \
  --version-code 2 --version-name 1.1 \
  "$BUILD/res.zip"

echo "[2/6] javac"
javac --release 11 -Xlint:-options \
  -classpath "$PLATFORM" \
  -d "$BUILD/classes" \
  "$BUILD/gen/cn/qwenwork/siriassistant/R.java" \
  "$ROOT"/src/cn/qwenwork/siriassistant/*.java

echo "[3/6] d8 (dex)"
"$BT/d8" --release --min-api 24 \
  --lib "$PLATFORM" \
  --output "$BUILD/dex" \
  $(find "$BUILD/classes" -name '*.class')

echo "[4/6] package"
cd "$BUILD" && zip -q -j base.apk dex/classes.dex

echo "[5/6] zipalign"
"$BT/zipalign" -f 4 "$BUILD/base.apk" "$BUILD/aligned.apk"

echo "[6/6] sign"
if [ ! -f "$ROOT/keystore/release.keystore" ]; then
  keytool -genkeypair -v \
    -keystore "$ROOT/keystore/release.keystore" \
    -storepass siri123456 -keypass siri123456 \
    -alias siri -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Siri Assistant, OU=QwenWork, O=QwenWork, C=CN" >/dev/null 2>&1
fi
"$BT/apksigner" sign \
  --ks "$ROOT/keystore/release.keystore" \
  --ks-pass pass:siri123456 --key-pass pass:siri123456 \
  --ks-key-alias siri \
  --out "$ROOT/SiriAssistant.apk" \
  "$BUILD/aligned.apk"

"$BT/apksigner" verify "$ROOT/SiriAssistant.apk" && echo "OK: $ROOT/SiriAssistant.apk"
ls -lh "$ROOT/SiriAssistant.apk"
