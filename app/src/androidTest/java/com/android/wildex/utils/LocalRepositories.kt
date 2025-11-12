package com.android.wildex.utils

import android.content.Context
import android.net.Uri
import com.android.wildex.model.achievement.Achievement
import com.android.wildex.model.achievement.UserAchievementsRepository
import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animal.AnimalRepository
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.AnimalInfoRepository
import com.android.wildex.model.animaldetector.BoundingBox
import com.android.wildex.model.animaldetector.Taxonomy
import com.android.wildex.model.report.Report
import com.android.wildex.model.report.ReportRepository
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentRepository
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.LikeRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.storage.StorageRepository
import com.android.wildex.model.user.AppearanceMode
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAnimalsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.user.UserSettings
import com.android.wildex.model.user.UserSettingsRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.model.utils.Input
import com.android.wildex.model.utils.URL
import kotlin.collections.mutableMapOf

interface ClearableRepository {
  fun clear()
}

object LocalRepositories {

  open class PostsRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
      PostsRepository, ClearableRepository {
    val listOfPosts = mutableListOf<Post>()

    init {
      clear()
    }

    override fun getNewPostId(): String = "newPostId"

    override suspend fun getAllPosts(): List<Post> = listOfPosts

    override suspend fun getAllPostsByAuthor(): List<Post> =
        listOfPosts.filter { it.authorId == currentUserId }

    override suspend fun getAllPostsByGivenAuthor(authorId: Id): List<Post> =
        listOfPosts.filter { it.authorId == authorId }

    override suspend fun getPost(postId: Id): Post = listOfPosts.find { it.postId == postId }!!

    override suspend fun addPost(post: Post) {
      listOfPosts.add(post)
    }

    override suspend fun editPost(postId: Id, newValue: Post) {
      listOfPosts.removeIf { it.postId == postId }
      listOfPosts.add(newValue)
    }

    override suspend fun deletePost(postId: Id) {
      listOfPosts.removeIf { it.postId == postId }
    }

    override suspend fun deletePostsByUser(userId: Id) {
      listOfPosts.removeIf { it.authorId == userId }
    }

    override fun clear() {
      listOfPosts.clear()
    }
  }

  open class LikeRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
      LikeRepository, ClearableRepository {
    val listOfLikes = mutableListOf<Like>()

    init {
      clear()
    }

    override fun getNewLikeId(): String = "newLikeId"

    override suspend fun getAllLikesByCurrentUser(): List<Like> =
        listOfLikes.filter { it.userId == currentUserId }

    override suspend fun getLikesForPost(postId: String): List<Like> =
        listOfLikes.filter { it.postId == postId }

    override suspend fun getLikeForPost(postId: String): Like? =
        listOfLikes.find { it.postId == postId && it.userId == currentUserId }

    override suspend fun addLike(like: Like) {
      listOfLikes.add(like)
    }

    override suspend fun deleteLike(likeId: String) {
      listOfLikes.removeIf { it.likeId == likeId }
    }

    override suspend fun getAllLikesByUser(userId: Id): List<Like> {
      return listOfLikes.filter { it.userId == userId }
    }

    override suspend fun deleteLikesByUser(userId: Id) {
      listOfLikes.removeIf { it.userId == userId }
    }

    override fun clear() {
      listOfLikes.clear()
    }
  }

  open class UserSettingsRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
      UserSettingsRepository, ClearableRepository {

    val mapUserToSettings = mutableMapOf<Id, UserSettings>()

    init {
      clear()
    }

    override suspend fun initializeUserSettings(userId: String) {
      mapUserToSettings.put(userId, UserSettings())
    }

    override suspend fun getEnableNotification(userId: String): Boolean {
      return mapUserToSettings[userId]?.enableNotifications
          ?: throw Exception("No User with id $userId found")
    }

    override suspend fun setEnableNotification(userId: String, enable: Boolean) {
      val userSettings = mapUserToSettings[userId]
      mapUserToSettings.put(
          userId,
          userSettings?.copy(enableNotifications = enable)
              ?: throw Exception("No User with id $userId found"))
    }

    override suspend fun getAppearanceMode(userId: String): AppearanceMode {
      return mapUserToSettings[userId]?.appearanceMode
          ?: throw Exception("No User with id $userId found")
    }

    override suspend fun setAppearanceMode(userId: String, mode: AppearanceMode) {
      val userSettings = mapUserToSettings[userId]
      mapUserToSettings.put(
          userId,
          userSettings?.copy(appearanceMode = mode)
              ?: throw Exception("No User with id $userId found"))
    }

    override suspend fun deleteUserSettings(userId: Id) {
      mapUserToSettings.remove(userId)
    }

    override fun clear() {
      mapUserToSettings.clear()
    }
  }

