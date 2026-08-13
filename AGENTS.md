# BERUANG — repo notes

Single-file Supabase-backed social app.

- `schema.sql` — run once in the Supabase SQL Editor. Contains 11 tables, RLS, storage buckets (`posts`, `avatars`), and an auto-profile trigger. **Do not modify** unless asked; `index.html` is written to match it exactly.
- `index.html` — self-contained app (HTML+CSS+JS). Swap `SUPABASE_URL` and `SUPABASE_ANON_KEY` near the top of the `<script>` before use. Only the anon/publishable key is used client-side.

Schema/JS invariants (must stay in sync):
- Tables referenced by `db.from('...')`: profiles, posts, likes, comments, stories, messages, global_messages, friendships, groups, group_members, notifications, wallets, transactions, point_events.
- Storage buckets (`storage.from('...')`): `posts`, `avatars`.
- All storage uploads use the path `${sessionUser.id}/<file>` so the folder matches `auth.uid()` per the RLS policies.
- RPC functions called via `db.rpc('...')`: `ensure_account_id`, `award_points`, `set_points_pin`, `transfer_points`. All are `SECURITY DEFINER`; `wallets`/`transactions`/`point_events` are owner-read-only via RLS and all writes go through these functions.
- Points/wallet feature (mirrors the Android app): `profiles` carries `points` (snapshot), `account_id` (unique 6-digit), `points_pin` (SHA-256 hex of `beruang:<pin>` via pgcrypto `digest`), `phone`, `email`, `gender` ('male'|'female'|'other'). `wallets(user_id, balance)` is the source of truth for the balance. Rewards: post +20, comment +50, friend accepted +10 (both users). Rank tiers: Start(0)/Bronze(100)/Silver(500)/Gold(2000)/Master(10000). QR encodes the `account_id`; scan + amount + 4-digit PIN → `transfer_points`.

Known security notes about the provided SQL (flagged, intentionally not fixed):
- `notifications` INSERT policy is `WITH CHECK (true)` — any authed user can insert a notification for any other user with arbitrary content.
- `friendships` UPDATE policy lets either party (sender or recipient) change `status`, so a request sender can self-accept their own pending request.

## Android app (`android/`)

A native Jetpack Compose + Firebase port of the web app lives under `android/`.
- Package: `com.altomedia.beruang`; Firebase project `altomedia-indonesia` (Storage bucket `altomedia-indonesia.firebasestorage.app`).
- `google-services.json` is committed under `android/app/`.
- Stack: Kotlin 1.9.24, AGP 8.5.2, Gradle 8.7, Hilt 2.51.1, KSP 1.9.24-1.0.20, Compose + material-icons-extended, Firebase (Auth/Firestore/Storage), Coil, navigation-compose, lifecycle-viewmodel-compose.
- Firebase security rules: `android/app/firestore.rules` and `android/app/storage.rules` mirror the Supabase RLS (users read/write own rows; posts/likes/comments/stories/global_messages/groups are public-read; notifications readable by owner; friendships bilateral). Deploy via `firebase deploy --only firestore:rules,storage`.
- Build env: `ANDROID_HOME=/opt/android-sdk` (platform-34, build-tools, platform-tools), `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` (JDK 21). `local.properties` points `sdk.dir` at the SDK.

### Build (verified passing)
```
cd android
export ANDROID_HOME=/opt/android-sdk JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew :app:assembleDebug --no-daemon
```
- `:app:compileDebugKotlin` and `:app:assembleDebug` both succeed (only deprecation/unused-param warnings).
- APK output: `android/app/build/outputs/apk/debug/app-debug.apk` (~22 MB).

### Auth = phone-number login, no OTP
- Users sign in/register with a **phone number + password**. Firebase's **Email/Password** provider is used under the hood; the phone number is normalized to the `08xxxxxxxx` format and turned into a synthetic email `0812…@beruang.phone`. No SMS/OTP is ever sent.
- Normalization (`AuthViewModel.normalizePhone`): empty → `""`; `0…` → kept; `62…` → `0` + rest (drops the 62); otherwise `0` + digits (prepends 0). Must start with `08` and be 9–14 digits, else "Nomor HP harus diawali 08."
- `AuthRepository` is unchanged (still `signInWithEmailAndPassword` / `createUserWithEmailAndPassword`); the mapping happens in `AuthViewModel`. The `AuthScreen` field is `KeyboardType.Phone`.
- Trade-offs (flagged, not silently fixed): no proof the user owns the number (anyone can register someone else's number); no SMS password reset (recovery is password-only). Firebase Console must have Email/Password enabled (see README step 1).

### Notable porting fixes (vs. the web source)
- `RelTime.kt` / `ProfileScreen` use `SimpleDateFormat` instead of the removed `Date.toLocaleDateString()`.
- Filter tabs in `HomeScreen` use `items(tabs)` inside the `LazyRow` (the old `forEach` placed `@Composable` calls outside a composable scope).
- `AppBottomSheet` content is typed `@Composable ColumnScope.() -> Unit` so `.weight()` resolves inside pickers; grid emoji list uses `androidx.compose.foundation.lazy.grid.items`.
- All `Modifier.clickable(fn)` call-sites rewritten as `clickable { fn() }` to avoid positional-arg overload ambiguity.
- Bottom-nav icons use stable names (`Icons.Outlined.Notifications`, `.Person`, `.House`, `.Group`, `AutoMirrored.Outlined.Comment`) instead of `Bell`/`CircleUser`.
- `RootNav` imports `HomeScreen` from `...ui.home` (not `...ui.feed`) and threads `onNavigate` into `NavigationIcon`.

