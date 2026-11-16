#!/bin/bash
set -e
cd "$(dirname "$0")"
export ANDROID_HOME=/opt/android-sdk
BT=$ANDROID_HOME/build-tools/34.0.0
PJ=$ANDROID_HOME/platforms/android-34/android.jar
rm -rf build; mkdir -p build/gen build/obj build/dex build/flats
$BT/aapt2 compile res/mipmap-mdpi/ic_launcher.png res/mipmap-hdpi/ic_launcher.png res/mipmap-xhdpi/ic_launcher.png res/mipmap-xxhdpi/ic_launcher.png res/mipmap-xxxhdpi/ic_launcher.png -o build/flats
$BT/aapt2 link -o /tmp/wukong-base.apk -I $PJ --manifest AndroidManifest.xml -A assets --java build/gen build/flats/*.flat
javac -nowarn -source 11 -target 11 -classpath $PJ -d build/obj $(find build/gen src -name '*.java')
$BT/d8 --release --min-api 24 --output build/dex $(find build/obj -name '*.class')
cp /tmp/wukong-base.apk build/unsigned.apk
python3 - <<'EOF'
import zipfile
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
    z.write('build/dex/classes.dex','classes.dex')
EOF
$BT/zipalign -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f wukong.jks ]; then keytool -genkeypair -keystore wukong.jks -alias wukong -keyalg RSA -keysize 2048 -validity 10000 -storepass wukong1234 -keypass wukong1234 -dname "CN=Wukong,OU=Wukong,O=Wukong,L=Zhengzhou,C=CN" 2>/dev/null; fi
$BT/apksigner sign --ks wukong.jks --ks-pass pass:wukong1234 --key-pass pass:wukong1234 --out /tmp/wukong-signed.apk build/aligned.apk
$BT/apksigner verify /tmp/wukong-signed.apk && cp /tmp/wukong-signed.apk "悟空AI-v1.0.0-signed.apk" && echo VERIFY_OK
ls -l "悟空AI-v1.0.0-signed.apk"
echo BUILD_OK
