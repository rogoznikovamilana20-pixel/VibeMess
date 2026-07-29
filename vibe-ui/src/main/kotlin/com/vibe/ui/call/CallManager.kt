package com.vibe.ui.call

import android.content.Context
import com.vibe.common.logging.VibeLogger
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.UUID

class CallManager(private val context: Context, private val userId: String) {

    interface Callback {
        fun onCallStateChanged(state: CallState)
        fun onRemoteVideoTrack(track: VideoTrack?)
        fun onIncomingCall(callerId: String, roomId: String)
        fun onE2eeStatusChanged(verified: Boolean, localFp: String, remoteFp: String, safetyNumber: String)
    }

    enum class CallState {
        IDLE, CONNECTING, RINGING, CONNECTED, DISCONNECTED, FAILED
    }

    private var state: CallState = CallState.IDLE
        set(value) {
            field = value
            callback?.onCallStateChanged(value)
        }

    private var callback: Callback? = null
    private var signaling: SupabaseSignaling? = null
    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var localStream: MediaStream? = null
    private var currentCallerId: String = ""
    private var adaptationManager: QualityAdaptationManager? = null

    private val qualityConfig = DeviceCapabilityDetector.detect(context)

    // TURN credentials should come from backend signaling, not be hardcoded.
    // TODO: Replace with server-provided TURN credentials via signaling.
    private val iceServers = mutableListOf(
        PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
        PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
    )

