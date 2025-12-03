package com.android.wildex.model.achievement

import com.android.wildex.model.user.UserAchievements
import com.android.wildex.model.utils.Id
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val USER_ACHIEVEMENTS_COLLECTION_PATH = "userAchievements"

class UserAchievementsRepositoryFirestore(private val db: FirebaseFirestore) :
    UserAchievementsRepository {

  override suspend fun initializeUserAchievements(userId: Id) {
    require(userId.isNotBlank()) { "User ID must not be blank" }
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    if (!doc.exists()) {
      val userAchievements = UserAchievements(userId = userId)
      docRef.set(userAchievements).await()
    }
  }

  override suspend fun getAllAchievementsByUser(userId: Id): List<Achievement> {
    val collection = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId).get().await()
    val userAchievements = collection.toObject(UserAchievements::class.java)
    // Map achievement IDs to Achievements
    // If an ID does not match any Achievement, it is ignored
    // If matches, add to the list of the user's achievements to be returned
    val achievements =
        userAchievements?.achievementsId?.mapNotNull { id -> Achievements.achievementById[id] }
            ?: emptyList()
    return achievements
  }

  override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> {
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("UserAchievementsRepositoryFirestore: User not logged in.")
    return getAllAchievementsByUser(userId)
  }

  override suspend fun getAllAchievements(): List<Achievement> {
    return Achievements.ALL
  }

  override suspend fun updateUserAchievements(userId: Id) {
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    require(doc.exists())

    val ua = doc.toObject(UserAchievements::class.java) ?: throw IllegalArgumentException()

    val updated =
        Achievements.ALL.mapNotNull {
          if (it.progress(userId).all { triple -> triple.second >= triple.third }) it.achievementId
          else null
        }

    if (updated.toSet() != ua.achievementsId.toSet()) {
      docRef.set(ua.copy(achievementsId = updated, achievementsCount = updated.size)).await()
    }
  }

  override suspend fun getAchievementsCountOfUser(userId: Id): Int {
    val collection = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId).get().await()
    require(collection.exists())
    val userAchievements = collection.toObject(UserAchievements::class.java)
    return userAchievements?.achievementsCount ?: 0
  }

  override suspend fun deleteUserAchievements(userId: Id) {
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    require(doc.exists()) { "A userAchievements with userId '$userId' does not exist." }
    docRef.delete().await()
  }
}
