package com.android.wildex.utils.offline

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.core.DataStore

class TestContext<T>(private val testDataStore: DataStore<T>, base: Context) :
    ContextWrapper(base) {
  val dataStore: DataStore<T>
    get() = testDataStore
}