  open class UserRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
      UserRepository, ClearableRepository {
    val listOfUsers = mutableListOf<User>()

    init {
      clear()
    }

    override suspend fun getUser(userId: Id): User = listOfUsers.find { it.userId == userId }!!

    override suspend fun getSimpleUser(userId: Id): SimpleUser {
      val user = listOfUsers.find { it.userId == userId }!!
      return SimpleUser(
          userId = user.userId,
          username = user.username,
          profilePictureURL = user.profilePictureURL,
      )
    }

    override suspend fun addUser(user: User) {
      listOfUsers.add(user)
    }

    override suspend fun editUser(userId: Id, newUser: User) {
      listOfUsers.removeIf { it.userId == userId }
      listOfUsers.add(newUser)
    }

    override suspend fun deleteUser(userId: Id) {
      listOfUsers.removeIf { it.userId == userId }
    }

    override fun clear() {
      listOfUsers.clear()
    }
  }

  open class CommentRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
      CommentRepository, ClearableRepository {
    val listOfComments = mutableListOf<Comment>()

    init {
      clear()
    }

    override fun getNewCommentId(): String = "newCommentId"

    override suspend fun getAllCommentsByPost(postId: String): List<Comment> =
        listOfComments.filter { it.postId == postId }

    override suspend fun addComment(comment: Comment) {
      listOfComments.add(comment)
    }

    override suspend fun editComment(commentId: String, newValue: Comment) {
      listOfComments.removeIf { it.commentId == commentId }
      listOfComments.add(newValue)
    }

    override suspend fun deleteAllCommentsOfPost(postId: Id) {
      listOfComments.removeIf { it.commentId == postId && it.tag == CommentTag.POST_COMMENT }
    }

    override suspend fun deleteAllCommentsOfReport(reportId: Id) {
      listOfComments.removeIf { it.commentId == reportId && it.tag == CommentTag.REPORT_COMMENT }
    }

    override suspend fun deleteComment(commentId: String) {
      listOfComments.removeIf { it.commentId == commentId }
    }

    override suspend fun getCommentsByUser(userId: String): List<Comment> =
        listOfComments.filter { it.authorId == userId }

    override suspend fun deleteCommentsByUser(userId: Id) {
      listOfComments.removeIf { it.authorId == userId }
    }

    override fun clear() {
      listOfComments.clear()
    }
  }

  open class AnimalRepositoryImpl() : AnimalRepository, ClearableRepository {
    val listOfAnimals = mutableListOf<Animal>()

    init {
      clear()
    }

    override suspend fun getAnimal(animalId: Id): Animal =
        listOfAnimals.find { it.animalId == animalId }!!

    override suspend fun getAllAnimals(): List<Animal> = listOfAnimals

    override suspend fun addAnimal(animal: Animal) {
      listOfAnimals.add(animal)
    }

    override fun clear() {
      listOfAnimals.clear()
    }
  }

  open class UserAnimalsRepositoryImpl(private val animalRepository: AnimalRepository) :
      UserAnimalsRepository, ClearableRepository {
    val mapUserToAnimals = mutableMapOf<Id, MutableList<Animal>>()

    init {
      clear()
    }

    override suspend fun initializeUserAnimals(userId: Id) {
      mapUserToAnimals.put(userId, mutableListOf())
    }

    override suspend fun getAllAnimalsByUser(userId: Id): List<Animal> {
      return mapUserToAnimals[userId]?.toList() ?: throw Exception("User not found")
    }

    override suspend fun getAnimalsCountOfUser(userId: Id): Int {
      return getAllAnimalsByUser(userId).size
    }

    override suspend fun addAnimalToUserAnimals(userId: Id, animalId: Id) {
      val oldList = mapUserToAnimals.getValue(userId)
      oldList.add(animalRepository.getAnimal(animalId))
      mapUserToAnimals.put(userId, oldList)
    }

    override suspend fun deleteAnimalToUserAnimals(userId: Id, animalId: Id) {
      val oldList = mapUserToAnimals.getValue(userId)
      oldList.removeIf { it.animalId == animalId }
      mapUserToAnimals.put(userId, oldList)
    }

    override suspend fun deleteUserAnimals(userId: Id) {
      mapUserToAnimals.remove(userId)
    }

    override fun clear() {
      mapUserToAnimals.forEach { p0, p1 -> mapUserToAnimals.put(p0, mutableListOf()) }
    }
  }

  open class UserAchievementsRepositoryImpl() : UserAchievementsRepository, ClearableRepository {
    val mapUserToAchievements = mutableMapOf<Id, List<Achievement>>()

    override suspend fun initializeUserAchievements(userId: Id) {
      mapUserToAchievements.put(userId, mutableListOf())
    }

    override suspend fun getAllAchievementsByUser(userId: Id): List<Achievement> {
      return mapUserToAchievements[userId] ?: throw Exception("User not found")
    }

    override suspend fun getAllAchievementsByCurrentUser(): List<Achievement> {
      // Not needed for tests
      return emptyList()
    }

    override suspend fun getAllAchievements(): List<Achievement> {
      return mapUserToAchievements.values.flatten().distinct()
    }