    private val pcObserver = object : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            signaling?.sendIce(candidate)
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}

        override fun onSignalingChange(state: PeerConnection.SignalingState) {}

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED -> {
                    this@CallManager.state = CallState.CONNECTED
                    updateE2eeStatus()
                    startQualityAdaptation()
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    this@CallManager.state = CallState.DISCONNECTED
                    stopQualityAdaptation()
                }
                PeerConnection.IceConnectionState.FAILED -> {
                    this@CallManager.state = CallState.FAILED
                    stopQualityAdaptation()
                }
                else -> {}
            }
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {}

        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}

        override fun onAddStream(stream: MediaStream) {
            if (stream.videoTracks.isNotEmpty()) {
                callback?.onRemoteVideoTrack(stream.videoTracks[0])
            }
        }

        override fun onRemoveStream(stream: MediaStream) {}

        override fun onDataChannel(channel: DataChannel) {}

        override fun onRenegotiationNeeded() {}

        override fun onAddTrack(receiver: org.webrtc.RtpReceiver, streams: Array<out MediaStream>) {
            if (receiver.track() is VideoTrack) {
                callback?.onRemoteVideoTrack(receiver.track() as VideoTrack)
            }
        }
    }

    fun initialize(callback: Callback) {
        this.callback = callback

        SdpQualityEnhancer.setConfig(qualityConfig)

        signaling = SupabaseSignaling(
            projectUrl = SupabaseSignaling.SUPABASE_URL,
            anonKey = SupabaseSignaling.SUPABASE_ANON_KEY,
            userId = userId,
            onIncomingCall = { callerId, roomId ->
                currentCallerId = callerId
                callback.onIncomingCall(callerId, roomId)
            },
            onRemoteSdp = { sdp ->
                setRemoteSdp(sdp)
                updateE2eeStatus()
                if (sdp.type == SessionDescription.Type.OFFER) {
                    peerConnection?.createAnswer(answerObserver, answerConstraints())
                }
            },
            onRemoteIce = { candidate ->
                addIceCandidate(candidate)
            },
            onCallAccepted = {
                state = CallState.CONNECTING
                peerConnection?.createOffer(offerObserver, offerConstraints())
            }
        )

        val fieldTrials = buildString {
            append("WebRTC-Audio-OpusBitrate/Enabled-${qualityConfig.audioBitrateBps}/")
            append("WebRTC-Audio-OpusAvoidNoisePumping/Enabled/")
            if (qualityConfig.audioFec) {
                append("WebRTC-Audio-Fec/Enabled/")
            }
            append("WebRTC-Video-QualityScaler/Enabled/")
            append("WebRTC-Video-Bwe/Enabled/")
            append("WebRTC-Audio-AudioNetworkAdaptor/Enabled/")
        }

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setFieldTrials(fieldTrials)
                .setNativeLibraryLoader(object : org.webrtc.NativeLibraryLoader {
                    override fun load(name: String): Boolean {
                        return true
                    }
                })
                .createInitializationOptions()
        )

        eglBase = EglBase.create()

        val encoderFactory = DefaultVideoEncoderFactory(
            eglBase?.eglBaseContext,
            true,
            qualityConfig.enableH264HighProfile
        )
        val decoderFactory = DefaultVideoDecoderFactory(eglBase?.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        signaling?.connect()
    }

    fun connectSignaling() {
        signaling?.connect()
    }

    fun createLocalStream(useVideo: Boolean): MediaStream {
        localStream = peerConnectionFactory?.createLocalMediaStream("stream_${UUID.randomUUID()}")!!

        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googTypingNoiseDetection", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseReduction", "true"))
        }
        localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_${UUID.randomUUID()}", localAudioSource)
        localAudioTrack?.setEnabled(true)
        localStream?.addTrack(localAudioTrack!!)

        if (useVideo) {
            videoCapturer = createVideoCapturer()
            if (videoCapturer != null) {
                val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase?.eglBaseContext)
                localVideoSource = peerConnectionFactory?.createVideoSource(false)
                videoCapturer?.initialize(surfaceTextureHelper, context, localVideoSource?.capturerObserver)
                videoCapturer?.startCapture(qualityConfig.videoWidth, qualityConfig.videoHeight, qualityConfig.videoFps)

                localVideoTrack = peerConnectionFactory?.createVideoTrack("video_${UUID.randomUUID()}", localVideoSource)
                localVideoTrack?.setEnabled(true)
                localStream?.addTrack(localVideoTrack!!)
            }
        }

        return localStream!!
    }

    fun createPeerConnection(): PeerConnection {
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceCandidatePoolSize = 1
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            keyType = PeerConnection.KeyType.ECDSA
            enableDscp = qualityConfig.enableDscp
            audioJitterBufferMaxPackets = qualityConfig.jitterBufferMaxPackets
            audioJitterBufferFastAccelerate = qualityConfig.jitterBufferFastAccelerate
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(config, pcObserver)

        localStream?.let { stream ->
            peerConnection?.addStream(stream)
        }

        peerConnection?.let { conn ->
            conn.setBitrate(
                qualityConfig.videoMinBitrateBps,
                qualityConfig.videoStartBitrateBps,
                qualityConfig.videoMaxBitrateBps
            )
        }

        return peerConnection!!
    }

    fun callUser(targetUserId: String): String {
        state = CallState.CONNECTING
        createLocalStream(useVideo = true)
        createPeerConnection()
        return signaling?.callUser(targetUserId) ?: ""
    }

    fun acceptCall(roomId: String, callerId: String = currentCallerId) {
        state = CallState.CONNECTING
        currentCallerId = callerId
        createLocalStream(useVideo = true)
        createPeerConnection()
        signaling?.acceptCall(callerId, roomId)
    }

    fun setRemoteSdp(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(sdpObserver, sdp)
    }

    fun addIceCandidate(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleMute(muted: Boolean) {
        localAudioTrack?.setEnabled(!muted)
    }

    fun toggleVideo(enabled: Boolean) {
        videoCapturer?.let {
            if (enabled) {
                it.startCapture(qualityConfig.videoWidth, qualityConfig.videoHeight, qualityConfig.videoFps)
            } else {
                it.stopCapture()
            }
        }
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    fun endCall() {
        stopQualityAdaptation()
        videoCapturer?.let {
            try { it.stopCapture() } catch (e: Exception) { VibeLogger.e("CallManager", "stopCapture failed", e) }
            it.dispose()
        }
        videoCapturer = null

        peerConnection?.close()
        peerConnection = null
        localStream = null
        localVideoTrack = null
        localVideoSource = null
        localAudioTrack = null
        localAudioSource = null

        signaling?.disconnect()
        signaling = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
        eglBase?.release()
        eglBase = null

        state = CallState.DISCONNECTED
    }

    fun getEglBaseContext() = eglBase?.eglBaseContext

    fun getE2eeStatus(): E2eeStatus {
        val sig = signaling
        return if (sig != null) {
            val safetyNumber = E2eeManager.generateSafetyNumber(
                sig.lastLocalFingerprint,
                sig.lastRemoteFingerprint
            )
            E2eeStatus(
                verified = sig.isE2eeVerified,
                localFingerprint = sig.lastLocalFingerprint,
                remoteFingerprint = sig.lastRemoteFingerprint,
                safetyNumber = safetyNumber
            )
        } else {
            E2eeStatus(false, "", "", "")
        }
    }

    fun confirmE2ee() {
        signaling?.isE2eeVerified = true
        updateE2eeStatus()
    }

    fun getQualityLabel(): String = qualityConfig.label

    data class E2eeStatus(
        val verified: Boolean,
        val localFingerprint: String,
        val remoteFingerprint: String,
        val safetyNumber: String
    )

    private fun updateE2eeStatus() {
        val status = getE2eeStatus()
        callback?.onE2eeStatusChanged(
            status.verified,
            status.localFingerprint,
            status.remoteFingerprint,
            status.safetyNumber
        )
    }

    private fun startQualityAdaptation() {
        peerConnection?.let { conn ->
            adaptationManager = QualityAdaptationManager(conn, qualityConfig)
            adaptationManager?.start()
        }
    }

    private fun stopQualityAdaptation() {
        adaptationManager?.stop()
        adaptationManager = null
    }

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(false)
        }

        val deviceNames = enumerator.deviceNames
        for (name in deviceNames) {
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        for (name in deviceNames) {
            return enumerator.createCapturer(name, null)
        }
        return null
    }

    private fun offerConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    private fun answerConstraints(): MediaConstraints {
        return MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
    }

    private val offerObserver = object : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {
            val enhanced = SdpQualityEnhancer.enhance(sdp)
            peerConnection?.setLocalDescription(innerSdpObserver, enhanced)
            signaling?.sendSdp(enhanced)
        }
        override fun onSetSuccess() {}
        override fun onCreateFailure(msg: String) { VibeLogger.e("CallManager", "Offer create failed: $msg") }
        override fun onSetFailure(msg: String) { VibeLogger.e("CallManager", "Offer set failed: $msg") }
    }

    private val answerObserver = object : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {
            val enhanced = SdpQualityEnhancer.enhance(sdp)
            peerConnection?.setLocalDescription(innerSdpObserver, enhanced)
            signaling?.sendSdp(enhanced)
        }
        override fun onSetSuccess() {}
        override fun onCreateFailure(msg: String) { VibeLogger.e("CallManager", "Answer create failed: $msg") }
        override fun onSetFailure(msg: String) { VibeLogger.e("CallManager", "Answer set failed: $msg") }
    }

    private val sdpObserver = object : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(msg: String) {}
        override fun onSetFailure(msg: String) { VibeLogger.e("CallManager", "SDP set failed: $msg") }
    }

    private val innerSdpObserver = object : org.webrtc.SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(msg: String) {}
        override fun onSetFailure(msg: String) { VibeLogger.e("CallManager", "Inner SDP set failed: $msg") }
    }
}
