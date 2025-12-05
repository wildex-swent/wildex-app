package com.android.wildex.utils.offline

import com.android.wildex.model.ConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeConnectivityObserver(initial: Boolean = false) : ConnectivityObserver {
  private val _isOnline = MutableStateFlow(initial)
  override val isOnline: StateFlow<Boolean> = _isOnline

  fun setOnline(value: Boolean) {
    _isOnline.value = value
  }
}
