# RuStore API Upload Script (Windows PowerShell)
# Auth: RSA signature → JWE token → API requests

$ErrorActionPreference = "Stop"

# === CONFIG ===
$KEY_ID = "2351030800"  # ← Вставь ID ключа из RuStore Console
$PRIVATE_KEY_PATH = "rustore-private.pem"  # Файл с приватным ключом
$PACKAGE_NAME = "com.vibe.messenger"
$API_BASE = "https://public-api.rustore.ru/public"
$APK_PATH = "TMessagesProj_App\build\outputs\apk\afat\release\app.apk"
$ICON_PATH = "vibe-ui\config\vibe_icon_512.png"

# === STEP 1: Get JWE Token ===
Write-Host "=== Getting JWE Token ===" -ForegroundColor Cyan

# Read private key
if (-not (Test-Path $PRIVATE_KEY_PATH)) {
    Write-Host "Error: Private key not found at $PRIVATE_KEY_PATH" -ForegroundColor Red
    Write-Host "Save your private key to this file"
    exit 1
}

$privateKey = Get-Content $PRIVATE_KEY_PATH -Raw

# Generate timestamp
$timestamp = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffzzz")

# Create message to sign
$messageToSign = $KEY_ID + $timestamp
Write-Host "Message to sign: $messageToSign"

# Create signature (RSA SHA512)
# Note: This requires OpenSSL or .NET crypto
$signature = & openssl dgst -sha512 -sign $PRIVATE_KEY_PATH | openssl base64 -A

# Get JWE token
$authBody = @{
    keyId = $KEY_ID
    timestamp = $timestamp
    signature = $signature
} | ConvertTo-Json

$authResponse = curl -s -X POST "$API_BASE/auth/" `
    -H "Content-Type: application/json" `
    -d $authBody

Write-Host "Auth response: $authResponse"

$jwe = ($authResponse | ConvertFrom-Json).body.jwe
if (-not $jwe) {
    Write-Host "Error: Failed to get JWE token" -ForegroundColor Red
    exit 1
}

Write-Host "JWE token obtained (valid 900 seconds)" -ForegroundColor Green
Write-Host ""

# === STEP 2: Create Version Draft ===
Write-Host "=== Creating Version Draft ===" -ForegroundColor Cyan
$draftBody = @{
    appName = "Vibe"
    appType = "MAIN"
    categories = @("communication")
    ageLegal = "12+"
    shortDescription = "Защищённый мессенджер с E2EE"
    fullDescription = "Vibe — мессенджер с сквозным шифрованием, Double Ratchet и post-quantum криптографией"
    whatsNew = "Внедрено E2EE шифрование"
    moderInfo = "Тестовый аккаунт: test@vibe.app / password123"
    publishType = "MANUAL"
    minAndroidVersion = 8
    developerContacts = @{
        email = "support@vibe.app"
    }
} | ConvertTo-Json

$draftResponse = curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version" `
    -H "Content-Type: application/json" `
    -H "API-key: $jwe" `
    -d $draftBody

Write-Host "Draft response: $draftResponse"

$VERSION_ID = ($draftResponse | ConvertFrom-Json).message
if (-not $VERSION_ID) {
    Write-Host "Error creating draft" -ForegroundColor Red
    exit 1
}

Write-Host "Version ID: $VERSION_ID"
Write-Host ""

# === STEP 3: Upload APK ===
Write-Host "=== Uploading APK ===" -ForegroundColor Cyan
if (-not (Test-Path $APK_PATH)) {
    Write-Host "Error: APK not found at $APK_PATH" -ForegroundColor Red
    Write-Host "Build first: .\gradlew :TMessagesProj_App:assembleAfatRelease"
    exit 1
}

$apkResponse = curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version/$VERSION_ID/apk?isMainApk=true&servicesType=Unknown" `
    -H "API-key: $jwe" `
    -F "file=@$APK_PATH"

Write-Host "APK response: $apkResponse"
Write-Host ""

# === STEP 4: Upload Icon ===
Write-Host "=== Uploading Icon ===" -ForegroundColor Cyan
if (Test-Path $ICON_PATH) {
    $iconResponse = curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version/$VERSION_ID/icon" `
        -H "API-key: $jwe" `
        -F "file=@$ICON_PATH"
    Write-Host "Icon response: $iconResponse"
} else {
    Write-Host "Warning: Icon not found at $ICON_PATH" -ForegroundColor DarkYellow
}
Write-Host ""

# === STEP 5: Submit for Moderation ===
Write-Host "=== Submitting for Moderation ===" -ForegroundColor Cyan
$moderationResponse = curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version/$VERSION_ID/moderation" `
    -H "API-key: $jwe"

Write-Host "Moderation response: $moderationResponse"
Write-Host ""

Write-Host "=== Done! ===" -ForegroundColor Green
Write-Host "Check status in RuStore Console"
