import sys
import base64
import datetime
import json
import requests
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, utils
from cryptography.hazmat.backends import default_backend

KEY_ID = "2351030801"
PRIVATE_KEY_PATH = "rustore-private.pem"
PACKAGE_NAME = "com.vibe.messenger"
API_BASE = "https://public-api.rustore.ru/public"
APK_PATH = "TMessagesProj_App/build/outputs/apk/afat/release/app.apk"
ICON_PATH = "vibe-ui/config/vibe_icon_512.png"

def get_jwe_token():
    print("1. Getting JWE token...")
    
    with open(PRIVATE_KEY_PATH, "rb") as f:
        pem_data = f.read()
    
    # Fix PEM label: "RSA PRIVATE KEY" (PKCS#1) → "PRIVATE KEY" (PKCS#8) if needed
    if b"RSA PRIVATE KEY" in pem_data:
        pem_data = pem_data.replace(b"RSA PRIVATE KEY", b"PRIVATE KEY")
        print("Note: Converted PEM label from RSA PRIVATE KEY to PRIVATE KEY")
    
    private_key = serialization.load_pem_private_key(
        pem_data,
        password=None,
        backend=default_backend()
    )
    
    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
    message_to_sign = KEY_ID + timestamp
    print(f"Message to sign: {message_to_sign}")
    
    signature = private_key.sign(
        message_to_sign.encode("utf-8"),
        padding.PKCS1v15(),
        hashes.SHA512()
    )
    signature_b64 = base64.b64encode(signature).decode("utf-8")
    
    auth_body = {
        "keyId": KEY_ID,
        "timestamp": timestamp,
        "signature": signature_b64
    }
    
    resp = requests.post(f"{API_BASE}/auth/", json=auth_body)
    print(f"Auth response: {resp.status_code} {resp.text}")
    
    data = resp.json()
    if data.get("code") != "OK":
        print(f"Error: {data}")
        sys.exit(1)
    
    jwe = data["body"]["jwe"]
    print(f"JWE obtained (valid {data['body']['ttl']}s)")
    return jwe

def create_draft(jwe):
    print("\n2. Creating version draft...")
    
    body = {
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
    }
    
    resp = requests.post(
        f"{API_BASE}/v1/application/{PACKAGE_NAME}/version",
        headers={"Public-Token": jwe},
        json=body
    )
    print(f"Response: {resp.status_code} {resp.text}")
    
    data = resp.json()
    if data.get("code") != "OK":
        print(f"Error creating draft: {data}")
        sys.exit(1)
    
    version_id = data["body"]
    print(f"Version ID: {version_id}")
    return version_id

def upload_apk(jwe, version_id):
    print(f"\n3. Uploading APK...")
    
    import os
    apk_size = os.path.getsize(APK_PATH)
    print(f"APK size: {apk_size // 1024 // 1024} MB")
    
    with open(APK_PATH, "rb") as f:
        resp = requests.post(
            f"{API_BASE}/v1/application/{PACKAGE_NAME}/version/{version_id}/apk?isMainApk=true&servicesType=Unknown",
            headers={"Public-Token": jwe},
            files={"file": (os.path.basename(APK_PATH), f, "application/vnd.android.package-archive")}
        )
    
    print(f"Response: {resp.status_code} {resp.text}")
    return resp.json()

def upload_icon(jwe, version_id):
    print(f"\n4. Uploading icon...")
    
    import os
    if not os.path.exists(ICON_PATH):
        print("Warning: Icon not found")
        return
    
    with open(ICON_PATH, "rb") as f:
        resp = requests.post(
            f"{API_BASE}/v1/application/{PACKAGE_NAME}/version/{version_id}/icon",
            headers={"Public-Token": jwe},
            files={"file": (os.path.basename(ICON_PATH), f, "image/png")}
        )
    
    print(f"Response: {resp.status_code} {resp.text}")

def submit_moderation(jwe, version_id):
    print(f"\n5. Submitting for moderation...")
    
    resp = requests.post(
        f"{API_BASE}/v1/application/{PACKAGE_NAME}/version/{version_id}/moderation",
        headers={"Public-Token": jwe}
    )
    print(f"Response: {resp.status_code} {resp.text}")

if __name__ == "__main__":
    print("=== RuStore Upload ===")
    print(f"Package: {PACKAGE_NAME}\n")
    
    jwe = get_jwe_token()
    version_id = create_draft(jwe)
    upload_apk(jwe, version_id)
    upload_icon(jwe, version_id)
    submit_moderation(jwe, version_id)
    
    print("\n=== Done! ===")
    print("Check status in RuStore Console")
