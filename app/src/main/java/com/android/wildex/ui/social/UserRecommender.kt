package com.android.wildex.ui.social

import android.util.Log
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.friendRequest.FriendRequestRepository
import com.android.wildex.model.social.Post
import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.user.SimpleUser
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserFriendsRepository
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.google.firebase.Timestamp
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date
import kotlin.math.sqrt

/**
 * Represents a candidate during the selection phase when the ranking hasn't been done yet
 *
 * @property user a view of the candidate's info to display on the app if they are chosen
 * @property score the score associated to the candidate, between 0 and 1, 1 being the highest score
 * @property reason a description text to be displayed on the app as to why the candidate has been
 *   suggested if they are
 */
data class CandidateScore(val user: SimpleUser, val score: Double, val reason: String)

/**
 * Represents the Mutual Friends Score of a candidate
 *
 * @property score the actual Mutual Friends Score of the candidate between 0 and 1
 * @property mutualFriendsCount the number of mutual friends with the current user to be added to
 *   the suggestion reason if the candidate is suggested and the Mutual Friends Score is the highest
 *   score out of the three parameters for this candidate.
 */
data class MutualFriendsScore(val score: Double, val mutualFriendsCount: Int)

/**
 * Represents the Local Activity Score of a candidate
 *
 * @property score the actual Mutual Friends Score of the candidate between 0 and 1
 * @property recentPostsCount the number of recent posts made by the candidate to be added to the
 *   suggestion reason if the candidate is suggested and the Local Activity Score is the highest
 *   score out of the three parameters for this candidate.
 */
data class LocalActivityScore(val score: Double, val recentPostsCount: Int)

/**
 * Represents a result of the friend suggestion algorithm
 *
 * @property user the suggested user in the form of a SimpleUser, to be displayed on the app
 * @property reason the suggestion reason to be displayed on the app, to give the current user
 *   insight on why this particular user was suggested to him
 */
data class RecommendationResult(val user: SimpleUser, val reason: String)

private const val MUTUAL_FRIENDS_WEIGHT = 0.5

private const val POPULARITY_WEIGHT = 0.2

private const val LOCAL_ACTIVITY_WEIGHT = 0.3

/**
 * A class responsible for recommending users to the current user. Users are recommended based on
 * mutual friends, popularity, close activity level. The recommendation algorithm avoids suggesting
 * users who already have pending friend requests with the current user or who are already friends
 * with the current user. Mutual friends are prioritized in the recommendations. Then popular users
 * and those active in close proximity are considered. Each recommendation parameter is given a
 * score between 0 and 1 for each candidate and a weight is applied to calculate a final
 * recommendation score.
 *
 * @property currentUserId The ID of the current user.
 * @property userRepository The repository for accessing user data.
 * @property postRepository The repository for accessing posts data, to compute location proximity
 *   and activity level.
 * @property userFriendsRepository The repository for accessing user friends data.
 * @property friendRequestRepository The repository for accessing friend requests data, to avoid
 *   recommending users with pending requests.
 */