    override suspend fun updateUserAchievements(userId: String, inputs: Input) {
      // Not needed for tests
    }

    override suspend fun getAchievementsCountOfUser(userId: Id): Int {
      return getAllAchievementsByUser(userId).size
    }

    override suspend fun deleteUserAchievements(userId: Id) {
      mapUserToAchievements.remove(userId)
    }

    override fun clear() {
      mapUserToAchievements.clear()
    }
  }

  open class ReportRepositoryImpl(private val currentUserId: Id = "currentUserId-1") :
      ReportRepository, ClearableRepository {

    val listOfReports = mutableListOf<Report>()

    override fun getNewReportId(): String = "newReportId"

    override suspend fun getAllReports(): List<Report> = listOfReports

    override suspend fun getAllReportsByAuthor(authorId: Id): List<Report> =
        listOfReports.filter { it.authorId == authorId }

    override suspend fun getAllReportsByAssignee(assigneeId: Id?): List<Report> =
        listOfReports.filter { it.assigneeId == assigneeId }

    override suspend fun getReport(reportId: Id): Report =
        listOfReports.find { it.reportId == reportId }!!

    override suspend fun addReport(report: Report) {
      listOfReports.add(report)
    }

    override suspend fun editReport(reportId: Id, newValue: Report) {
      listOfReports.removeIf { it.reportId == reportId }
      listOfReports.add(newValue)
    }

    override suspend fun deleteReport(reportId: Id) {
      listOfReports.removeIf { it.reportId == reportId }
    }

    override suspend fun deleteReportsByUser(userId: Id) {
      listOfReports.removeIf { it.authorId == userId }
    }

    override fun clear() {
      listOfReports.clear()
    }
  }

  open class StorageRepositoryImpl() : StorageRepository, ClearableRepository {

    val storage = mutableMapOf<Id, Uri>()

    override suspend fun uploadUserProfilePicture(userId: Id, imageUri: Uri): URL? {
      storage[userId] = imageUri
      return "imageUrl:$userId"
    }

    override suspend fun uploadPostImage(postId: Id, imageUri: Uri): URL? {
      storage[postId] = imageUri
      return "imageUrl:$postId"
    }

    override suspend fun uploadAnimalPicture(animalId: Id, imageUri: Uri): URL? {
      storage[animalId] = imageUri
      return "imageUrl:$animalId"
    }

    override suspend fun deleteUserProfilePicture(userId: Id) {
      storage.remove(userId)
    }

    override suspend fun deletePostImage(postId: Id) {
      storage.remove(postId)
    }

    override suspend fun deleteAnimalPicture(animalId: Id) {
      storage.remove(animalId)
    }

    override fun clear() {
      storage.clear()
    }
  }

  open class AnimalInfoRepositoryImpl() : AnimalInfoRepository {
    override suspend fun detectAnimal(
        context: Context,
        imageUri: Uri,
    ): List<AnimalDetectResponse> {
      return listOf(
          AnimalDetectResponse(
              "default animal",
              0.9f,
              BoundingBox(0f, 0f, 0f, 0f),
              Taxonomy(
                  "animalId",
                  "animalClass",
                  "animalOrder",
                  "animalFamily",
                  "animalGenus",
                  "animalSpecies",
              ),
          ))
    }

    override suspend fun getAnimalDescription(animalName: String): String? {
      return "This is a default animal"
    }
  }

  val postsRepository: PostsRepository = PostsRepositoryImpl()
  val likeRepository: LikeRepository = LikeRepositoryImpl()
  val userRepository: UserRepository = UserRepositoryImpl()
  val commentRepository: CommentRepository = CommentRepositoryImpl()
  val animalRepository: AnimalRepository = AnimalRepositoryImpl()
  val userSettingsRepository: UserSettingsRepository = UserSettingsRepositoryImpl()
  val userAnimalsRepository: UserAnimalsRepository =
      UserAnimalsRepositoryImpl(animalRepository = animalRepository)
  val userAchievementsRepository: UserAchievementsRepository = UserAchievementsRepositoryImpl()
  val reportRepository: ReportRepository = ReportRepositoryImpl()
  val storageRepository: StorageRepository = StorageRepositoryImpl()
  val animalInfoRepository: AnimalInfoRepository = AnimalInfoRepositoryImpl()

  fun clearAll() {
    (postsRepository as ClearableRepository).clear()
    (likeRepository as ClearableRepository).clear()
    (userRepository as ClearableRepository).clear()
    (commentRepository as ClearableRepository).clear()
    (animalRepository as ClearableRepository).clear()
    (userSettingsRepository as ClearableRepository).clear()
    (userAnimalsRepository as ClearableRepository).clear()
    (userAchievementsRepository as ClearableRepository).clear()
    (reportRepository as ClearableRepository).clear()
    (storageRepository as ClearableRepository).clear()
  }

  fun clearUserAnimalsAndAnimals() {
    (userAnimalsRepository as ClearableRepository).clear()
    (animalRepository as ClearableRepository).clear()
  }
}
