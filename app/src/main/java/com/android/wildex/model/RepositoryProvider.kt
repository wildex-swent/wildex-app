package com.android.wildex.model

import com.android.wildex.HttpClientProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.achievement.UserAchievementsRepositoryFirestore
import com.android.wildex.model.animaldetector.AnimalDetectRepository
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.authentication.AuthRepositoryFirebase
import com.android.wildex.model.social.CommentsRepository
import com.android.wildex.model.social.CommentsRepositoryFirestore
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.LikeRepositoryFirestore
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.social.PostsRepositoryFirestore
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserRepositoryFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/** Provides a single instance of all the repository in the app. */
object RepositoryProvider {

  val postRepository: PostsRepository by lazy { PostsRepositoryFirestore(Firebase.firestore) }
  val userRepository: UserRepository by lazy { UserRepositoryFirestore(Firebase.firestore) }
  val commentRepository: CommentsRepository by lazy {
    CommentsRepositoryFirestore(Firebase.firestore)
  }
  val userAchievementsRepository: UserAchievementsRepository by lazy {
    UserAchievementsRepositoryFirestore(Firebase.firestore)
  }
  val animalDetectRepository: AnimalDetectRepository by lazy {
    AnimalDetectRepository(HttpClientProvider.client)
  }
  val likeRepository: LikeRepository by lazy { LikeRepositoryFirestore(Firebase.firestore) }
  val authRepository: AuthRepository by lazy { AuthRepositoryFirebase() }
}
