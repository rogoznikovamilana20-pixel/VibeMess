import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.Base64;

public class RuStoreUploader {
    private static final String KEY_ID = "2351030801";
    private static final String PRIVATE_KEY_PATH = "rustore-private.pem";
    private static final String PACKAGE_NAME = "com.vibe.messenger";
    private static final String API_BASE = "https://public-api.rustore.ru/public";
    private static final String APK_PATH = "TMessagesProj_App/build/outputs/apk/afat/release/app.apk";
    private static final String ICON_PATH = "vibe-ui/config/vibe_icon_512.png";

    public static void main(String[] args) throws Exception {
        System.out.println("=== RuStore Upload ===");
        System.out.println("Package: " + PACKAGE_NAME);
        System.out.println();

        // Step 1: Get JWE token
        System.out.println("1. Getting JWE token...");
        String jwe = getJweToken();
        System.out.println("JWE token obtained (valid 900 seconds)");
        System.out.println("JWE length: " + jwe.length());
        System.out.println("JWE starts with: " + jwe.substring(0, Math.min(20, jwe.length())) + "...");
        System.out.println();

        // Step 2: Create version draft
        System.out.println("2. Creating version draft...");
        String draftBody = """
            {
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
            """;

        String draftResponse = makeRequest(
            API_BASE + "/v1/application/" + PACKAGE_NAME + "/version",
            "POST",
            jwe,
            draftBody
        );
        System.out.println("Response: " + draftResponse);

        String versionId = extractVersionId(draftResponse);
        if (versionId == null) {
            System.err.println("Error: Failed to extract version ID");
            System.exit(1);
        }
        System.out.println("Version ID: " + versionId);
        System.out.println();

        // Step 3: Upload APK
        System.out.println("3. Uploading APK...");
        File apkFile = new File(APK_PATH);
        if (!apkFile.exists()) {
            System.err.println("Error: APK not found at " + APK_PATH);
            System.exit(1);
        }
        System.out.println("APK size: " + (apkFile.length() / 1024 / 1024) + " MB");

        String apkResponse = makeMultipartRequest(
            API_BASE + "/v1/application/" + PACKAGE_NAME + "/version/" + versionId + "/apk?isMainApk=true&servicesType=Unknown",
            jwe,
            apkFile
        );
        System.out.println("Response: " + apkResponse);
        System.out.println();

        // Step 4: Upload icon
        System.out.println("4. Uploading icon...");
        File iconFile = new File(ICON_PATH);
        if (iconFile.exists()) {
            String iconResponse = makeMultipartRequest(
                API_BASE + "/v1/application/" + PACKAGE_NAME + "/version/" + versionId + "/icon",
                jwe,
                iconFile
            );
            System.out.println("Response: " + iconResponse);
        } else {
            System.out.println("Warning: Icon not found at " + ICON_PATH);
        }
        System.out.println();

        // Step 5: Submit for moderation
        System.out.println("5. Submitting for moderation...");
        String moderationResponse = makeRequest(
            API_BASE + "/v1/application/" + PACKAGE_NAME + "/version/" + versionId + "/moderation",
            "POST",
            jwe,
            null
        );
        System.out.println("Response: " + moderationResponse);
        System.out.println();

        System.out.println("=== Done! ===");
        System.out.println("Check status in RuStore Console");
    }

    private static String getJweToken() throws Exception {
        // Read private key
        String privateKeyPem = new String(Files.readAllBytes(Paths.get(PRIVATE_KEY_PATH)));
        privateKeyPem = privateKeyPem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\r?\\n", "")
            .replaceAll("\\s", "");

        System.out.println("Key length: " + privateKeyPem.length());

        byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);

