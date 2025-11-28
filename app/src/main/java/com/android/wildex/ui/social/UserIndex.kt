package com.android.wildex.ui.social

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.social.SearchDataProvider
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserRepository
import java.util.regex.Matcher
import java.util.regex.Pattern

private data class ScoredMatch(
  val score: Int,
  val string: String
)

private const val WORD_START_FACTOR = 4

private const val WORD_END_FACTOR = 2

class UserIndex(
  private val searchDataProvider: SearchDataProvider,
  private val userRepository: UserRepository = RepositoryProvider.userRepository
) {
  private val subQuerySeparator = Regex("\\s+")

  private val expansions = mapOf(
    "c" to "[cç]",
    "a" to "[aáàâä]",
    "e" to "[eéèêë]",
    "i" to "[iíìîï]",
    "o" to "[oóòôö]",
    "u" to "[uúùûü]"
  )

  private fun searchData() = searchDataProvider.getSearchData()

  suspend fun usersMatching(query: String, limit: Int): List<User>{
    if (searchDataProvider.dataNeedsUpdate.value) {
      searchDataProvider.invalidateCache()
    }
    val searchDataToUserIds = searchData()

    val patterns = subQuerySeparator.split(query)
      .filter { it.isNotBlank() }
      .map { patternFor(it) }

    val suggestionIds = searchDataToUserIds.keys.asSequence()
      .mapNotNull { totalScore(it, patterns) }
      .sortedByDescending { it.score }
      .mapNotNull { searchDataToUserIds[it.string] }
      .distinct()
      .take(limit)
      .toList()

    return suggestionIds.map { userRepository.getUser(it) }
  }

  private fun patternFor(subQuery: String): Pattern{
    val regex = buildString {
      for (c in subQuery) {
        append(expansions[c.toString()] ?: Pattern.quote(c.toString()))
      }
    }

    val flags = if (subQuery.none { it.isUpperCase() })
        (Pattern.CASE_INSENSITIVE.or(Pattern.UNICODE_CASE))
    else 0

    return Pattern.compile(regex, flags)
  }

  private fun totalScore(string: String, patterns: List<Pattern>): ScoredMatch?{
    var totalScore = 0
    for (pattern in patterns){
      val matcher = pattern.matcher(string)
      if (!matcher.find()) return null
      totalScore += score(string, matcher)
    }
    return ScoredMatch(totalScore, string)
  }

  private fun score(string: String, matcher: Matcher): Int{
    var baseScore = 100 * (matcher.end() - matcher.start()) / string.length
    if (matcher.start() == 0 || !string[matcher.start() - 1].isLetter())
      baseScore *= WORD_START_FACTOR
    if (matcher.end() == string.length || !string[matcher.end()].isLetter())
      baseScore *= WORD_END_FACTOR
    return baseScore
  }
}