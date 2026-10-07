package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Real-time network connectivity monitor for MCQ Test Series.
 * Detects online/offline transitions, checks valid internet capabilities,
 * and provides automatic recovery callbacks.
 */
class NetworkMonitor private constructor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var onNetworkRestoredCallback: (() -> Unit)? = null

    private val monitorScope = CoroutineScope(Dispatchers.Default)

    init {
        try {
            _isOnline.value = isCurrentlyConnected()
            registerNetworkCallback()
            startContinuousPolling()
        } catch (e: Exception) {
            _isOnline.value = true
        }
    }

    private fun startContinuousPolling() {
        monitorScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1500)
                try {
                    val current = isCurrentlyConnected()
                    val wasOffline = !_isOnline.value
                    if (_isOnline.value != current) {
                        _isOnline.value = current
                        if (current && wasOffline) {
                            withContext(Dispatchers.Main) {
                                onNetworkRestoredCallback?.invoke()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore exceptions during polling
                }
            }
        }
    }

    fun setOnNetworkRestoredListener(callback: () -> Unit) {
        this.onNetworkRestoredCallback = callback
    }

    /**
     * Checks if device is currently connected to an active network with internet capability.
     */
    fun isCurrentlyConnected(): Boolean {
        val cm = connectivityManager ?: return false
        return try {
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false

            val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val hasValidTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

            hasInternet && hasValidTransport
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Error checking connection: ${e.message}")
            false
        }
    }

    /**
     * Performs an active socket ping check to verify true end-to-end reachability.
     */
    suspend fun verifyInternetAccess(): Boolean = withContext(Dispatchers.IO) {
        if (!isCurrentlyConnected()) return@withContext false
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                true
            }
        } catch (e: Exception) {
            isCurrentlyConnected()
        }
    }

    /**
     * Re-evaluates connection status immediately (e.g. for manual "Try Again" button).
     */
    fun refreshStatus(): Boolean {
        val connected = isCurrentlyConnected()
        val wasOffline = !_isOnline.value
        _isOnline.value = connected
        if (connected && wasOffline) {
            onNetworkRestoredCallback?.invoke()
        }
        return connected
    }

    private fun registerNetworkCallback() {
        if (connectivityManager == null) return

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("NetworkMonitor", "Network is available")
                val wasOffline = !_isOnline.value
                val connected = isCurrentlyConnected()
                _isOnline.value = connected
                if (connected && wasOffline) {
                    onNetworkRestoredCallback?.invoke()
                }
            }

            override fun onLost(network: Network) {
                Log.d("NetworkMonitor", "Network lost")
                _isOnline.value = isCurrentlyConnected()
            }

            override fun onUnavailable() {
                Log.d("NetworkMonitor", "Network unavailable")
                _isOnline.value = false
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                         networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                         networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                         networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
                val wasOffline = !_isOnline.value
                _isOnline.value = hasInternet
                if (hasInternet && wasOffline) {
                    onNetworkRestoredCallback?.invoke()
                }
            }
        }

        try {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            try {
                connectivityManager.registerNetworkCallback(networkRequest, callback)
            } catch (ex: Exception) {
                Log.w("NetworkMonitor", "Could not register network callback: ${ex.message}")
            }
        }
    }

    companion object {
        @Volatile
        private var instance: NetworkMonitor? = null

        fun getInstance(context: Context): NetworkMonitor {
            return instance ?: synchronized(this) {
                instance ?: NetworkMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
}
