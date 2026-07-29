package com.vibe.ui.call

data class QualityConfig(
    val label: String,
    val audioBitrateBps: Int,
    val audioStereo: Boolean,
    val audioSampleRate: Int,
    val audioFec: Boolean,
    val audioDtx: Boolean,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoFps: Int,
    val videoMaxBitrateBps: Int,
    val videoMinBitrateBps: Int,
    val videoStartBitrateBps: Int,
    val jitterBufferMaxPackets: Int,
    val jitterBufferFastAccelerate: Boolean,
    val enableDscp: Boolean,
    val cpuOveruseDetection: Boolean,
    val hardwareEncoding: Boolean,
    val enableH264HighProfile: Boolean,
    val enableVp9: Boolean,
    val enableH265: Boolean
) {
    companion object {
        val FLAGSHIP = QualityConfig(
            label = "FLAGSHIP",
            audioBitrateBps = 128000,
            audioStereo = true,
            audioSampleRate = 48000,
            audioFec = true,
            audioDtx = false,
            videoWidth = 1920,
            videoHeight = 1080,
            videoFps = 60,
            videoMaxBitrateBps = 8_000_000,
            videoMinBitrateBps = 500_000,
            videoStartBitrateBps = 4_000_000,
            jitterBufferMaxPackets = 75,
            jitterBufferFastAccelerate = true,
            enableDscp = true,
            cpuOveruseDetection = false,
            hardwareEncoding = true,
            enableH264HighProfile = true,
            enableVp9 = true,
            enableH265 = true
        )

        val HIGH = QualityConfig(
            label = "HIGH",
            audioBitrateBps = 96000,
            audioStereo = true,
            audioSampleRate = 48000,
            audioFec = true,
            audioDtx = false,
            videoWidth = 1920,
            videoHeight = 1080,
            videoFps = 30,
            videoMaxBitrateBps = 5_000_000,
            videoMinBitrateBps = 300_000,
            videoStartBitrateBps = 3_000_000,
            jitterBufferMaxPackets = 50,
            jitterBufferFastAccelerate = true,
            enableDscp = true,
            cpuOveruseDetection = true,
            hardwareEncoding = true,
            enableH264HighProfile = true,
            enableVp9 = false,
            enableH265 = false
        )

        val MID = QualityConfig(
            label = "MID",
            audioBitrateBps = 64000,
            audioStereo = true,
            audioSampleRate = 48000,
            audioFec = true,
            audioDtx = true,
            videoWidth = 1280,
            videoHeight = 720,
            videoFps = 30,
            videoMaxBitrateBps = 3_000_000,
            videoMinBitrateBps = 200_000,
            videoStartBitrateBps = 2_000_000,
            jitterBufferMaxPackets = 50,
            jitterBufferFastAccelerate = true,
            enableDscp = false,
            cpuOveruseDetection = true,
            hardwareEncoding = true,
            enableH264HighProfile = false,
            enableVp9 = false,
            enableH265 = false
        )

        val LOW = QualityConfig(
            label = "LOW",
            audioBitrateBps = 32000,
            audioStereo = false,
            audioSampleRate = 48000,
            audioFec = true,
            audioDtx = true,
            videoWidth = 640,
            videoHeight = 480,
            videoFps = 30,
            videoMaxBitrateBps = 1_500_000,
            videoMinBitrateBps = 100_000,
            videoStartBitrateBps = 800_000,
            jitterBufferMaxPackets = 100,
            jitterBufferFastAccelerate = true,
            enableDscp = false,
            cpuOveruseDetection = true,
            hardwareEncoding = false,
            enableH264HighProfile = false,
            enableVp9 = false,
            enableH265 = false
        )

        fun all(): List<QualityConfig> = listOf(FLAGSHIP, HIGH, MID, LOW)

        fun fromLabel(label: String): QualityConfig = when (label) {
            "FLAGSHIP" -> FLAGSHIP
            "HIGH" -> HIGH
            "MID" -> MID
            else -> LOW
        }
    }
}
