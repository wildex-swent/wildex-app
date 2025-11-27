package com.android.wildex.dataClasses

import com.android.wildex.model.animal.Animal
import com.android.wildex.model.animaldetector.AnimalDetectResponse
import com.android.wildex.model.animaldetector.Taxonomy
import com.android.wildex.model.friendRequest.FriendRequest
import com.android.wildex.model.notification.Notification
import com.android.wildex.model.report.Report
import com.android.wildex.model.social.Comment
import com.android.wildex.model.social.CommentTag
import com.android.wildex.model.social.Like
import com.android.wildex.model.social.Post
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserAchievements
import com.android.wildex.model.user.UserAnimals
import com.android.wildex.model.user.UserType
import com.android.wildex.model.utils.Location
import com.google.firebase.Timestamp
import junit.framework.TestCase
import org.junit.Test

/**
 * Test suite for data classes in the WildEx application.
 *
 * This class contains unit tests to verify the correct instantiation and property assignments of
 * various data classes used in the application, including Achievement, Animal,
 * AnimalDetectResponse, FriendRequest, Report, Comment, Like, Post, SimpleUser, User,
 * UserAchievements, UserAnimals, and Location. And to increase code coverage while waiting to
 * implement them in the codebase.
 */
class DataClassesTest {

  @Test
  fun testAnimal() {
    val animal =
        Animal(
            animalId = "animal1",
            pictureURL = "https://example.com/animal_pic",
            name = "Lion",
            species = "Panthera leo",
            description = "The lion is a species in the family Felidae.",
        )

    TestCase.assertEquals("animal1", animal.animalId)
    TestCase.assertEquals("https://example.com/animal_pic", animal.pictureURL)
    TestCase.assertEquals("Lion", animal.name)
    TestCase.assertEquals("Panthera leo", animal.species)
    TestCase.assertEquals("The lion is a species in the family Felidae.", animal.description)
  }

  @Test
  fun testAnimalDetectResponse() {
    val taxonomy =
        Taxonomy(
            id = "1",
            animalClass = "Mammalia",
            order = "Carnivora",
            family = "Felidae",
            genus = "Panthera",
            species = "P. leo",
        )
    val response =
        AnimalDetectResponse(
            animalType = "Lion",
            confidence = 0.98f,
            taxonomy = taxonomy,
        )

    TestCase.assertEquals("Lion", response.animalType)
    TestCase.assertEquals(0.98f, response.confidence)
    TestCase.assertEquals(taxonomy, response.taxonomy)
  }

  @Test
  fun testFriendRequest() {
    val friendRequest = FriendRequest(senderId = "user1", receiverId = "user2")

    TestCase.assertEquals("user1", friendRequest.senderId)
    TestCase.assertEquals("user2", friendRequest.receiverId)
  }

  @Test
  fun testReport() {
    val report =
        Report(
            reportId = "report1",
            imageURL = "https://example.com/report_pic",
            location = Location(50.0, 8.0, "Test Location"),
            date = Timestamp.now(),
            description = "Test report",
            authorId = "user1",
            assigneeId = "user2",
        )

    TestCase.assertEquals("report1", report.reportId)
    TestCase.assertEquals("https://example.com/report_pic", report.imageURL)
    TestCase.assertEquals("Test Location", report.location.name)
    TestCase.assertEquals("Test report", report.description)
    TestCase.assertEquals("user1", report.authorId)
    TestCase.assertEquals("user2", report.assigneeId)
  }

  @Test
  fun testComment() {
    val comment =
        Comment(
            commentId = "comment1",
            parentId = "post1",
            authorId = "user1",
            text = "This is a comment.",
            date = Timestamp.now(),
            tag = CommentTag.POST_COMMENT,
        )

    TestCase.assertEquals("comment1", comment.commentId)
    TestCase.assertEquals("post1", comment.parentId)
    TestCase.assertEquals("user1", comment.authorId)
    TestCase.assertEquals("This is a comment.", comment.text)
  }

  @Test
  fun testLike() {
    val like = Like(likeId = "like1", postId = "post1", userId = "user1")

    TestCase.assertEquals("like1", like.likeId)
    TestCase.assertEquals("post1", like.postId)
    TestCase.assertEquals("user1", like.userId)
  }

