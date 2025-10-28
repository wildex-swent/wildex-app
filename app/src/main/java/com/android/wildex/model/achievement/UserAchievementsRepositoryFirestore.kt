package com.android.wildex.model.achievement

import com.android.wildex.model.user.UserAchievements
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Input
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
    require(collection.exists())
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

  override suspend fun updateUserAchievements(userId: Id, inputs: Input) {
    val docRef = db.collection(USER_ACHIEVEMENTS_COLLECTION_PATH).document(userId)
    val doc = docRef.get().await()
    require(doc.exists())

    val ua = doc.toObject(UserAchievements::class.java) ?: throw IllegalArgumentException()

    if (inputs.isEmpty() || inputs.values.all { it.isEmpty() }) return

    val updated =
        Achievements.ALL.filter {
              it.expects.all { key -> inputs.containsKey(key) } && it.condition(inputs)
            }
            .map { it.achievementId }

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
}
