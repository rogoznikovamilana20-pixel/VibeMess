package com.vibe.ui.call

import android.os.Handler
import android.os.Looper
import org.webrtc.PeerConnection
import org.webrtc.RTCStatsCollectorCallback
import org.webrtc.RTCStatsReport
import org.webrtc.RtpParameters
import org.webrtc.RtpSender

class QualityAdaptationManager(
    private val peerConnection: PeerConnection,
    private val config: QualityConfig
) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var currentTargetBitrate = config.videoStartBitrateBps

    private val statsRunnable = object : Runnable {
        override fun run() {
            if (!running) return
            peerConnection.getStats(statsCallback)
            handler.postDelayed(this, 2000)
        }
    }

    private val statsCallback = object : RTCStatsCollectorCallback {
        override fun onStatsDelivered(report: RTCStatsReport) {
            analyzeAndAdapt(report)
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.post(statsRunnable)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(statsRunnable)
    }

    private fun analyzeAndAdapt(report: RTCStatsReport) {
        var currentRttMs = 0.0
        var packetsLost = 0L
        var packetsReceived = 0L

        for (stat in report.statsMap.values) {
            when {
                stat.type == "candidate-pair" && stat.getMembers()["state"] == "succeeded" -> {
                    currentRttMs = (stat.getMembers()["currentRoundTripTime"] as? Double)?.let { it * 1000 } ?: 0.0
                }
                stat.type == "inbound-rtp" -> {
                    packetsLost = (stat.getMembers()["packetsLost"] as? Long) ?: 0
                    packetsReceived = (stat.getMembers()["packetsReceived"] as? Long) ?: 0
                }
            }
        }

        val lossRate = if (packetsReceived + packetsLost > 0) {
            packetsLost.toDouble() / (packetsReceived + packetsLost)
        } else 0.0

        adaptBitrate(currentRttMs, lossRate)
    }

    private fun adaptBitrate(rttMs: Double, lossRate: Double) {
        val maxBitrate = config.videoMaxBitrateBps
        val minBitrate = config.videoMinBitrateBps

        when {
            rttMs < 100 && lossRate < 0.01 -> {
                currentTargetBitrate = (currentTargetBitrate * 1.1).toInt().coerceAtMost(maxBitrate)
            }
            rttMs > 300 || lossRate > 0.05 -> {
                currentTargetBitrate = (currentTargetBitrate * 0.7).toInt().coerceAtLeast(minBitrate)
            }
            rttMs > 200 || lossRate > 0.02 -> {
                currentTargetBitrate = (currentTargetBitrate * 0.9).toInt().coerceAtLeast(minBitrate)
            }
        }

        applyBitrate(currentTargetBitrate)
    }

    private fun applyBitrate(targetBps: Int) {
        peerConnection.setBitrate(null, targetBps, null)

        val senders = peerConnection.senders
        for (sender in senders) {
            if (sender.track()?.kind() == "video") {
                val params = sender.parameters
                for (encoding in params.encodings) {
                    encoding.maxBitrateBps = targetBps
                    encoding.minBitrateBps = config.videoMinBitrateBps
                    encoding.maxFramerate = config.videoFps
                    encoding.scaleResolutionDownBy = computeScale(targetBps)
                }
                sender.parameters = params
            }
        }
    }

    private fun computeScale(bitrateBps: Int): Double {
        return when {
            bitrateBps >= 5_000_000 -> 1.0
            bitrateBps >= 3_000_000 -> 1.5
            bitrateBps >= 1_500_000 -> 2.0
            else -> 3.0
        }
    }
}
