package com.android.wildex.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Exposes the current network connectivity state. */
interface ConnectivityObserver {
  val isOnline: StateFlow<Boolean>
}

/** Default implementation using ConnectivityManager callbacks. */
class DefaultConnectivityObserver(
    context: Context,
) : ConnectivityObserver {

  private val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  private val _isOnline = MutableStateFlow(isCurrentlyOnline())
  override val isOnline: StateFlow<Boolean> = _isOnline

  private val networkCallback =
      object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          _isOnline.value = true
        }

        override fun onLost(network: Network) {
          _isOnline.value = isCurrentlyOnline()
        }
      }

  init {
    val request =
        NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
    connectivityManager.registerNetworkCallback(request, networkCallback)
  }

  private fun isCurrentlyOnline(): Boolean {
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}

val LocalConnectivityObserver =
    staticCompositionLocalOf<ConnectivityObserver> { error("ConnectivityObserver not provided") }
