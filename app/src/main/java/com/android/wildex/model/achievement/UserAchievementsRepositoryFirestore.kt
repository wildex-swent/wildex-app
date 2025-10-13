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

  override suspend fun initializeUserAchievements(userId: String) {
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    if (!doc.exists()) {
      val userAchievements = UserAchievements(userId, emptyList(), 0)
      docRef.set(userAchievements).await()
    }
  }

  override suspend fun getAllAchievementsByUser(userId: String): List<Achievement> {
    val collection = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId).get().await()
    if (!collection.exists()) {
      throw IllegalArgumentException("UserAchievements of the given userId was not initialized")
    }
    val userAchievements = collection.toObject(UserAchievements::class.java)
    // Map achievement IDs to Achievements
    // If an ID does not match any Achievement, it is ignored
    // If matches, add to the list of the user's achievements to be returned
    val achievements =
        userAchievements?.achievementsId?.mapNotNull { id ->
          Achievements.ALL.find { it.achievementId == id }
        } ?: emptyList()
    return achievements
  }

  override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> {
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("PostsRepositoryFirestore: User not logged in.")
    return getAllAchievementsByUser(userId)
  }

  override suspend fun updateUserAchievements(userId: String, listIds: List<Id>) {
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    if (!doc.exists()) {
      throw IllegalArgumentException("UserAchievements with given userId not found")
    }

    val userAchievements =
        doc.toObject(UserAchievements::class.java)
            ?: throw IllegalArgumentException("UserAchievements with given userId not found")

    // List to hold the IDs of achievements that the user qualifies for
    val updatedAchievementIds = mutableListOf<Id>()

    for (achievement in Achievements.ALL) {
      try {
        if (achievement.condition(listIds)) {
          updatedAchievementIds.add(achievement.achievementId)
        }
      } catch (e: IllegalArgumentException) {
        // If the condition throws an exception
        // (because the given listIds does not meet the expected format by the condition
        // like for example giving a list of like IDs when the condition expects a list of post IDs)
        // Skip this one and continue
        continue
      }
    }
    // Only update if something changed
    if (updatedAchievementIds.toSet() != userAchievements.achievementsId.toSet()) {
      val updatedUserAchievements =
          userAchievements.copy(
              achievementsId = updatedAchievementIds,
              achievementsCount = updatedAchievementIds.size,
          )
      docRef.set(updatedUserAchievements).await()
    }
  }

  override suspend fun getAchievementsCountOfUser(userId: String): Int {
    val collection = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId).get().await()
    if (!collection.exists()) {
      throw IllegalArgumentException("UserAchievements of the given userId was not initialized")
    }
    val userAchievements = collection.toObject(UserAchievements::class.java)
    return userAchievements?.achievementsCount ?: 0
  }
}
