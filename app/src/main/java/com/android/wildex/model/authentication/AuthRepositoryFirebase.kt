package com.android.wildex.model.authentication

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

/**
 * A Firebase implementation of [AuthRepository].
 *
 * Retrieves a Google ID token via Credential Manager and authenticates the user with Firebase.
 * Handles sign-out and credential state clearing.
 *
 * @param auth The [FirebaseAuth] instance for Firebase authentication.
 */
class AuthRepositoryFirebase(
    private val auth: FirebaseAuth = Firebase.auth,
) : AuthRepository {

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    TODO()
  }

  override fun signOut(): Result<Unit> {
    TODO()
  }
}
