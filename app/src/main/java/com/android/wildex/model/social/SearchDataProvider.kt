package com.android.wildex.model.social

/**
 * Fetches the latest version of the local search data file.
 *
 * @property storage local file maintainer used to read the local search data file
 */
class SearchDataProvider(private val storage: FileSearchDataStorage) {
  private var cache: Map<String, String>? = null

  /** Broadcasts the file maintainer update status to know when the cache is obsolete */
  val dataNeedsUpdate = storage.updated

  /**
   * Returns the search data file's content, cached so that we limit IO operations
   *
   * @return the search data file's content, as a map from user string representation to user ids
   */
  fun getSearchData(): Map<String, String> {
    if (cache == null) {
      cache = storage.read()
    }
    return cache!!
  }

  /** Invalidates the cache. To be used when the search data is updated */
  fun invalidateCache() {
    cache = null
  }
}
