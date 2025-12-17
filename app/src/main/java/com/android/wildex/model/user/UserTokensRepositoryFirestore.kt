package com.android.wildex.model.user

import com.android.wildex.model.utils.Id
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.messaging
import kotlinx.coroutines.tasks.await

/** Firestore implementation of [UserTokensRepository]. */
class UserTokensRepositoryFirestore(db: FirebaseFirestore) : UserTokensRepository {
  companion object {
    private const val USER_TOKENS_COLLECTION_PATH = "userTokens"
  }

  private val collection = db.collection(USER_TOKENS_COLLECTION_PATH)

  override suspend fun getCurrentToken(): String = Firebase.messaging.token.await()

  override suspend fun initializeUserTokens(userId: Id) {
    val docRef = collection.document(userId)
    val snapshot = docRef.get().await()
    require(!snapshot.exists()) { "A userTokens with userId '${userId}' already exists." }
    docRef.set(UserTokens(userId = userId, tokens = emptyList())).await()
  }

  override suspend fun getAllTokensOfUser(userId: Id): List<String> {
    val docRef = collection.document(userId)
    val doc = docRef.get().await().toObject(UserTokens::class.java)
    require(doc != null) { "A userTokens with userId '${userId}' does not exist." }
    return doc.tokens
  }

  override suspend fun addTokenToUser(userId: Id, token: String) {
    val docRef = collection.document(userId)
    val doc = docRef.get().await().toObject(UserTokens::class.java)
    require(doc != null) { "A userTokens with userId '${userId}' does not exist." }
    val newTokens = doc.tokens.toMutableSet().apply { add(token) }.toList()
    docRef.set(UserTokens(userId = userId, tokens = newTokens)).await()
  }

  override suspend fun deleteTokenOfUser(userId: Id, token: String) {
    val docRef = collection.document(userId)
    val doc = docRef.get().await().toObject(UserTokens::class.java)
    require(doc != null) { "A userTokens with userId '${userId}' does not exist." }
    val newTokens = doc.tokens.toMutableList().apply { remove(token) }
    docRef.set(UserTokens(userId = userId, tokens = newTokens)).await()
  }

  override suspend fun deleteUserTokens(userId: Id) {
    val docRef = collection.document(userId)
    val doc = docRef.get().await().toObject(UserTokens::class.java)
    require(doc != null) { "A userTokens with userId '${userId}' does not exist." }
    docRef.delete().await()
  }
}
