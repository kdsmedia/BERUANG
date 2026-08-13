# BERUANG

Native Android social app built with Jetpack Compose + Firebase. The repo is Android-only.

## Build
```
cd android
export ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:assembleDebug --no-daemon
```
APK output: `android/app/build/outputs/apk/debug/app-debug.apk`