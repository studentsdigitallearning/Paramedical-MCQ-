package com.example.data.supabase

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight client for Supabase Realtime via WebSocket (Phoenix Channels protocol).
 * Listens for live updates on shared data: Leaderboard and Test changes.
 */
class SupabaseRealtimeClient private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val httpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websockets
        .pingInterval(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private val refCounter = AtomicInteger(1)

    private val leaderboardListeners = mutableListOf<() -> Unit>()
    private val testListeners = mutableListOf<() -> Unit>()

    @Volatile
    private var isConnected = false

    fun addLeaderboardListener(listener: () -> Unit) {
        synchronized(leaderboardListeners) {
            leaderboardListeners.add(listener)
        }
    }

    fun removeLeaderboardListener(listener: () -> Unit) {
        synchronized(leaderboardListeners) {
            leaderboardListeners.remove(listener)
        }
    }

    fun addTestListener(listener: () -> Unit) {
        synchronized(testListeners) {
            testListeners.add(listener)
        }
    }

    fun removeTestListener(listener: () -> Unit) {
        synchronized(testListeners) {
            testListeners.remove(listener)
        }
    }

    fun connect() {
        if (isConnected || webSocket != null) return

        val wsUrl = buildWsUrl()
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "Supabase Realtime connected")
                isConnected = true
                startHeartbeat(ws)
                subscribeToChannels(ws)
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Realtime closing: $code / $reason")
                isConnected = false
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Realtime closed: $code / $reason")
                isConnected = false
                cleanup()
                scheduleReconnect()
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "Realtime failure: ${t.message}")
                isConnected = false
                cleanup()
                scheduleReconnect()
            }
        })
    }

    private fun buildWsUrl(): String {
        val base = SupabaseConfig.projectUrl.trimEnd('/')
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        return "$base/realtime/v1/websocket?apikey=${SupabaseConfig.anonKey}&vsn=1.0.0"
    }

    private fun subscribeToChannels(ws: WebSocket) {
        // Subscribe to public:leaderboard
        sendJoin(ws, "realtime:public:leaderboard")
        // Subscribe to public:tests
        sendJoin(ws, "realtime:public:tests")
    }

    private fun sendJoin(ws: WebSocket, topic: String) {
        val ref = refCounter.getAndIncrement().toString()
        val msg = JSONObject().apply {
            put("topic", topic)
            put("event", "phx_join")
            put("payload", JSONObject())
            put("ref", ref)
        }.toString()
        ws.send(msg)
    }

    private fun startHeartbeat(ws: WebSocket) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && isConnected) {
                delay(25_000)
                try {
                    val ref = refCounter.getAndIncrement().toString()
                    val heartbeat = JSONObject().apply {
                        put("topic", "phoenix")
                        put("event", "heartbeat")
                        put("payload", JSONObject())
                        put("ref", ref)
                    }.toString()
                    ws.send(heartbeat)
                } catch (e: Exception) {
                    Log.w(TAG, "Heartbeat failed: ${e.message}")
                    break
                }
            }
        }
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            val topic = json.optString("topic", "")
            val event = json.optString("event", "")

            if (event == "phx_reply" || event == "heartbeat") return

            // If a change event (INSERT, UPDATE, DELETE, or broadcast) happened
            if (topic.contains("leaderboard")) {
                Log.d(TAG, "Realtime event on leaderboard: $event")
                notifyLeaderboardChanged()
            } else if (topic.contains("tests")) {
                Log.d(TAG, "Realtime event on tests: $event")
                notifyTestsChanged()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling realtime message: ${e.message}")
        }
    }

    private fun notifyLeaderboardChanged() {
        val copy = synchronized(leaderboardListeners) { leaderboardListeners.toList() }
        copy.forEach {
            try { it.invoke() } catch (e: Exception) { Log.e(TAG, "Leaderboard listener error: ${e.message}") }
        }
    }

    private fun notifyTestsChanged() {
        val copy = synchronized(testListeners) { testListeners.toList() }
        copy.forEach {
            try { it.invoke() } catch (e: Exception) { Log.e(TAG, "Tests listener error: ${e.message}") }
        }
    }

    private fun cleanup() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        webSocket = null
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(5_000)
            if (!isConnected) {
                Log.d(TAG, "Attempting realtime reconnect...")
                connect()
            }
        }
    }

    fun disconnect() {
        isConnected = false
        cleanup()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    companion object {
        private const val TAG = "SupabaseRealtime"

        @Volatile
        private var instance: SupabaseRealtimeClient? = null

        fun getInstance(): SupabaseRealtimeClient {
            return instance ?: synchronized(this) {
                instance ?: SupabaseRealtimeClient().also { instance = it }
            }
        }
    }
}
