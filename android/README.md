# BERUANG — Android (native) build instructions

## Prerequisites
- Android Studio (Hedgehog or newer) with Android SDK 34.
- A Firebase project with `google-services.json` already placed at `app/google-services.json`
  (client package `com.altomedia.beruang`, project `altomedia-indonesia`).

## 1. Enable Firebase services (one-time, in the Firebase Console)
1. **Authentication → Sign-in method → Email/Password → Enable.** (Required: login uses phone numbers,
   but they are stored internally as synthetic emails like `0812…@beruang.phone` under the
   Email/Password provider — **no OTP / SMS verification** is sent.)
2. **Firestore Database → Create database** (start in production mode; the rules below secure it).
3. **Storage → Get started** (default bucket `altomedia-indonesia.firebasestorage.app`).

## 2. Deploy security rules
From the `android/` folder (requires the Firebase CLI: `npm i -g firebase-tools`):
```bash
firebase login
firebase deploy --only firestore:rules,storage
```
This deploys `app/firestore.rules` and `app/storage.rules`.

> ⚠️ Two rules are intentionally loose (flagged, tighten in production if desired):
> - `notifications` allow `create: if signedIn()` — any signed-in user can insert a notification for any user.
> - `friendships` allow `update` by either party — the sender can self-accept their own pending request.
> Consider a `SECURITY DEFINER`-style Cloud Function for notifications.

## 3. Build & run
Open the `android/` folder in Android Studio, let Gradle sync, then Run (▶) on a device/emulator
(min Android 7.0 / API 24). The first sign-up also creates a `profiles` document via the app's
fallback path (a Cloud Function trigger is recommended for guaranteed creation — see note below).

## Note on auto-profile creation
There is no server-side Auth trigger that auto-creates a `profiles` row on signup unless you add a
Cloud Function. This app includes a client-side fallback: `ProfileRepository.loadMyProfile()` writes
a `profiles` doc for the current user on first load if one doesn't exist (allowed by the `profiles`
create rule). For a more robust setup, deploy an `onCreate` Auth Cloud Function that writes the doc.

## Project layout
```
android/
  app/
    google-services.json          ← your Firebase config (client com.altomedia.beruang)
    build.gradle.kts              ← dependencies (Compose, Firebase BoM, Hilt, Coil)
    firestore.rules / storage.rules
    src/main/
      AndroidManifest.xml
      java/com/altomedia/beruang/
        BeruangApp.kt             ← @HiltAndroidApp
        MainActivity.kt           ← Compose entry
        data/  model/ repo/ FirebaseModule.kt
        ui/   theme/ components/ auth/ home/ feed/ friends/ messages/ groups/ notifs/ profile/ nav/
  firebase.json
  build.gradle.kts settings.gradle.kts gradle.properties
```