  @Test
  fun testPost() {
    val post =
        Post(
            postId = "post1",
            authorId = "user1",
            pictureURL = "https://example.com/post_pic",
            location = Location(51.0, 9.0, "Post Location"),
            description = "Post description",
            date = Timestamp.now(),
            animalId = "animal1",
            likesCount = 10,
            commentsCount = 5,
        )

    TestCase.assertEquals("post1", post.postId)
    TestCase.assertEquals("user1", post.authorId)
    TestCase.assertEquals("https://example.com/post_pic", post.pictureURL)
    TestCase.assertEquals("Post Location", post.location?.name)
    TestCase.assertEquals("Post description", post.description)
    TestCase.assertEquals(10, post.likesCount)
    TestCase.assertEquals(5, post.commentsCount)
  }

  @Test
  fun testSimpleUser() {
    val simpleUser =
        SimpleUser(
            userId = "user1",
            username = "TestUser",
            profilePictureURL = "https://example.com/user_pic",
            userType = UserType.REGULAR,
        )

    TestCase.assertEquals("user1", simpleUser.userId)
    TestCase.assertEquals("TestUser", simpleUser.username)
    TestCase.assertEquals("https://example.com/user_pic", simpleUser.profilePictureURL)
    TestCase.assertEquals(UserType.REGULAR, simpleUser.userType)
  }

  @Test
  fun testUser() {
    val user =
        User(
            userId = "user1",
            username = "TestUser",
            name = "John",
            surname = "Doe",
            bio = "Nature enthusiast",
            profilePictureURL = "https://example.com/user_pic",
            userType = UserType.REGULAR,
            creationDate = Timestamp.now(),
            country = "Switzerland",
        )

    TestCase.assertEquals("user1", user.userId)
    TestCase.assertEquals("TestUser", user.username)
    TestCase.assertEquals("John", user.name)
    TestCase.assertEquals("Doe", user.surname)
    TestCase.assertEquals("Nature enthusiast", user.bio)
    TestCase.assertEquals("https://example.com/user_pic", user.profilePictureURL)
    TestCase.assertEquals(UserType.REGULAR, user.userType)
    TestCase.assertEquals("Switzerland", user.country)
  }

  @Test
  fun testUserAchievements() {
    val userAchievements =
        UserAchievements(
            userId = "user1",
            achievementsId = listOf("ach1", "ach2"),
            achievementsCount = 2,
        )

    TestCase.assertEquals("user1", userAchievements.userId)
    TestCase.assertEquals(2, userAchievements.achievementsCount)
  }

  @Test
  fun testUserAnimals() {
    val userAnimals =
        UserAnimals(userId = "user1", animalsId = listOf("animal1", "animal2"), animalsCount = 2)

    TestCase.assertEquals("user1", userAnimals.userId)
    TestCase.assertEquals(2, userAnimals.animalsCount)
  }

  @Test
  fun testLocation() {
    val location = Location(latitude = 50.0, longitude = 8.0, name = "Test Location")

    TestCase.assertEquals(50.0, location.latitude)
    TestCase.assertEquals(8.0, location.longitude)
    TestCase.assertEquals("Test Location", location.name)
  }

  @Test
  fun testNotification() {
    val date = Timestamp.now()
    val notification =
        Notification(
            notificationId = "notification1",
            targetId = "user1",
            authorId = "user2",
            isRead = false,
            title = "New Friend Request",
            body = "John Doe has sent you a friend request.",
            route = "route",
            date = date,
        )

    TestCase.assertEquals("notification1", notification.notificationId)
    TestCase.assertEquals("user1", notification.targetId)
    TestCase.assertEquals("user2", notification.authorId)
    TestCase.assertEquals(false, notification.isRead)
    TestCase.assertEquals("New Friend Request", notification.title)
    TestCase.assertEquals("John Doe has sent you a friend request.", notification.body)
    TestCase.assertEquals("route", notification.route)
    TestCase.assertEquals(date, notification.date)
  }
}
