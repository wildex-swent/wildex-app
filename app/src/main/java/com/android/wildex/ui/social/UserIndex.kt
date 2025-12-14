package com.android.wildex.ui.social

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.SearchDataProvider
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import com.android.wildex.model.utils.Id
import com.android.wildex.ui.utils.search.SearchEngine
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Factor to apply to the score of a user if the query is the beginning of the user's name, surname
 * or username
 */
private const val WORD_START_FACTOR = 4

/**
 * Factor to apply to the score of a user if the query is the end of the user's name, surname or
 * username
 */
private const val WORD_END_FACTOR = 2

/**
 * Determines and ranks users that match a search query.
 *
 * @property searchDataProvider gets the users data in order to determine matches to queries
 * @property userRepository gets the matching users in order to display them on the screen
 * @property searchEngine the search engine used to determine matches to queries
 */
class UserIndex(
    private val searchDataProvider: SearchDataProvider,
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val searchEngine: SearchEngine = SearchEngine(WORD_START_FACTOR, WORD_END_FACTOR)
) {
  /** Gets the users search data from the data provider */
  private fun searchData() = searchDataProvider.getSearchData()

  /**
   * Computes all users matching to a given query but only returns at most the top N=limit ones
   *
   * @param query the search query to match users to
   * @param limit the maximum number of wanted results
   * @return the list of matching users
   */
  suspend fun usersMatching(
      query: String,
      limit: Int,
      excludeIds: List<Id> = emptyList()
  ): List<User> {
      if (searchDataProvider.dataNeedsUpdate.value) {
          searchDataProvider.invalidateCache()
      }

      val searchDataToUserIds: Map<String, String> = searchData()

      val suggestionIds = searchEngine
          .search(query, searchDataToUserIds.keys.toList(), limit * 2)
          .mapNotNull { searchDataToUserIds[it.string] }
          .filter { !excludeIds.contains(it) }
          .distinct()
          .take(limit)

      return suggestionIds.map { userRepository.getUser(it) }
  }
}
