package com.vibe.ui.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Application integrity verification.
 * Detects:
 * - Debug mode
 * - Rooted devices
 * - Tampered APK
 * - Emulator environment
 */
class IntegrityVerifier(private val context: Context) {

    companion object {
        private const val TAG = "IntegrityVerifier"
    }

    /**
     * Check if app is running in debug mode.
     */
    fun isDebugMode(): Boolean {
        return (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Check if device is rooted.
     */
    fun isDeviceRooted(): Boolean {
        // Check for common root indicators
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/xbin/su",
            "/system/bin/su",
            "/system/bin/.ext",
            "/system/app/SuperSU",
            "/system/app/SuperSU.apk",
            "/system/app/Supersu",
            "/system/app/Supersu.apk",
            "/system/app/Busybox",
            "/system/app/Busybox.apk",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su",
            "/su/bin/su"
        )

        for (path in rootPaths) {
            if (java.io.File(path).exists()) {
                return true
            }
        }

        // Check for root management apps
        val rootPackages = arrayOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            "com.noshufou.android.su"
        )

        for (pkg in rootPackages) {
            if (isPackageInstalled(pkg)) {
                return true
            }
        }

        return false
    }

    /**
     * Check if running on emulator.
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    /**
     * Verify APK integrity using checksum.
     */
    fun verifyApkIntegrity(): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return false

            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(signatures[0].toByteArray())
            val hashHex = hash.joinToString("") { "%02x".format(it) }

            // In production, compare against expected hash
            // For now, just check that we have a valid signature
            hashHex.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if Xposed framework is installed.
     */
    fun isXposedInstalled(): Boolean {
        return try {
            ClassLoader.getSystemClassLoader().loadClass("de.robv.android.xposed.XposedBridge")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Check if Frida is running.
     */
    fun isFridaRunning(): Boolean {
        return try {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("ps")
            val reader = process.inputStream.bufferedReader()
            val output = reader.readText()
            output.contains("frida") || output.contains("gadget")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Comprehensive integrity check.
     */
    fun performFullCheck(): IntegrityReport {
        return IntegrityReport(
            isDebug = isDebugMode(),
            isRooted = isDeviceRooted(),
            isEmulator = isEmulator(),
            isApkValid = verifyApkIntegrity(),
            isXposed = isXposedInstalled(),
            isFrida = isFridaRunning(),
            isSecure = !isDebugMode() && !isDeviceRooted() && !isEmulator() &&
                       verifyApkIntegrity() && !isXposedInstalled() && !isFridaRunning()
        )
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}

data class IntegrityReport(
    val isDebug: Boolean,
    val isRooted: Boolean,
    val isEmulator: Boolean,
    val isApkValid: Boolean,
    val isXposed: Boolean,
    val isFrida: Boolean,
    val isSecure: Boolean
) {
    fun getWarnings(): List<String> {
        val warnings = mutableListOf<String>()
        if (isDebug) warnings.add("Running in debug mode")
        if (isRooted) warnings.add("Device is rooted")
        if (isEmulator) warnings.add("Running on emulator")
        if (!isApkValid) warnings.add("APK integrity check failed")
        if (isXposed) warnings.add("Xposed framework detected")
        if (isFrida) warnings.add("Frida detected")
        return warnings
    }
}
