package com.android.wildex.model.user

import com.android.wildex.model.utils.Id

interface UserTokensRepository {

  /** Retrieves the current token for a user. */
  suspend fun getCurrentToken(): String

  /** Initializes UserTokens for a new User with empty list and zero count. */
  suspend fun initializeUserTokens(userId: Id)

  /** Retrieves UserTokens associated with a specific User. */
  suspend fun getAllTokensOfUser(userId: Id): List<String>

  /** Add an token to the UserTokens of a specific User. */
  suspend fun addTokenToUser(userId: Id, token: String)

  /** Delete an token to the UserTokens of a specific User. */
  suspend fun deleteTokenOfUser(userId: Id, token: String)

  /** Delete the UserTokens linked to the given user */
  suspend fun deleteUserTokens(userId: Id)
}
