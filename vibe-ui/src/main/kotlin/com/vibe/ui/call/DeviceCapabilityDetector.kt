package com.vibe.ui.call

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Size
import android.content.Context

object DeviceCapabilityDetector {

    private var cachedConfig: QualityConfig? = null

    fun detect(context: Context): QualityConfig {
        cachedConfig?.let { return it }

        val config = computeConfig(context)
        cachedConfig = config
        return config
    }

    private fun computeConfig(context: Context): QualityConfig {
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val totalRamMb = getTotalRamMb()
        val socModel = (Build.HARDWARE + " " + Build.MODEL + " " + Build.MANUFACTURER).lowercase()
        val cpuArch = Build.SUPPORTED_ABIS.joinToString(",")
        val hasH264HighProfile = supportsCodec(MediaFormat.MIMETYPE_VIDEO_AVC, true)
        val hasH265 = supportsCodec(MediaFormat.MIMETYPE_VIDEO_HEVC, false)
        val hasVp9 = supportsCodec(MediaFormat.MIMETYPE_VIDEO_VP9, false)
        val cameraMaxRes = getCameraMaxResolution(context)

        return when {
            isFlagship(cpuCores, totalRamMb, socModel, hasH264HighProfile, hasH265, cameraMaxRes) -> QualityConfig.FLAGSHIP
            isHigh(cpuCores, totalRamMb, socModel, hasH264HighProfile, cameraMaxRes) -> QualityConfig.HIGH
            isMid(cpuCores, totalRamMb, socModel, cameraMaxRes) -> QualityConfig.MID
            else -> QualityConfig.LOW
        }
    }

    private fun isFlagship(
        cpuCores: Int, ramMb: Long, soc: String,
        hasH264High: Boolean, hasH265: Boolean, camMax: Size?
    ): Boolean {
        if (ramMb < 6000) return false
        if (cpuCores < 8) return false
        val camOk = camMax == null || (camMax.width >= 1920 && camMax.height >= 1080)
        return camOk && (hasH265 || hasH264High)
    }

    private fun isHigh(
        cpuCores: Int, ramMb: Long, soc: String,
        hasH264High: Boolean, camMax: Size?
    ): Boolean {
        if (ramMb < 4000) return false
        if (cpuCores < 6) return false
        val camOk = camMax == null || (camMax.width >= 1280)
        return camOk && hasH264High
    }

    private fun isMid(
        cpuCores: Int, ramMb: Long, soc: String, camMax: Size?
    ): Boolean {
        if (ramMb < 2000) return false
        if (cpuCores < 4) return false
        return true
    }

    private fun getTotalRamMb(): Long {
        return try {
            val reader = java.io.RandomAccessFile("/proc/meminfo", "r")
            val line = reader.readLine()
            reader.close()
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) parts[1].toLong() / 1024 else 2048L
        } catch (_: Exception) {
            2048
        }
    }

    private fun supportsCodec(mimeType: String, highProfile: Boolean): Boolean {
        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        for (info in codecList.codecInfos) {
            if (!info.isEncoder) continue
            for (mime in info.supportedTypes) {
                if (mime == mimeType) {
                    if (highProfile && mimeType == MediaFormat.MIMETYPE_VIDEO_AVC) {
                        val name = info.name.lowercase()
                        if (name.contains("hevc") || name.contains("vp9")) continue
                        return name.contains("qcom") || name.contains("exynos") ||
                               name.contains("omx.google") || name.contains("c2.android")
                    }
                    return true
                }
            }
        }
        return false
    }

    private fun getCameraMaxResolution(context: Context): Size? {
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = manager.cameraIdList.firstOrNull() ?: return null
            val chars = manager.getCameraCharacteristics(cameraId)
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return null
            val outSizes = map.getOutputSizes(android.graphics.ImageFormat.YUV_420_888) ?:
                           map.getOutputSizes(android.view.SurfaceHolder::class.java)
            outSizes?.maxByOrNull { it.width * it.height }
        } catch (_: Exception) {
            null
        }
    }
}
