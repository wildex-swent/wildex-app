package com.android.wildex.ui.utils.search

import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.text.iterator

/**
 * Represents a result of the search, containing the matched string and its relevance score
 */
data class ScoredMatch(val score: Int, val string: String)

/**
 * Generic text search engine that supports fuzzy matching with accent-insensitive search
 * and intelligent scoring based on word boundaries.
 *
 * @property wordStartFactor multiplier when query matches the beginning of a word
 * @property wordEndFactor multiplier when query matches the end of a word
 */
class SearchEngine(
    private val wordStartFactor: Int = 4,
    private val wordEndFactor: Int = 2
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

    /**
     * Searches through a list of strings and returns the top matches
     *
     * @param query the search query
     * @param candidates list of strings to search through
     * @param limit maximum number of results to return
     * @return list of scored matches ordered by relevance
     */
    fun search(
        query: String,
        candidates: List<String>,
        limit: Int
    ): List<ScoredMatch> {
        val patterns = subQuerySeparator.split(query)
            .filter { it.isNotBlank() }
            .map { patternFor(it) }

        return candidates
            .asSequence()
            .mapNotNull { totalScore(it, patterns) }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
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

        val flags = if (subQuery.none { it.isUpperCase() }) {
            Pattern.CASE_INSENSITIVE.or(Pattern.UNICODE_CASE)
        } else {
            0
        }

        return Pattern.compile(regex, flags)
    }

    /**
     * Computes the total score of a string representation relative
     * to a list of patterns which are all the words of a search query turned into a Pattern
     *
     * @param string a string representation
     * @param patterns a list of patterns which are the words of the search query formatted not to
     *   discard good candidates (i.e. not discard Léonard for query leo)
     * @return a scored match which represents the score of a relative to a search query, null if
     *   the query doesn't match
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
     * Computes the score of a string representation given a
     * matcher which represents the matching characters from the string to a search pattern
     *
     * @param string a string representation
     * @param matcher the matching elements of the string and a search query pattern
     * @return the score of the string
     */
    private fun score(string: String, matcher: Matcher): Int {
        var baseScore = 100 * (matcher.end() - matcher.start()) / string.length
        if (matcher.start() == 0 || string[matcher.start() - 1].isWhitespace()) {
            baseScore *= wordStartFactor
        }
        if (matcher.end() == string.length || string[matcher.end()].isWhitespace()) {
            baseScore *= wordEndFactor
        }
        return baseScore
    }
}