        // Try PKCS8 first, then convert from PKCS1
        PrivateKey privateKey;
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(keySpec);
            System.out.println("Loaded as PKCS8");
        } catch (Exception e) {
            System.out.println("Not PKCS8, converting from PKCS1...");
            byte[] pkcs8Bytes = convertPkcs1ToPkcs8(keyBytes);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKey = kf.generatePrivate(keySpec);
            System.out.println("Converted PKCS1 -> PKCS8");
        }

        // Generate timestamp in UTC
        String timestamp = Instant.now()
            .atZone(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));

        // Create message to sign
        String messageToSign = KEY_ID + timestamp;
        System.out.println("Message to sign: " + messageToSign);

        // Sign with SHA512withRSA
        Signature signature = Signature.getInstance("SHA512withRSA");
        signature.initSign(privateKey);
        signature.update(messageToSign.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();
        String signatureValue = Base64.getEncoder().encodeToString(signatureBytes);

        // Get JWE token
        String authBody = String.format("""
            {
                "keyId": "%s",
                "timestamp": "%s",
                "signature": "%s"
            }
            """, KEY_ID, timestamp, signatureValue);

        String response = makeRequest(API_BASE + "/auth/", "POST", null, authBody);

        // Extract JWE
        int jweStart = response.indexOf("\"jwe\":\"") + 7;
        int jweEnd = response.indexOf("\"", jweStart);
        if (jweStart < 7 || jweEnd < 0) {
            throw new RuntimeException("Failed to extract JWE from response: " + response);
        }

        return response.substring(jweStart, jweEnd);
    }

    private static byte[] convertPkcs1ToPkcs8(byte[] pkcs1Bytes) {
        try {
            // PKCS8 header for RSA key
            // SEQUENCE { INTEGER 0, SEQUENCE { OID rsaEncryption, NULL }, OCTET STRING { pkcs1Key } }
            byte[] pkcs1Header = new byte[] {
                0x30, (byte)0x82, 0x00, 0x00, // SEQUENCE (placeholder for total length)
                0x02, 0x01, 0x00,               // INTEGER 0 (version)
                0x30, (byte)0x82, 0x00, 0x00,   // SEQUENCE (placeholder for algorithm length)
                0x06, 0x09, 0x2A, (byte)0x86, 0x48, (byte)0x86, (byte)0xF7, 0x0D, 0x01, 0x01, 0x01, // OID 1.2.840.113549.1.1.1
                0x05, 0x00,                       // NULL
                0x04, (byte)0x82, 0x00, 0x00    // OCTET STRING (placeholder for key length)
            };

            // Calculate lengths
            int pkcs1Length = pkcs1Bytes.length;
            int algIdLength = 15; // OID (11) + NULL (2) + tag+length (2) = 15 bytes in DER
            int keyLengthField = pkcs1Length + 2; // tag + length bytes for OCTET STRING
            int algIdTotalLength = algIdLength;
            int pkcs8TotalLength = 2 + algIdTotalLength + 2 + keyLengthField; // version + algId + octetString tag/len + key

            // Build the PKCS8 structure manually
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            // Top-level SEQUENCE
            baos.write(0x30);
            writeLength(baos, pkcs8TotalLength);

            // Version INTEGER = 0
            baos.write(0x02);
            baos.write(0x01);
            baos.write(0x00);

            // AlgorithmIdentifier SEQUENCE
            baos.write(0x30);
            writeLength(baos, algIdTotalLength);

            // OID for RSA Encryption
            baos.write(new byte[] {
                0x06, 0x09, 0x2A, (byte)0x86, 0x48, (byte)0x86, (byte)0xF7, 0x0D, 0x01, 0x01, 0x01
            });

            // NULL
            baos.write(0x05);
            baos.write(0x00);

            // OCTET STRING containing the PKCS1 key
            baos.write(0x04);
            writeLength(baos, pkcs1Length);
            baos.write(pkcs1Bytes);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert PKCS1 to PKCS8", e);
        }
    }

    private static void writeLength(java.io.ByteArrayOutputStream os, int length) throws IOException {
        if (length < 128) {
            os.write(length);
        } else if (length < 256) {
            os.write(0x81);
            os.write(length);
        } else {
            os.write(0x82);
            os.write((length >> 8) & 0xFF);
            os.write(length & 0xFF);
        }
    }

    private static String extractVersionId(String response) {
        int start = response.indexOf("\"message\":\"") + 11;
        int end = response.indexOf("\"", start);
        if (start < 11 || end < 0) return null;
        return response.substring(start, end);
    }

    private static String makeRequest(String urlStr, String method, String apiKey, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        if (apiKey != null) {
            conn.setRequestProperty("Public-Token", apiKey);
        }
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        if (body != null) {
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String response = sb.toString();
            if (code >= 400) {
                System.err.println("HTTP " + code + ": " + response);
            }
            return response;
        }
    }

    private static String makeMultipartRequest(String urlStr, String apiKey, File file) throws Exception {
        String boundary = "----VibeBoundary" + System.currentTimeMillis();
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Public-Token", apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(120000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(("--" + boundary + "\r\n").getBytes());
            os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n").getBytes());
            os.write("Content-Type: application/octet-stream\r\n\r\n".getBytes());
            os.write(Files.readAllBytes(file.toPath()));
            os.write(("\r\n--" + boundary + "--\r\n").getBytes());
        }

        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String response = sb.toString();
            if (code >= 400) {
                System.err.println("HTTP " + code + ": " + response);
            }
            return response;
        }
    }
}
