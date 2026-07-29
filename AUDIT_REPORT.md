# Vibe Messenger — Full Code Audit

## Executive Summary

| Check | Status | Details |
|-------|--------|---------|
| **Compilation** | ✅ PASS | 0 errors across vibe-common, vibe-bridge, vibe-ui |
| **Unit Tests** | ✅ PASS | 7 test classes, all passed |
| **Android Lint** | ❌ 35 errors, 379 warnings | Runtime crash risks, API level violations |
| **Security Review** | ⚠️ E2EE disabled | Known & documented — needs library replacement |
| **Code Quality** | ⚠️ Moderate | Many unused params, deprecated APIs, potential NPEs |

---

## 🔴 Critical Issues (fix immediately)

### 1. WrongViewCast — Runtime crash on 3 screens
**Files:** `CallsActivity.kt:25`, `ContactsActivity.kt:31`, `SearchActivity.kt:36`

`R.id.empty_state` is a `<LinearLayout>` in XML, but code casts it to `TextView`. Will crash with `ClassCastException` when `empty_state` is accessed.

```kotlin
// BUG: empty_state is LinearLayout in XML, not TextView
val emptyState = findViewById<TextView>(R.id.empty_state)
```

### 2. MissingClass — FloatingActionButton not found
**File:** `vibe_screen_main.xml:128`

Layout references `com.google.android.material.floatingactionbutton.FloatingActionButton` but the Material library is not in dependencies. Will crash on inflation.

### 3. NewApi violations — Crash on API 21–22 devices
**Files:** `KeyManager.kt:50-58`, `SecureKeyManager.kt:62-71`, `MessageEncryptor.kt`

Android Keystore APIs (`KeyGenParameterSpec.Builder`, `setBlockModes`, `setEncryptionPaddings`, `setKeySize`) require API 23+. `minSdk = 21`. No `@RequiresApi` or version guard — **crashes on Android 5.x**.

### 4. MqttSignaling — infinite reconnect without backoff
**File:** `MqttSignaling.kt:125-131`

On connection loss, `reconnect()` is called immediately. If broker is down, it loops infinitely — battery drain.

### 5. E2EE is disabled — mathematically broken
**File:** `SecurityManager.kt:13-20`

> "The previous E2EE implementation was mathematically broken: used secp256r1 instead of X25519, incorrect HKDF, sessions never persisted."

All `encryptMessage()`/`decryptMessage()` return null. Without E2EE, calls and messages are NOT end-to-end encrypted.

---

## 🟡 Medium Issues

### 6. SupabaseSignaling — thread blocking
**File:** `SupabaseSignaling.kt:222-228`

`reconnect()` calls `Thread.sleep(3000)` on the WebSocket callback thread. Blocks the OkHttp WebSocket thread pool.

### 7. CallManager — non-null assertions on nullables
**Files:** `CallManager.kt:193`, `CallManager.kt:206`, `CallManager.kt:222`, `CallManager.kt:252`

Uses `!!` on nullable fields (`peerConnectionFactory`, `localStream`, `peerConnection`). If any of these are null at call time → **NPE crash**.

```kotlin
localStream = peerConnectionFactory?.createLocalMediaStream(...)!!
```

### 8. Only STUN, no TURN servers
**File:** `CallManager.kt:63-67`

Only Google public STUN servers configured. Calls behind symmetric NAT will fail. TURN credentials should come from signaling.

### 9. Deprecated APIs
- `KeyManager.kt:58` — `setUserAuthenticationValidityDurationSeconds()` deprecated in API 30
- `SplashActivity.kt:284` — `overridePendingTransition(0,0)` deprecated
- `VibeComposeTheme.kt:84-85` — `statusBarColor`/`navigationBarColor` setters deprecated
- `CallScreen.kt:313` — `Icons.Filled.VolumeUp` deprecated, use AutoMirrored
- `CallScreen.kt:320` — `AudioManager.isSpeakerphoneOn` setter deprecated

### 10. API level warnings (InlinedApi) — need @RequiresApi guards
- `IntegrityVerifier.kt:100` — `GET_SIGNING_CERTIFICATES` requires API 28
- `VibeCallService.kt:34` — `STOP_FOREGROUND_REMOVE` requires API 24
- `KeyManager.kt`, `SecureKeyManager.kt` — multiple API 23+ constants without guard

---

## 🔵 Low Issues

### 11. Unused code (40+ variables/parameters)
Across `DeviceCapabilityDetector`, `QualityAdaptationManager`, `MqttSignaling`, `SupabaseSignaling`, `VibeButton`, `VibeChatBubble`, `VaybikCharacter`, `VibeAvatar`, `VibeInput`, `MarketplaceScreen`, `SettingsStorageScreen`, `VibeComposeTheme`, `WelcomeActivity`, `SplashActivity`, `ContactsActivity`, `SearchActivity`, `CallsActivity`, `SecurityManager`, etc.

### 12. Named parameter mismatches in SupabaseSignaling
**File:** `SupabaseSignaling.kt:39,45,49,54,59`

`WebSocketListener` callbacks use parameter name `webSocket` but code uses `ws`. Works but fails with named arguments.

### 13. Parameter shadowing
- `VibeAvatar.kt:62` — `primaryColor` shadows outer scope
- `SettingsStorageScreen.kt:66` — `db` shadows outer `db`
- `AuthActivity.kt:145` — `otpContainer` shadows field

---

## 📊 Stats

| Metric | Value |
|--------|-------|
| Total source files reviewed | 90+ |
| Total lines of Kotlin | ~12,000 |
| 🔴 Critical bugs | 5 |
| 🟡 Medium issues | 5 |
| 🔵 Low/cleanup | 4 |
| Lint errors | 35 |
| Lint warnings | 379 |
| Test coverage | 7 test classes (low) |
