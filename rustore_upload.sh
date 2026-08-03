#!/bin/bash

# RuStore API Upload Script
# Auth: RSA signature → JWE token → API requests

set -e

# === CONFIG ===
KEY_ID="2351030800"
PRIVATE_KEY_PATH="rustore-private.pem"
PACKAGE_NAME="com.vibe.messenger"
API_BASE="https://public-api.rustore.ru/public"
APK_PATH="TMessagesProj_App/build/outputs/apk/afat/release/app.apk"
ICON_PATH="vibe-ui/config/vibe_icon_512.png"

echo "=== Getting JWE Token ==="

# Read private key
if [ ! -f "$PRIVATE_KEY_PATH" ]; then
    echo "Error: Private key not found at $PRIVATE_KEY_PATH"
    exit 1
fi

# Generate timestamp
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S.000+00:00")

# Create message to sign
MESSAGE_TO_SIGN="${KEY_ID}${TIMESTAMP}"
echo "Message to sign: $MESSAGE_TO_SIGN"

# Create signature (RSA SHA512)
SIGNATURE=$(echo -n "$MESSAGE_TO_SIGN" | openssl dgst -sha512 -sign "$PRIVATE_KEY_PATH" | openssl base64 -A)

# Get JWE token
AUTH_RESPONSE=$(curl -s -X POST "$API_BASE/auth/" \
    -H "Content-Type: application/json" \
    -d "{
        \"keyId\": \"$KEY_ID\",
        \"timestamp\": \"$TIMESTAMP\",
        \"signature\": \"$SIGNATURE\"
    }")

echo "Auth response: $AUTH_RESPONSE"

JWE=$(echo "$AUTH_RESPONSE" | jq -r '.body.jwe // empty')

if [ -z "$JWE" ]; then
    echo "Error: Failed to get JWE token"
    exit 1
fi

echo "JWE token obtained (valid 900 seconds)"
echo ""

# === STEP 2: Create Version Draft ===
echo "=== Creating Version Draft ==="
DRAFT_RESPONSE=$(curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version" \
    -H "Content-Type: application/json" \
    -H "API-key: $JWE" \
    -d '{
        "appName": "Vibe",
        "appType": "MAIN",
        "categories": ["communication"],
        "ageLegal": "12+",
        "shortDescription": "Защищённый мессенджер с E2EE",
        "fullDescription": "Vibe — мессенджер с сквозным шифрованием, Double Ratchet и post-quantum криптографией",
        "whatsNew": "Внедрено E2EE шифрование",
        "moderInfo": "Тестовый аккаунт: test@vibe.app / password123",
        "publishType": "MANUAL",
        "minAndroidVersion": 8,
        "developerContacts": {
            "email": "support@vibe.app"
        }
    }')

echo "$DRAFT_RESPONSE" | jq .
VERSION_ID=$(echo "$DRAFT_RESPONSE" | jq -r '.message // empty')

if [ -z "$VERSION_ID" ]; then
    echo "Error creating draft"
    exit 1
fi

echo "Version ID: $VERSION_ID"
echo ""

# === STEP 3: Upload APK ===
echo "=== Uploading APK ==="
if [ ! -f "$APK_PATH" ]; then
    echo "Error: APK not found at $APK_PATH"
    echo "Build first: ./gradlew :TMessagesProj_App:assembleAfatRelease"
    exit 1
fi

APK_RESPONSE=$(curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version/$VERSION_ID/apk?isMainApk=true&servicesType=Unknown" \
    -H "API-key: $JWE" \
    -F "file=@$APK_PATH")

echo "$APK_RESPONSE" | jq .
echo ""

# === STEP 4: Upload Icon ===
echo "=== Uploading Icon ==="
if [ ! -f "$ICON_PATH" ]; then
    echo "Warning: Icon not found at $ICON_PATH"
else
    ICON_RESPONSE=$(curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version/$VERSION_ID/icon" \
        -H "API-key: $JWE" \
        -F "file=@$ICON_PATH")
    echo "$ICON_RESPONSE" | jq .
fi
echo ""

# === STEP 5: Submit for Moderation ===
echo "=== Submitting for Moderation ==="
MODERATION_RESPONSE=$(curl -s -X POST "$API_BASE/v1/application/$PACKAGE_NAME/version/$VERSION_ID/moderation" \
    -H "API-key: $JWE")

echo "$MODERATION_RESPONSE" | jq .
echo ""

echo "=== Done! ==="
echo "Check status in RuStore Console"