class UserRecommender(
  private val currentUserId: Id,
  private val userRepository: UserRepository = RepositoryProvider.userRepository,
  private val postRepository: PostsRepository = RepositoryProvider.postRepository,
  private val userFriendsRepository: UserFriendsRepository = RepositoryProvider.userFriendsRepository,
  private val friendRequestRepository: FriendRequestRepository =
        RepositoryProvider.friendRequestRepository,
) {

  /**
   * Returns the most interesting profiles to follow for the current user based on the algorithm
   * described in the class's documentation
   *
   * @param limit maximum size of the returned suggestion list
   * @return the results of the recommendation algorithm in the form of a list of users, each
   *   accompanied by a reason as to why they were suggested
   */
  suspend fun getRecommendedUsers(limit: Int = 10): List<RecommendationResult> {
    val users = userRepository.getAllUsers()

    val currentUserFriends = userFriendsRepository.getAllFriendsOfUser(currentUserId)
    val pendingRequestsByCurrentUser =
        friendRequestRepository.getAllFriendRequestsBySender(currentUserId).map { it.receiverId }
    val pendingRequestsToCurrentUser =
        friendRequestRepository.getAllFriendRequestsByReceiver(currentUserId).map { it.senderId }

    // filter out users who are already friends or have pending requests with current user
    val candidates =
        users
            .filter { it.userId != currentUserId && !currentUserFriends.contains(it) }
            .filter {
              !pendingRequestsByCurrentUser.contains(it.userId) &&
                  !pendingRequestsToCurrentUser.contains(it.userId)
            }

    val friendsOfFriendsMap = computeFriendsOfFriendsMap()
    val maxMutualFriends = friendsOfFriendsMap.values.maxOrNull() ?: 0

    // compute candidates friend counts and max friends for popularity score
    val candidatesFriendsCount =
        candidates.associate { it.userId to userFriendsRepository.getFriendsCountOfUser(it.userId) }
    val maxFriends = candidatesFriendsCount.values.maxOrNull()

    // compute mean location for current user and candidates for local activity score
    val currentUserPosts = postRepository.getAllPostsByAuthor()

    val currentUserMeanLocation = userMeanLocation(currentUserPosts)

    val candidatesPosts =
        candidates.associate { it.userId to postRepository.getAllPostsByGivenAuthor(it.userId) }
    val candidatesMeanLocations =
        candidatesPosts.mapValues { userMeanLocation(candidatesPosts[it.key] ?: emptyList()) }

    // compute candidates' distances from the current user for the local activity score
    val candidatesDistance =
        computeCandidatesDistance(currentUserMeanLocation, candidatesMeanLocations)

    // compute candidates' number of recent posts for the local activity score
    val candidatesRecentPosts = computeCandidatesRecentPosts(candidatesPosts)
    val maxDistance = candidatesDistance.values.maxOrNull()
    val maxRecentPosts = candidatesRecentPosts.values.maxOrNull()

    // compute final score and main suggestion reason for each candidate
    val candidatesScoreReason =
        candidates
            .map {
              // compute all scores
              val (mutualFriendsScore, mutualFriendsCount) =
                  computeMutualFriendsScore(it, friendsOfFriendsMap, maxMutualFriends)
              val candidateFriendsCount = candidatesFriendsCount[it.userId] ?: 0
              val popularityScore = computePopularityScore(candidateFriendsCount, maxFriends ?: 0)
              val (geoActivityScore, geoActivityReason) =
                  computeGeoProxAndActivityScore(
                      candidatesDistance[it.userId] ?: 0.0,
                      candidatesRecentPosts[it.userId] ?: 0,
                      maxDistance ?: 0.0,
                      maxRecentPosts ?: 0)

              val mutualFriendsContribution = MUTUAL_FRIENDS_WEIGHT * mutualFriendsScore
              val popularityContribution = POPULARITY_WEIGHT * popularityScore
              val geoActivityContribution = LOCAL_ACTIVITY_WEIGHT * geoActivityScore

              // Deduce suggestion reason
              val maxScore =
                  maxOf(mutualFriendsContribution, popularityContribution, geoActivityContribution)
              val reason =
                  when (maxScore) {
                    mutualFriendsContribution ->
                        "shares $mutualFriendsCount common friend${if (mutualFriendsCount > 1) "s" else ""} with you"
                    popularityContribution -> "is popular in ${it.country}"
                    geoActivityContribution ->
                        "recently posted $geoActivityReason time${if (geoActivityReason > 1) "s" else ""} near you"
                    else -> ""
                  }

              val finalScore =
                  mutualFriendsContribution + popularityContribution + geoActivityContribution
              CandidateScore(
                  SimpleUser(it.userId, it.username, it.profilePictureURL, it.userType),
                  finalScore,
                  reason)
            }
            .filter { it.score != 0.0 }

    val topCandidates = candidatesScoreReason.sortedByDescending { it.score }.take(limit)

    return topCandidates.map { RecommendationResult(it.user, it.reason) }
  }

  /**
   * Computes the mapping from users at distance 2 from the current user in the friendship graph to
   * the number of connections of each of these users to the current user. For example, if the
   * current user is friends with User1 and User2, and User3 is friends with User1, and User4 is
   * friends with User1 and User2, then the result of this function should be of the form {User3: 1,
   * User4: 2}
   *
   * @return the mapping from all friends of the current user's friends to their respective number
   *   of mutual friends with the current user
   */
  private suspend fun computeFriendsOfFriendsMap(): Map<Id, Int> {
    val userToMutualFriendsCount = mutableMapOf<Id, Int>()
    val currentUserFriends = userFriendsRepository.getAllFriendsOfUser(currentUserId)
    for (friendId in currentUserFriends) {
      val friendsOfFriend = userFriendsRepository.getAllFriendsOfUser(friendId.userId)
      for (potentialCandidateId in friendsOfFriend) {
        if (potentialCandidateId.userId != currentUserId &&
            !currentUserFriends.contains(potentialCandidateId)) {
          userToMutualFriendsCount[potentialCandidateId.userId] =
              userToMutualFriendsCount.getOrDefault(potentialCandidateId.userId, 0) + 1
        }
      }
    }
    return userToMutualFriendsCount
  }

  /**
   * Computes the Mutual Friends Score for the given user. This score is determined based on the
   * maximum number of mutual friends that the current user has with any friend of friend.
   *
   * @param user the user whose score we want to compute
   * @param friendsOfFriendsMap the mapping from the friends of the current user's friends to their
   *   respective number of mutual friends with the current user
   * @param maxMutualFriends the maximum number of mutual friends that any user has with the current
   *   user
   * @return a pair of values, the first one being the Mutual Friends Score, and the second being
   *   the number of mutual friends to include in the suggestion reason later on if this user is
   *   suggested
   */
  private fun computeMutualFriendsScore(
      user: User,
      friendsOfFriendsMap: Map<Id, Int>,
      maxMutualFriends: Int
  ): MutualFriendsScore {
    val mutualFriendsCount = friendsOfFriendsMap.getOrDefault(user.userId, 0)
    val mutualFriendsScore =
        if (maxMutualFriends != 0) mutualFriendsCount / maxMutualFriends.toDouble() else 0.0

    // compute score based on number of mutual friends with current user
    return MutualFriendsScore(mutualFriendsScore, mutualFriendsCount)
  }

  /**
   * Computes the Popularity Score for the given user stats. This score is based on the maximum
   * number of friends that any user has.
   *
   * @param userFriendsCount the number of friends of the user whose Popularity Score we want to
   *   compute
   * @param maxFriends the maximum number of friends that any user has
   * @return the Popularity Score for this user
   */
  private fun computePopularityScore(userFriendsCount: Int, maxFriends: Int): Double {
    return if (maxFriends != 0) userFriendsCount / maxFriends.toDouble() else 0.0
  }

  /**
   * Computes the Close By Activity Score for the given user stats. This score is determined based
   * on the minimum and maximum distances from any user to the current user, as well as on the
   * maximum number of recent posts that any user has made.
   *
   * @param userDistance the distance from the user to the current user
   * @param userRecentPosts the number of recent posts of the user
   * @param maxDistance the maximum distance from any user to the current user
   * @param maxRecentPosts the maximum number of posts made by any user in the last 30 days
   * @return a pair containing the Close By Activity Score and the number of recent posts made by
   *   the user whose score we computed
   */
  private fun computeGeoProxAndActivityScore(
      userDistance: Double,
      userRecentPosts: Int,
      maxDistance: Double,
      maxRecentPosts: Int
  ): LocalActivityScore {
    val geoProximityScore =
        if (maxDistance != 0.0) (maxDistance - userDistance) / maxDistance else 1.0
    val activityScore =
        if (maxRecentPosts != 0) userRecentPosts / maxRecentPosts.toDouble() else 0.0

    return LocalActivityScore(geoProximityScore * activityScore, userRecentPosts)
  }

  /**
   * Computes the mean location of a user based on the location of their posts. As a user uses the
   * app more and more, the average location of their posts should ultimately converge towards a
   * good approximation of their living location.
   *
   * @param userPosts the posts of the user whose average location we want to compute
   * @return a pair containing the average latitude and longitude of the user's posts
   */
  private fun userMeanLocation(userPosts: List<Post>): Pair<Double, Double> {
    val latitudes = userPosts.mapNotNull { it.location?.latitude }
    val longitudes = userPosts.mapNotNull { it.location?.longitude }

    val meanLatitude = if (latitudes.isNotEmpty()) latitudes.average() else 0.0
    val meanLongitude = if (longitudes.isNotEmpty()) longitudes.average() else 0.0

    return meanLatitude to meanLongitude
  }

  /**
   * Computes the mapping from the given candidates to their distance to the current user
   *
   * @param currentUserMeanLocation the current user's average location, used to compute the
   *   candidates' distances from it
   * @param candidatesMeanLocations the candidates' average location to compute the distances to the
   *   current user
   * @return the mapping from all candidates to their distance to the current user
   */
  private fun computeCandidatesDistance(
      currentUserMeanLocation: Pair<Double, Double>,
      candidatesMeanLocations: Map<Id, Pair<Double, Double>>
  ): Map<Id, Double> {
    return candidatesMeanLocations.mapValues {
      val x = currentUserMeanLocation.first - it.value.first
      val y = currentUserMeanLocation.second - it.value.second
      sqrt(x * x + y * y)
    }
  }

  /**
   * Computes the mapping from all given candidates to their number of recent posts
   *
   * @param candidatesPosts the candidates' posts, yet to be filtered to keep only recent ones
   * @return the mapping from all candidates to their number of recent posts
   */
  private fun computeCandidatesRecentPosts(candidatesPosts: Map<Id, List<Post>>): Map<Id, Int> {
    val oneMonthAgo = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(30).toInstant()
    val timestampOneMonthAgo = Timestamp(Date.from(oneMonthAgo))
    return candidatesPosts.mapValues {
      it.value.filter { post -> post.date >= timestampOneMonthAgo }.size
    }
  }
}
