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
    require(userId.isNotBlank()) { "User ID must not be blank" }
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    if (!doc.exists()) {
      val userAchievements = UserAchievements(userId = userId)
      docRef.set(userAchievements).await()
    }
  }

  override suspend fun getAllAchievementsByUser(userId: String): List<Achievement> {
    val collection = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId).get().await()
    require(collection.exists())
    val userAchievements = collection.toObject(UserAchievements::class.java)
    // Map achievement IDs to Achievements
    // If an ID does not match any Achievement, it is ignored
    // If matches, add to the list of the user's achievements to be returned
    val achievements =
        userAchievements?.achievementsId?.mapNotNull { id -> Achievements.achievement_by_id[id] }
            ?: emptyList()
    return achievements
  }

  override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> {
    val userId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("UserAchievementsRepositoryFirestore: User not logged in.")
    return getAllAchievementsByUser(userId)
  }

  override suspend fun updateUserAchievements(userId: String, inputs: Map<InputKey, List<Id>>) {
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    require(doc.exists())

    val ua =
        doc.toObject(UserAchievements::class.java)
            ?: throw IllegalArgumentException("Malformed UserAchievements for userId=$userId")

    if (inputs.isEmpty() || inputs.values.all { it.isEmpty() }) return

    val updated = mutableListOf<Id>()

    for (a in Achievements.ALL) {
      val expects = a.expects

      val ok: Boolean =
          try {
            when {
              // Require all expected keys present, then evaluate multiCondition
              expects.size > 1 && a.multiCondition != null -> {
                val hasAll = expects.all { key -> inputs.containsKey(key) }
                if (!hasAll) false else a.multiCondition.invoke(inputs)
              }

              // Evaluate condition on the single provided list
              expects.size == 1 && a.condition != null -> {
                val key = expects.first()
                val list = inputs[key].orEmpty()
                // If the required key isn't in inputs at all, skip
                if (!inputs.containsKey(key)) false else a.condition.invoke(list)
              }
              else -> false
            }
          } catch (_: Exception) {
            // If error is thrown, skip this achievement
            false
          }

      if (ok) updated += a.achievementId
    }

    // No changes does not update
    if (updated.toSet() != ua.achievementsId.toSet()) {
      docRef.set(ua.copy(achievementsId = updated, achievementsCount = updated.size)).await()
    }
  }

  override suspend fun getAchievementsCountOfUser(userId: String): Int {
    val collection = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId).get().await()
    require(collection.exists())
    val userAchievements = collection.toObject(UserAchievements::class.java)
    return userAchievements?.achievementsCount ?: 0
  }
}
