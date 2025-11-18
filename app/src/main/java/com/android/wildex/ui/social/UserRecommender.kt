package com.android.wildex.ui.social

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

data class RecommendationResult(val user: SimpleUser, val reason: String)

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
    private val userFriendsRepository: UserFriendsRepository,
    private val friendRequestRepository: RelationshipRepository =
        RepositoryProvider.relationshipRepository,
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
        friendRequestRepository.getAllPendingRelationshipsBySender(currentUserId).map {
          it.receiverId
        }
    val pendingRequestsToCurrentUser =
        friendRequestRepository.getAllPendingRelationshipsByReceiver(currentUserId).map {
          it.senderId
        }

    // filter out users who are already friends or have pending requests with current user
    val candidates =
        users
            .filter { it.userId != currentUserId && !currentUserFriends.contains(it.userId) }
            .filter {
              !pendingRequestsByCurrentUser.contains(it.userId) &&
                  !pendingRequestsToCurrentUser.contains(it.userId)
            }

    // compute friends of friends map for mutual friends score
    val friendsOfFriendsMap = computeFriendsOfFriendsMap()
    val maxMutualFriends = friendsOfFriendsMap.values.maxOrNull() ?: 0

    // compute max friends for popularity score
    val maxFriends =
        candidates.maxOfOrNull { userFriendsRepository.getFriendsCountOfUser(it.userId) } ?: 0

    // compute mean location for current user and candidates for geo proximity score
    val currentUserMeanLocation = userMeanLocation(postRepository.getAllPostsByAuthor())
    val candidateDistances =
        computeCandidatesDistanceAndRecentPosts(
            currentUserMeanLocation, candidates.map { it.userId })
    val maxDistance = candidateDistances.values.maxOfOrNull { it.first } ?: 0.0
    val maxRecentPosts = candidateDistances.values.maxOfOrNull { it.second } ?: 0

    // compute final score and main suggestion reason for each candidate
    val candidatesScoreReason =
        candidates
            .map {
              val (mutualFriendsScore, mutualFriendsCount) =
                  computeMutualFriendsScore(it, friendsOfFriendsMap, maxMutualFriends)
              val candidateFriendsCount = userFriendsRepository.getFriendsCountOfUser(it.userId)
              val popularityScore = computePopularityScore(candidateFriendsCount, maxFriends)
              val (geoActivityScore, geoActivityReason) =
                  computeGeoProxAndActivityScore(
                      candidateDistances[it.userId] ?: Pair(1.0, 0), maxDistance, maxRecentPosts)

              val mutualFriendsContribution = 0.5 * mutualFriendsScore
              val popularityContribution = 0.2 * popularityScore
              val geoActivityContribution = 0.3 * geoActivityScore

              val maxContribution =
                  maxOf(mutualFriendsContribution, popularityContribution, geoActivityContribution)
              val reason =
                  when (maxContribution) {
                    mutualFriendsContribution ->
                        "shares $mutualFriendsCount common friend${if (mutualFriendsCount > 1) "s" else ""} with you"
                    popularityContribution -> "is popular in ${it.country}"
                    geoActivityContribution ->
                        "recently posted $geoActivityReason time${if (geoActivityReason > 1) "s" else ""} near you"
                    else -> ""
                  }

              val finalScore =
                  mutualFriendsContribution + popularityContribution + geoActivityContribution
              Triple(SimpleUser(it.userId, it.username, it.profilePictureURL), finalScore, reason)
            }
            .filter { it.second != 0.0 }

    val topCandidates = candidatesScoreReason.sortedByDescending { it.second }.take(limit)

    return topCandidates.map { RecommendationResult(it.first, it.third) }
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
      val friendsOfFriend = userFriendsRepository.getAllFriendsOfUser(friendId)
      for (potentialCandidateId in friendsOfFriend) {
        if (potentialCandidateId != currentUserId &&
            !currentUserFriends.contains(potentialCandidateId)) {
          userToMutualFriendsCount[potentialCandidateId] =
              userToMutualFriendsCount.getOrDefault(potentialCandidateId, 0) + 1
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
  ): Pair<Double, Int> {
    val mutualFriendsCount = friendsOfFriendsMap.getOrDefault(user.userId, 0)
    val mutualFriendsScore =
        if (maxMutualFriends != 0) mutualFriendsCount / maxMutualFriends.toDouble() else 0.0

    // compute score based on number of mutual friends with current user
    return Pair(mutualFriendsScore, mutualFriendsCount)
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
   * @param userDistanceAndRecentPosts a pair containing the distance to the current user and the
   *   number of recent posts of the user whose Close By Activity Score we want to compute
   * @param minDistance the minimum distance from any user to the current user
   * @param maxDistance the maximum distance from any user to the current user
   * @param maxRecentPosts the maximum number of posts made by any user in the last 30 days
   * @return a pair containing the Close By Activity Score and the number of recent posts made by
   *   the user whose score we computed
   */
  private fun computeGeoProxAndActivityScore(
      userDistanceAndRecentPosts: Pair<Double, Int>,
      maxDistance: Double,
      maxRecentPosts: Int
  ): Pair<Double, Int> {
    val (distance, recentPostsCount) = userDistanceAndRecentPosts
    val geoProximityScore = if (maxDistance != 0.0) (maxDistance - distance) / maxDistance else 1.0
    val activityScore =
        if (maxRecentPosts != 0) recentPostsCount / maxRecentPosts.toDouble() else 0.0

    return Pair(geoProximityScore * activityScore, recentPostsCount)
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

    return Pair(meanLatitude, meanLongitude)
  }

  /**
   * Computes the mapping from the given candidates to their distance to the current user and their
   * number of recent posts.
   *
   * @param currentUserMeanLocation the current user's average location, used to compute the
   *   candidates' distances from it
   * @param candidates the candidates whose distances to the current user and recent posts we need
   *   to compute
   * @return the mapping from all candidates to their distance to the current user and their number
   *   of recent posts
   */
  private suspend fun computeCandidatesDistanceAndRecentPosts(
      currentUserMeanLocation: Pair<Double, Double>,
      candidates: List<Id>
  ): Map<Id, Pair<Double, Int>> {
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
