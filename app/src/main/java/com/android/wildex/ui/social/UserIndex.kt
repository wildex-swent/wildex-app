package com.android.wildex.ui.social

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.SearchDataProvider
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Represents a result of the search, a user that corresponds to the query along with a score of how
 * close the query is to the user
 */
private data class ScoredMatch(val score: Int, val string: String)

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
 */
class UserIndex(
    private val searchDataProvider: SearchDataProvider,
    private val userRepository: UserRepository = RepositoryProvider.userRepository
) {
  /** Regex corresponding to a space to cut queries into composing words */
  private val subQuerySeparator = Regex("\\s+")

  /**
   * Mappings from non-accented letters to all of their accented versions so that if a user types
   * leonard -> Léonard isn't discarded as a result
   */
  private val expansions =
      mapOf(
          "c" to "[cç]",
          "a" to "[aáàâä]",
          "e" to "[eéèêë]",
          "i" to "[iíìîï]",
          "o" to "[oóòôö]",
          "u" to "[uúùûü]")

  /** Gets the users search data from the data provider */
  private fun searchData() = searchDataProvider.getSearchData()

  /**
   * Computes all users matching to a given query but only returns at most the top N=limit ones
   *
   * @param query the search query to match users to
   * @param limit the maximum number of wanted results
   * @return the list of matching users
   */
  suspend fun usersMatching(query: String, limit: Int): List<User> {
    if (searchDataProvider.dataNeedsUpdate.value) {
      searchDataProvider.invalidateCache()
    }
    val searchDataToUserIds: Map<String, String> = searchData()

    val patterns = subQuerySeparator.split(query).filter { it.isNotBlank() }.map { patternFor(it) }

    val suggestionIds =
        searchDataToUserIds.keys
            .asSequence()
            .mapNotNull { totalScore(it, patterns) }
            .sortedByDescending { it.score }
            .mapNotNull { searchDataToUserIds[it.string] }
            .distinct()
            .take(limit)
            .toList()

    return suggestionIds.map { userRepository.getUser(it) }
  }

  /**
   * Computes the Pattern for a given string which is a word of the search query. If a letter has
   * accented versions, then it is accounted for in the pattern. Also, if the query is exclusively
   * lowercase, then strings with uppercase letters can get matched to it (i.e leo matches entirely
   * with Leo), but on the contrary, if a query has some uppercase letters in it, it won't get
   * matched with lower case strings (i.e match between Leo and leo is {eo} and not {leo})
   *
   * @param subQuery word to compute the Pattern for
   * @return the pattern corresponding to the given sub query
   */
  private fun patternFor(subQuery: String): Pattern {
    val regex = buildString {
      for (c in subQuery) {
        append(expansions[c.toString()] ?: Pattern.quote(c.toString()))
      }
    }

    val flags =
        if (subQuery.none { it.isUpperCase() }) (Pattern.CASE_INSENSITIVE.or(Pattern.UNICODE_CASE))
        else 0

    return Pattern.compile(regex, flags)
  }

  /**
   * Computes the total score of a user's string representation (name, surname, username) relative
   * to a list of patterns which are all the words of a search query turned into a Pattern
   *
   * @param string a user's string representation (name + surname + username)
   * @param patterns a list of patterns which are the words of the search query formatted not to
   *   discard good candidates (i.e. not discard Léonard for query leo)
   * @return a scored match which represents the score of a user relative to a search query, null if
   *   the query doesn't match the user
   */
  private fun totalScore(string: String, patterns: List<Pattern>): ScoredMatch? {
    var totalScore = 0
    for (pattern in patterns) {
      val matcher = pattern.matcher(string)
      if (!matcher.find()) return null
      totalScore += score(string, matcher)
    }
    return ScoredMatch(totalScore, string)
  }

  /**
   * Computes the score of a user's string representation (name + surname + username) given a
   * matcher which represents the matching characters from the string to a search pattern
   *
   * @param string a user's string representation (name + surname + username)
   * @param matcher the matching elements of the string and a search query pattern
   * @return the score of the string
   */
  private fun score(string: String, matcher: Matcher): Int {
    var baseScore = 100 * (matcher.end() - matcher.start()) / string.length
    if (matcher.start() == 0 || string[matcher.start() - 1].isWhitespace())
        baseScore *= WORD_START_FACTOR
    if (matcher.end() == string.length || string[matcher.end()].isWhitespace())
        baseScore *= WORD_END_FACTOR
    return baseScore
  }
}
