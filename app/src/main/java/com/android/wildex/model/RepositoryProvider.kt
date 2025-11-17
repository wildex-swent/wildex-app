package com.android.wildex.model

import com.android.wildex.HttpClientProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.achievement.UserAchievementsRepositoryFirestore
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animal.AnimalRepositoryFirestore
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.animaldetector.AnimalInfoRepositoryHttp
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.authentication.AuthRepositoryFirebase
import com.android.wildex.model.relationship.RelationshipRepository
import com.android.wildex.model.relationship.RelationshipRepositoryFirestore
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.report.ReportRepositoryFirestore
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.CommentRepositoryFirestore
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.LikeRepositoryFirestore
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.social.PostsRepositoryFirestore
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.storage.StorageRepositoryFirebase
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserAnimalsRepositoryFirestore
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserRepositoryFirestore
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserSettingsRepositoryFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlin.getValue

/** Provides a single instance of all the repository in the app. */
object RepositoryProvider {
  val authRepository: AuthRepository by lazy { AuthRepositoryFirebase(Firebase.auth) }
  val postRepository: PostsRepository by lazy { PostsRepositoryFirestore(Firebase.firestore) }
  val userRepository: UserRepository by lazy { UserRepositoryFirestore(Firebase.firestore) }
  val likeRepository: LikeRepository by lazy { LikeRepositoryFirestore(Firebase.firestore) }
  val reportRepository: ReportRepository by lazy { ReportRepositoryFirestore(Firebase.firestore) }

  val commentRepository: CommentRepository by lazy {
    CommentRepositoryFirestore(Firebase.firestore)
  }
  val userAchievementsRepository: UserAchievementsRepository by lazy {
    UserAchievementsRepositoryFirestore(Firebase.firestore)
  }
  val animalInfoRepository: AnimalInfoRepository by lazy {
    AnimalInfoRepositoryHttp(HttpClientProvider.client)
  }
  val storageRepository: StorageRepository by lazy { StorageRepositoryFirebase(Firebase.storage) }
  val animalRepository: AnimalRepository by lazy { AnimalRepositoryFirestore(Firebase.firestore) }
  val userAnimalsRepository: UserAnimalsRepository by lazy {
    UserAnimalsRepositoryFirestore(Firebase.firestore)
  }
  val userSettingsRepository: UserSettingsRepository by lazy {
    UserSettingsRepositoryFirestore(Firebase.firestore)
  }
  val relationshipRepository: RelationshipRepository by lazy {
    RelationshipRepositoryFirestore(Firebase.firestore)
  }
}
