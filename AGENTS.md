# BERUANG — repo notes

Native Android social app (Jetpack Compose + Firebase). The repo is Android-only.

## Android app (`android/`)
- Package: `com.altomedia.beruang`; Firebase project `altomedia-indonesia` (Storage bucket `altomedia-indonesia.firebasestorage.app`).
- `google-services.json` is committed under `android/app/`.
- Stack: Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7, Hilt 2.51.1, KSP 1.9.24-1.0.20, Compose + material-icons-extended, Firebase (Auth/Firestore/Storage), Coil, navigation-compose, lifecycle-viewmodel-compose.
- Firebase security rules: `android/app/firestore.rules` and `android/app/storage.rules` (users read/write own rows; posts/likes/comments/stories/global_messages/groups are public-read; notifications readable by owner; friendships bilateral; wallets readable, wallets.update open to any signed-in user for P2P transfer; transactions readable by sender/recipient). Deploy via `firebase deploy --only firestore:rules,storage`.
- Build env: `ANDROID_HOME=/opt/android-sdk` (platform-34, build-tools, platform-tools), `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (JDK 21). `local.properties` points `sdk.dir` at the SDK.

### Build
```
cd android
export ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:assembleDebug --no-daemon
```
- APK output: `android/app/build/outputs/apk/debug/app-debug.apk` (~22 MB).

### Firestore collections
`profiles`, `posts`, `likes`, `comments`, `stories`, `messages`, `global_messages`, `friendships`, `groups`, `group_members`, `notifications`, `wallets`, `transactions`.

### Points / wallet feature
- `profiles` carries `points` (snapshot), `account_id` (unique 6-digit), `points_pin` (SHA-256 hex of `beruang:<pin>`), `phone`, `email`, `gender` ('male'|'female'|'other'). `wallets(user_id, balance)` is the source of truth for the balance.
- Rewards (via `AccountsRepository.awardPoints`, called from `FeedRepository`/`FriendsRepository`): post +20, comment +50, friend accepted +10 (both users).
- Rank tiers (`RankTiers.kt`): Start(0)/Bronze(100)/Silver(500)/Gold(2000)/Master(10000) with badge drawables under `res/drawable/`.
- QR encodes the `account_id`; scan (`QrScannerScreen`) + amount + 4-digit PIN (`TransferDialog`) → `AccountsRepository.transfer` (validates PIN + balance, debits/credits wallets, records `transactions`).
- Known trade-off (flagged, not silently fixed): `wallets.update` is open to any signed-in user so a P2P transfer can credit the recipient from the sender's client. For full security move the debit/credit into a Cloud Function.

### Auth = phone-number login, no OTP
- Users sign in/register with a **phone number + password**. Firebase's **Email/Password** provider is used under the hood; the phone number is normalized to the `08xxxxxxxx` format and turned into a synthetic email `0812…@beruang.phone`. No SMS/OTP is ever sent.
- Normalization (`AuthViewModel.normalizePhone`): empty → `""`; `0…` → kept; `62…` → `0` + rest (drops the 62); otherwise `0` + digits (prepends 0). Must start with `08` and be 9–14 digits, else "Nomor HP harus diawali 08."
- `AuthRepository` is unchanged (still `signInWithEmailAndPassword` / `createUserWithEmailAndPassword`); the mapping happens in `AuthViewModel`. The `AuthScreen` field is `KeyboardType.Phone`.
- Trade-offs (flagged, not silently fixed): no proof the user owns the number (anyone can register someone else's number); no SMS password reset (recovery is password-only). Firebase Console must have Email/Password enabled.

### `ALTOMEDIA/` (not Android source)
Store-listing assets, release notes, privacy/terms HTML, and the release keystore/APK/AAB for the Play Store. The keystore (`ALTOMEDIA/keystore/beruang-release.jks`) is a secret — do not move it into a public location.

