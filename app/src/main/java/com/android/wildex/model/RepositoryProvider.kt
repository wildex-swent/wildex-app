package com.android.wildex.model

import android.content.Context
import com.android.wildex.HttpClientProvider
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.achievement.UserAchievementsRepositoryFirestore
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animal.AnimalRepositoryFirestore
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.animaldetector.AnimalInfoRepositoryHttp
import com.android.wildex.model.authentication.AuthRepository
import com.android.wildex.model.authentication.AuthRepositoryFirebase
import com.android.wildex.model.cache.posts.PostsCache
import com.android.wildex.model.cache.posts.postDataStore
import com.android.wildex.model.cache.report.ReportCache
import com.android.wildex.model.cache.report.reportDataStore
import com.android.wildex.model.cache.user.UserCache
import com.android.wildex.model.cache.user.userDataStore
import com.android.wildex.model.cache.usersettings.UserSettingsCache
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.friendRequest.FriendRequestRepositoryFirestore
import com.android.wildex.model.location.GeocodingRepository
import com.android.wildex.model.location.MapboxGeocodingRepository
import com.android.wildex.model.notification.NotificationRepository
import com.android.wildex.model.notification.NotificationRepositoryFirestore
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
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserFriendsRepositoryFirestore
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserRepositoryFirestore
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.user.UserSettingsRepositoryFirestore
import com.android.wildex.model.user.UserTokensRepository
import com.android.wildex.model.user.UserTokensRepositoryFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/** Provides a single instance of all the Firestore repositories in the app. */
object RepositoryProvider {
  private lateinit var appContext: Context
  private lateinit var connectivityObserver: ConnectivityObserver

  fun init(context: Context) {
    appContext = context.applicationContext
    connectivityObserver = DefaultConnectivityObserver(appContext)
  }

  val authRepository: AuthRepository by lazy { AuthRepositoryFirebase(Firebase.auth) }
  val postRepository: PostsRepository by lazy {
    val cache = PostsCache(appContext.postDataStore, connectivityObserver)
    PostsRepositoryFirestore(Firebase.firestore, cache)
  }
  val userRepository: UserRepository by lazy {
    val cache = UserCache(appContext.userDataStore, connectivityObserver)
    UserRepositoryFirestore(Firebase.firestore, cache)
  }
  val likeRepository: LikeRepository by lazy { LikeRepositoryFirestore(Firebase.firestore) }
  val reportRepository: ReportRepository by lazy {
    val cache = ReportCache(appContext.reportDataStore, connectivityObserver)
    ReportRepositoryFirestore(Firebase.firestore, cache)
  }

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
    val cache = UserSettingsCache(appContext)
    UserSettingsRepositoryFirestore(Firebase.firestore, cache)
  }
  val friendRequestRepository: FriendRequestRepository by lazy {
    FriendRequestRepositoryFirestore(Firebase.firestore)
  }
  val userFriendsRepository: UserFriendsRepository by lazy {
    UserFriendsRepositoryFirestore(Firebase.firestore)
  }
  val notificationRepository: NotificationRepository by lazy {
    NotificationRepositoryFirestore(Firebase.firestore)
  }
  val userTokensRepository: UserTokensRepository by lazy {
    UserTokensRepositoryFirestore(Firebase.firestore)
  }
  val geocodingRepository: GeocodingRepository by lazy {
    MapboxGeocodingRepository(HttpClientProvider.client)
  }
}
