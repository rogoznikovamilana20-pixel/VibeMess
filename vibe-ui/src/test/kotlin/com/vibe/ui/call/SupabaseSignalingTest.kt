package com.vibe.ui.call

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SupabaseSignalingTest {

    private var lastCallerId: String? = null
    private var lastRoomId: String? = null
    private var callAccepted = false

    @Test
    fun `handlePhoenixMessage call event triggers onIncomingCall`() {
        val signaling = createSignaling()
        val payload = JSONObject().apply {
            put("caller", "user_bob")
            put("room", "room_abc123")
        }

        signaling.handlePhoenixMessage("user:test_user", "call", payload)

        assertEquals("user_bob", lastCallerId)
        assertEquals("room_abc123", lastRoomId)
    }

    @Test
    fun `handlePhoenixMessage call without caller does nothing`() {
        val signaling = createSignaling()
        val payload = JSONObject()
        signaling.handlePhoenixMessage("user:test_user", "call", payload)

        assertNull(lastCallerId)
    }

    @Test
    fun `handlePhoenixMessage call_accepted triggers onCallAccepted`() {
        val signaling = createSignaling()
        val payload = JSONObject().apply { put("room", "room_abc") }
        signaling.handlePhoenixMessage("user:test_user", "call_accepted", payload)

        assertTrue(callAccepted)
    }

    @Test
    fun `handlePhoenixMessage unknown event does nothing`() {
        val signaling = createSignaling()
        signaling.handlePhoenixMessage("room:test_room", "unknown_event", JSONObject())

        assertNull(lastCallerId)
        assertFalse(callAccepted)
    }

    @Test
    fun `handlePhoenixMessage phx_reply does not crash`() {
        createSignaling().handlePhoenixMessage("room:test_room", "phx_reply", JSONObject())
    }

    @Test
    fun `handlePhoenixMessage user_offline does not crash`() {
        createSignaling().handlePhoenixMessage("room:test_room", "user_offline",
            JSONObject().apply { put("user", "test") })
    }

    @Test
    fun `handleMessage invalid JSON does not crash`() {
        createSignaling().handleMessage("not json {{{")
    }

    @Test
    fun `handleMessage empty string does not crash`() {
        createSignaling().handleMessage("")
    }

    @Test
    fun `handleMessage partial JSON does not crash`() {
        createSignaling().handleMessage("""["1","2"]""")
    }

    private fun createSignaling(): SupabaseSignaling {
        return SupabaseSignaling(
            projectUrl = "https://test.supabase.co",
            anonKey = "test_key",
            userId = "test_user",
            onIncomingCall = { callerId, roomId ->
                lastCallerId = callerId
                lastRoomId = roomId
            },
            onRemoteSdp = {},
            onRemoteIce = {},
            onCallAccepted = { callAccepted = true }
        )
    }
}
