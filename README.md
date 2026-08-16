# Word Loop for Android

Native Android client for [Word Loop](https://word-loop.codewiz.dev) — AI-generated vocabulary cards, quizzes, and spaced repetition. Uses the same backend and Firebase project as the iOS app.

## Stack

Kotlin · Jetpack Compose · Material 3 · Hilt · Retrofit · DataStore · Firebase Auth / FCM / Analytics / Crashlytics

Server is the source of truth. SM-2 scheduling is **not** implemented on the device.

## Setup

- JDK 17
- Android SDK 36
- Emulator or device with Google Play (for Google Sign-In)

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

`app/google-services.json` is registered for package `com.codewiz.wordloop` on Firebase project `word-loop-c1f08`.

## Backend

Default: `https://word-loop.codewiz.dev`

Debug/test users can switch Production / Dev from Settings. Debug builds can also point at `http://10.0.2.2:3000` via `-PuseLocalBackend`.
