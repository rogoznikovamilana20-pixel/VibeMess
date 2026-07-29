package com.vibe.ui.call

import org.webrtc.SessionDescription

object SdpQualityEnhancer {

    private var currentConfig: QualityConfig = QualityConfig.MID

    fun setConfig(config: QualityConfig) {
        currentConfig = config
    }

    fun enhance(sdp: SessionDescription): SessionDescription {
        val enhanced = enhanceSdpText(sdp.description, sdp.type)
        return SessionDescription(sdp.type, enhanced)
    }

    private fun enhanceSdpText(sdp: String, type: SessionDescription.Type): String {
        val result = enhanceOpusParams(sdp)
        return enhanceVideoParams(result)
    }

    private fun enhanceOpusParams(sdp: String): String {
        val opusPt = extractOpusPayloadType(sdp) ?: return sdp
        val oldFmtp = "a=fmtp:$opusPt "
        val oldRegex = Regex("$oldFmtp.*")
        val existingMatch = oldRegex.find(sdp)

        val stereo = if (currentConfig.audioStereo) "1" else "0"
        val fec = if (currentConfig.audioFec) "1" else "0"
        val dtx = if (currentConfig.audioDtx) "1" else "0"
        val bitrate = currentConfig.audioBitrateBps / 1000

        val newFmtpLine = "${oldFmtp}maxaveragebitrate=$bitrate;stereo=$stereo;useinbandfec=$fec;dtx=$dtx"

        return if (existingMatch != null) {
            sdp.replace(existingMatch.value, newFmtpLine)
        } else {
            val rtpmapRegex = Regex("a=rtpmap:$opusPt opus/.*")
            val rtpmapMatch = rtpmapRegex.find(sdp)
            if (rtpmapMatch != null) {
                sdp.replace(rtpmapMatch.value, rtpmapMatch.value + "\n$newFmtpLine")
            } else {
                sdp
            }
        }
    }

    private fun extractOpusPayloadType(sdp: String): String? {
        val regex = Regex("a=rtpmap:(\\d+) opus/")
        return regex.find(sdp)?.groupValues?.getOrNull(1)
    }

    private fun enhanceVideoParams(sdp: String): String {
        val h264Pt = extractPayloadType(sdp, "H264")
        if (h264Pt != null && currentConfig.enableH264HighProfile) {
            val highProfileLevel = "640032"
            val oldRegex = Regex("a=fmtp:$h264Pt .*")
            val match = oldRegex.find(sdp)
            if (match != null) {
                val value = match.value
                val withProfile = if (value.contains("profile-level-id")) {
                    value.replace(Regex("profile-level-id=[0-9a-fA-F]+"), "profile-level-id=$highProfileLevel")
                } else {
                    "$value;profile-level-id=$highProfileLevel"
                }
                val withHighBitrate = if (withProfile.contains("max-br")) {
                    withProfile.replace(Regex("max-br=\\d+"), "max-br=8000")
                } else {
                    "$withProfile;max-br=8000"
                }
                return sdp.replace(value, withHighBitrate)
            }
        }

        val vp9Pt = extractPayloadType(sdp, "VP9")
        if (vp9Pt != null && currentConfig.enableVp9) {
            val oldRegex = Regex("a=fmtp:$vp9Pt .*")
            val match = oldRegex.find(sdp)
            if (match != null) {
                val withProfile = if (match.value.contains("profile-id")) {
                    match.value
                } else {
                    "${match.value};profile-id=2"
                }
                return sdp.replace(match.value, withProfile)
            }
        }

        return sdp
    }

    private fun extractPayloadType(sdp: String, codec: String): String? {
        val regex = Regex("a=rtpmap:(\\d+) $codec/")
        return regex.find(sdp)?.groupValues?.getOrNull(1)
    }
}
