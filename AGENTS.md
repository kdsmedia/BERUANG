# BERUANG — repo notes

Native Android social app (Jetpack Compose + Supabase). The repo is Android-only.

## Android app (`android/`)
- Package: `com.altomedia.beruang`; developer **ALTOMEDIA** (contact `altomediaindonesia@gmail.com`).
- **Backend = Supabase** (was Firebase; migrated). The Firebase config (`google-services.json`, `firestore.rules`, `storage.rules`, `FirebaseModule.kt`) has been removed. Schema: `android/supabase/schema.sql`.
- Stack: Kotlin 1.9.24, AGP 8.10.1, Gradle 8.11.1, Hilt 2.51.1, KSP 1.9.24-1.0.20, Compose + material-icons-extended, **Supabase** (postgrest-kt/gotrue-kt/realtime-kt 2.5.2 + Ktor 2.3.12 OkHttp engine), Coil, navigation-compose, lifecycle-viewmodel-compose. compileSdk/targetSdk 37, minSdk 21, versionCode 4, versionName 1.2.0.
- Supabase RLS mirrors the old Firestore rules (users read/write own rows; posts/likes/comments/stories/global_messages/groups public-read; notifications readable by owner; friendships bilateral). `wallets` writes are blocked by RLS, so point transfers go through security-definer RPCs (`transfer_points`, `award_points`) in `schema.sql`.
- Build env: `ANDROID_HOME` set to an Android SDK with `platforms;android-37.0` (symlinked as `android-37` for AGP), `build-tools;37.0.0`, `platform-tools`; `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (JDK 21). `local.properties` points `sdk.dir` at the SDK.
- Release signing: `ALTOMEDIA.jks` (alias `kdsmedia`, password `Kdsmedia@123`), located at `ALTOMEDIA/keystore/`. Credentials read from `android/keystore.properties` (gitignored). DN: `CN=ALTOMEDIA, OU=Developer, O=ALTOMEDIA, L=Karawang, ST=Jawa Barat, C=ID`, validity 10000 days.

### Build
```
cd android
export ANDROID_HOME=<sdk> JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:assembleDebug --no-daemon          # debug APK
./gradlew :app:assembleRelease :app:bundleRelease --no-daemon  # signed release APK + AAB
```
- `:app:assembleDebug` builds with zero errors (debug APK ~34 MB). `:app:lintDebug` passes with no errors.
- Outputs: `android/app/build/outputs/apk/debug/app-debug.apk`, `.../apk/release/app-release.apk`, `.../bundle/release/app-release.aab`. Release artifacts are signed with the ALTOMEDIA keystore.

### Supabase migration notes (replaces the Firebase backend)
- `SupabaseModule.kt` (Hilt) provides `SupabaseClient`, `Auth` (gotrue), and `Realtime` singletons; installs Postgrest+Auth+Realtime plugins. Only the anon/publishable key is used client-side.
- Repositories inject **`SupabaseClient`** and derive `auth`/`postgrest`/`realtime` via the gotrue/postgrest/realtime **extension accessors** (`io.github.jan.supabase.gotrue.auth`, `io.github.jan.supabase.postgrest.postgrest`, `io.github.jan.supabase.realtime.realtime`, `.channel`).
- **Critical KSP/Hilt note (do not regress):** do NOT inject `Postgrest` and `Auth` together as separate `@Inject constructor` params in the same repository. KSP1 fails to resolve `io.github.jan.supabase.gotrue.Auth` in that combined signature and emits `error.NonExistentClass` for the `Auth` param (while `Postgrest` alone resolves fine). The `SupabaseClient`-only injection pattern works around this. ViewModels inject `Auth` or `Realtime` alone (never combined with `Postgrest`), so they don't hit the bug. KSP2 was tried and reverted (hits a separate `KSTypeArgument STAR null` bug).
- Auth = gotrue **Email/Password**; the synthetic `08…@beruang.phone` email trick from the Firebase build still applies (`AuthViewModel.normalizePhone`).
- Models are `@Serializable`; timestamps are ISO-8601 strings (`TimeUtil.isoNow()` replaces `Firebase.Timestamp.now()`).
- Realtime change flows: `channel.postgresChangeFlow<T>("public"){ table = ... }` after a `channel.subscribe()` (suspend), so the `*Changes()` repo methods are `suspend` and return a `Flow<PostgresAction>`.
- KSP2 was attempted (hits `KSTypeArgument.type should not have been null, STAR null`) and **reverted to KSP1**; `gradle.properties` keeps `org.gradle.jvmargs=-Xmx4096m`, `kotlin.daemon.jvmargs=-Xmx2g` (no KSP2 line).

### Build env re-provision (fresh container)
The JDK + Android SDK are not baked in; re-install on a fresh container:
```
sudo apt-get install -y openjdk-21-jdk-headless unzip
mkdir -p /home/openhands/android-sdk/cmdline-tools && cd $_
# download commandlinetools-linux-11076708_latest.zip, unzip, mv cmdline-tools latest
export ANDROID_HOME=/home/openhands/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
ln -s /home/openhands/android-sdk/platforms/android-37.0 /home/openhands/android-sdk/platforms/android-37
```

### Firestore collections
`profiles`, `posts`, `likes`, `comments`, `stories`, `messages`, `global_messages`, `friendships`, `groups`, `group_members`, `notifications`, `wallets`, `transactions`.

### Points / wallet feature
- `profiles` carries `points` (snapshot), `account_id` (unique 6-digit), `points_pin` (SHA-256 hex of `beruang:<pin>`), `phone`, `email`, `gender` ('male'|'female'|'other'). `wallets(user_id, balance)` is the source of truth for the balance.
- Rewards (via `AccountsRepository.awardPoints`, called from `FeedRepository`/`FriendsRepository`): post +20, comment +50, friend accepted +10 (both users).
- Rank tiers (`RankTiers.kt`): Start(0)/Bronze(100)/Silver(500)/Gold(2000)/Master(10000) with badge drawables under `res/drawable/`.
- QR encodes the `account_id`; scan (`QrScannerScreen`, fires once on main thread via AtomicBoolean guard) + amount + 4-digit PIN (`TransferDialog`) → `AccountsRepository.transfer` (validates PIN + balance, then debits sender + credits recipient + records `transactions` atomically in a single `Firestore.runBatch`).
- Known trade-off (flagged, not silently fixed): `wallets.update` is open to any signed-in user so a P2P transfer can credit the recipient from the sender's client. For full security move the debit/credit into a Cloud Function.

### Auth = phone-number login, no OTP
- Users sign in/register with a **phone number + password**. Firebase's **Email/Password** provider is used under the hood; the phone number is normalized to the `08xxxxxxxx` format and turned into a synthetic email `0812…@beruang.phone`. No SMS/OTP is ever sent.
- Normalization (`AuthViewModel.normalizePhone`): empty → `""`; `0…` → kept; `62…` → `0` + rest (drops the 62); otherwise `0` + digits (prepends 0). Must start with `08` and be 9–14 digits, else "Nomor HP harus diawali 08."
- `AuthRepository` is unchanged (still `signInWithEmailAndPassword` / `createUserWithEmailAndPassword`); the mapping happens in `AuthViewModel`. The `AuthScreen` field is `KeyboardType.Phone`.
- Trade-offs (flagged, not silently fixed): no proof the user owns the number (anyone can register someone else's number); no SMS password reset (recovery is password-only). Firebase Console must have Email/Password enabled.

### `ALTOMEDIA/` (not Android source)
Store-listing assets, release notes, privacy/terms HTML, and the release keystore/APK/AAB for the Play Store. The keystore (`ALTOMEDIA/keystore/beruang-release.jks`) is a secret — do not move it into a public location.

