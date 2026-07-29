# Vibe Messenger — Project Context for opencode

## Project
Vibe Messenger — Telegram-клиент с AI-ассистентом (Aurion), WebRTC-звонками, кастомным UI (Material 3, Compose).

## Architecture
Sidecar/Bridge pattern. Telegram core (`TMessagesProj`) untouched. Vibe-логика в отдельных модулях.

## Modules
| Module | Type | Description |
|--------|------|-------------|
| `:TMessagesProj` | library | Telegram core (Java) |
| `:TMessagesProj_App` | app | Main Telegram entry point |
| `:vibe-common` | lib | Shared utils, logging, result types |
| `:vibe-bridge` | lib | Glue layer — реализует Vibe-интерфейсы через Telegram API |
| `:vibe-ui` | lib | UI-компоненты (Compose), Room DB, MQTT, Supabase |

## Gradle
- AGP 8.6.1, Kotlin 1.9.20, KSP, JDK 17
- Gradle 8.14.5 wrapper

## Commands
```bash
# Tests
./gradlew testDebugUnitTest                                    # all modules
./gradlew :vibe-common:testDebugUnitTest :vibe-ui:testDebugUnitTest  # only vibe modules

# Kotlin compile check
./gradlew :vibe-common:compileDebugKotlin :vibe-bridge:compileDebugKotlin :vibe-ui:compileDebugKotlin

# Android lint
./gradlew :vibe-ui:lintDebug

# Build APK
./gradlew :TMessagesProj_App:assembleAfatDebug
```

## Signing credentials flow
1. `settings.gradle` читает из `local.properties` в `gradle.ext`
2. `build.gradle` (root) копирует `gradle.ext.*` → `project.ext.*` для всех subprojects
3. Нужные значения: `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, `VIBE_KEYSTORE_PATH`

## local.properties (local dev)
```
sdk.dir=C:\Users\andre\AppData\Local\Android\Sdk
RELEASE_KEY_PASSWORD=vibe2026
RELEASE_KEY_ALIAS=vibe-release
RELEASE_STORE_PASSWORD=vibe2026
VIBE_KEYSTORE_PATH=vibe-ui/config/vibe-release.jks
AI_API_KEY=your-openrouter-api-key
```

## CI (.github/workflows/ci.yml)
- `test` job: unit tests + Kotlin compile (fast check)
- `lint` job: `:vibe-ui:lintDebug`
- `apk` job: `:TMessagesProj_App:assembleAfatDebug` (только main/tags)
- Secrets: `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `AI_API_KEY`

## Key dependencies
- AndroidX Compose BOM 2024.02
- Room 2.6.1, KSP
- Lottie Compose 6.4.0
- Paging 3.2.1, MockK 1.13.10
- MQTT (Paho) 1.2.5, OkHttp 4.12.0
- Firebase Crashlytics + Analytics
- Supabase (via OkHttp WebSocket)

## Local.properties example
`sdk.dir` + signing creds + `AI_API_KEY` + `SUPABASE_URL` + `SUPABASE_ANON_KEY`

## Tracking
- `VIBE_CURRENT_STATE.md` — текущий статус и план
- `DESIGN_SYSTEM.md` — дизайн-система (цвета, типографика, компоненты)
