package com.android.wildex.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

/** Handles authentication operations such as signing in with Google and signing out. */
interface AuthRepository {

  /**
   * Signs in the user using a Google account through the Credential Manager API.
   *
   * @return A [Result] containing a [FirebaseUser] on success, or an exception on failure.
   */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  /**
   * Signs out the currently authenticated user and clears the credential state.
   *
   * @return A [Result] indication success of failure.
   */
  fun signOut(): Result<Unit>

  /**
   * Deletes the user's authentication entry.
   *
   * @return A [Result] indication success of failure.
   */
  fun deleteUserAuth(): Result<Unit>
}
