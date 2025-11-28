package com.android.wildex.model.social

class SearchDataProvider(
  private val storage: FileSearchDataStorage
) {
  private var cache: Map<String, String>? = null

  val dataNeedsUpdate = storage.updated

  fun getSearchData(): Map<String, String> {
    if (cache == null || dataNeedsUpdate.value) {
      cache = storage.read()
    }
    return cache!!
  }

  fun invalidateCache() {
    cache = null
  }
}