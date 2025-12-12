package com.android.wildex.utils.offline

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import com.android.wildex.datastore.UserCacheStorage

class TestContext(private val testDataStore: DataStore<UserCacheStorage>, base: Context) :
    ContextWrapper(base) {
  val userDataStore: DataStore<UserCacheStorage>
    get() = testDataStore
}
