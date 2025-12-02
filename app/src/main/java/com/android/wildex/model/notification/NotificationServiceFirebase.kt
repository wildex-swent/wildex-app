package com.android.wildex.model.notification

import com.android.wildex.model.RepositoryProvider
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationServiceFirebase : FirebaseMessagingService() {

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    CoroutineScope(Dispatchers.IO).launch {
      Firebase.auth.currentUser?.apply {
        RepositoryProvider.userTokensRepository.addTokenToUser(uid, token)
      }
    }
  }
}
