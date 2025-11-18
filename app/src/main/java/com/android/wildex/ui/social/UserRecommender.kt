package com.android.wildex.ui.social

import android.util.Log
import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.relationship.RelationshipRepository
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

data class RecommendationResult(
  val user: SimpleUser,
  val reason: String
)

/**
 * A class responsible for recommending users to the current user.
 * Users are recommended based on mutual friends, popularity, close activity level.
 * The recommendation algorithm avoids suggesting users who already have pending friend requests
 * with the current user or who are already friends with the current user.
 * Mutual friends are prioritized in the recommendations. Then popular users and those active in close
 * proximity are considered.
 * Each recommendation parameter is given a score between 0 and 1 for each candidate and a weight is
 * applied to calculate a final recommendation score.
 *
 * @property currentUserId The ID of the current user.
 * @property userRepository The repository for accessing user data.
 * @property postRepository The repository for accessing posts data, to compute location proximity and activity level.
 * @property userFriendsRepository The repository for accessing user friends data.
 * @property friendRequestRepository The repository for accessing friend requests data, to avoid recommending users with pending requests.
 */
class UserRecommender (
  private val currentUserId: Id,
  private val userRepository: UserRepository = RepositoryProvider.userRepository,
  private val postRepository: PostsRepository = RepositoryProvider.postRepository,
  private val userFriendsRepository: UserFriendsRepository,
  private val friendRequestRepository: RelationshipRepository = RepositoryProvider.relationshipRepository,
) {

  suspend fun getRecommendedUsers(limit: Int = 10): List<RecommendationResult> {
    val users = userRepository.getAllUsers()
    val currentUserFriends = userFriendsRepository.getAllFriendsOfUser(currentUserId)
    val pendingRequestsByCurrentUser = friendRequestRepository.getAllPendingRelationshipsBySender(currentUserId).map { it.receiverId }
    val pendingRequestsToCurrentUser = friendRequestRepository.getAllPendingRelationshipsByReceiver(currentUserId).map { it.senderId }

    //filter out users who are already friends or have pending requests with current user
    val candidates = users
      .filter { it.userId != currentUserId && !currentUserFriends.contains(it.userId) }
      .filter { !pendingRequestsByCurrentUser.contains(it.userId) && !pendingRequestsToCurrentUser.contains(it.userId)}

    //compute friends of friends map for mutual friends score
    val friendsOfFriendsMap = computeFriendsOfFriendsMap()
    val maxMutualFriends = friendsOfFriendsMap.values.maxOrNull() ?: 0

    //compute max friends for popularity score
    val maxFriends = candidates.maxOfOrNull { userFriendsRepository.getFriendsCountOfUser(it.userId) } ?: 0

    //compute mean location for current user and candidates for geo proximity score
    val currentUserMeanLocation = userMeanLocation(postRepository.getAllPostsByAuthor())
    val candidateDistances = computeCandidatesDistanceAndRecentPosts(currentUserMeanLocation, candidates.map { it.userId })
    val minDistance = candidateDistances.values.minOfOrNull { it.first } ?: 0.0
    val maxDistance = candidateDistances.values.maxOfOrNull { it.first } ?: 0.0
    val maxRecentPosts = candidateDistances.values.maxOfOrNull { it.second } ?: 0

    //compute final score and main suggestion reason for each candidate
    val candidatesScoreReason = candidates.map {
      val (mutualFriendsScore, mutualFriendsCount) = computeMutualFriendsScore(it, friendsOfFriendsMap, maxMutualFriends)
      val candidateFriendsCount = userFriendsRepository.getFriendsCountOfUser(it.userId)
      val (popularityScore, popularityReason) = computePopularityScore(candidateFriendsCount, maxFriends, it.country)
      val (geoActivityScore, geoActivityReason) =
        computeGeoProxAndActivityScore(candidateDistances[it.userId] ?: Pair(1.0, 0), minDistance, maxDistance, maxRecentPosts)

      val mutualFriendsContribution = 0.5 * mutualFriendsScore
      val popularityContribution = 0.2 * popularityScore
      val geoActivityContribution = 0.3 * geoActivityScore
      Log.w(null, "Candidate ${it.userId}: Mutual Friend Score = $mutualFriendsScore, Popularity Score = $popularityScore, GeoActivity Score = $geoActivityScore")

      val maxContribution = maxOf(mutualFriendsContribution, popularityContribution, geoActivityContribution)
      val reason = when (maxContribution) {
        mutualFriendsContribution -> "shares $mutualFriendsCount common friend${if (mutualFriendsCount > 1) "s" else ""} with you"
        popularityContribution -> "is popular in $popularityReason"
        geoActivityContribution -> "recently posted $geoActivityReason time${if (geoActivityReason > 1) "s" else ""} near you"
        else -> ""
      }

      val finalScore = mutualFriendsContribution + popularityContribution + geoActivityContribution
      Triple(SimpleUser(it.userId, it.username, it.profilePictureURL), finalScore, reason)
    }.filter { it.second != 0.0 }

    val topCandidates = candidatesScoreReason
      .sortedByDescending { it.second }
      .take(limit)

    return topCandidates.map {
      RecommendationResult(it.first, it.third)
    }
  }

  private suspend fun computeFriendsOfFriendsMap(): Map<Id, Int> {
    val userToMutualFriendsCount = mutableMapOf<Id, Int>()
    val currentUserFriends = userFriendsRepository.getAllFriendsOfUser(currentUserId)
    for (friendId in currentUserFriends) {
      val friendsOfFriend = userFriendsRepository.getAllFriendsOfUser(friendId)
      for (potentialCandidateId in friendsOfFriend) {
        if (potentialCandidateId != currentUserId && !currentUserFriends.contains(potentialCandidateId)) {
          userToMutualFriendsCount[potentialCandidateId] =
            userToMutualFriendsCount.getOrDefault(potentialCandidateId, 0) + 1
        }
      }
    }
    return userToMutualFriendsCount
  }

  private fun computeMutualFriendsScore(user: User, friendsOfFriendsMap: Map<Id, Int>, maxMutualFriends: Int): Pair<Double, Int> {
    val mutualFriendsCount = friendsOfFriendsMap.getOrDefault(user.userId, 0)
    val mutualFriendsScore =
      if (maxMutualFriends != 0) mutualFriendsCount / maxMutualFriends.toDouble()
      else 0.0

    //compute score based on number of mutual friends with current user
    return Pair(mutualFriendsScore, mutualFriendsCount)
  }

  private fun computePopularityScore(userFriendsCount: Int, maxFriends: Int, userCountry: String): Pair<Double, String> {
    return Pair(if (maxFriends != 0) userFriendsCount / maxFriends.toDouble() else 0.0, userCountry)
  }

  private fun computeGeoProxAndActivityScore(
    userDistanceAndRecentPosts: Pair<Double, Int>,
    minDistance: Double,
    maxDistance: Double,
    maxRecentPosts: Int
  ): Pair<Double, Int> {
    val (distance, recentPostsCount) = userDistanceAndRecentPosts
    val geoProximityScore = if (minDistance != maxDistance) (maxDistance - distance) / (maxDistance - minDistance) else 1.0
    val activityScore = if (maxRecentPosts != 0) recentPostsCount / maxRecentPosts.toDouble() else 0.0

    return Pair(geoProximityScore * activityScore, recentPostsCount)
  }

  private fun userMeanLocation(userPosts: List<Post>): Pair<Double, Double> {
    val latitudes = userPosts.mapNotNull { it.location?.latitude }
    val longitudes = userPosts.mapNotNull { it.location?.longitude }

    val meanLatitude = if (latitudes.isNotEmpty()) latitudes.average() else 0.0
    val meanLongitude = if (longitudes.isNotEmpty()) longitudes.average() else 0.0

    return Pair(meanLatitude, meanLongitude)
  }

  private suspend fun computeCandidatesDistanceAndRecentPosts(
    currentUserMeanLocation: Pair<Double, Double>,
    candidates: List<Id>
  ) : Map<Id, Pair<Double, Int>> {
    val candidateDistances = mutableMapOf<Id, Pair<Double, Int>>()
    val oneMonthAgo = ZonedDateTime.now(ZoneId.systemDefault()).minusDays(30).toInstant()
    val timestampOneMonthAgo = Timestamp(Date.from(oneMonthAgo))
    for (candidate in candidates) {
      val candidatePosts = postRepository.getAllPostsByGivenAuthor(candidate)
      val recentPosts = candidatePosts.filter { it.date >= timestampOneMonthAgo }.size
      val candidateMeanLocation = userMeanLocation(candidatePosts)
      val x = currentUserMeanLocation.first - candidateMeanLocation.first
      val y = currentUserMeanLocation.second - candidateMeanLocation.second
      val distance = sqrt(x * x + y * y)
      candidateDistances[candidate] = Pair(distance, recentPosts)
    }
    return candidateDistances
  }
}