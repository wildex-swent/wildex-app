package com.android.wildex.model.social

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SearchDataProviderTest {

  private val mockMap = mapOf(
    "jonathan pilemand jona82" to "jonaUserId",
    "paul atreides muaddib" to "paulUserId",
    "rania hida ainar" to "raniaUserId",
    "youssef benhayoun youssef-9511" to "youssefUserId"
  )

  private lateinit var fileSearchDataStorage: FileSearchDataStorage

  private lateinit var searchDataProvider: SearchDataProvider

  private val fileUpdated = MutableStateFlow(false)

  @Before
  fun setup(){
    fileSearchDataStorage = mockk(relaxed = true)
    every { fileSearchDataStorage.read() } answers {
      fileUpdated.value = false
      mockMap
    }
    every { fileSearchDataStorage.write(any()) } answers { fileUpdated.value = true }
    every { fileSearchDataStorage.updated } returns fileUpdated
    searchDataProvider = SearchDataProvider(fileSearchDataStorage)
  }

  @After
  fun tearDown() {
    clearMocks(fileSearchDataStorage)
  }

  @Test
  fun dataDoesNotNeedUpdateByDefault(){
    Assert.assertFalse(searchDataProvider.dataNeedsUpdate.value)
  }

  @Test
  fun cachingWorksAsIntended(){
    val result = searchDataProvider.getSearchData()
    Assert.assertEquals(mockMap, result)
    Assert.assertFalse(searchDataProvider.dataNeedsUpdate.value)
    verify(exactly = 1){fileSearchDataStorage.read()}
    val result2 = searchDataProvider.getSearchData()
    Assert.assertEquals(mockMap, result2)
    verify(exactly = 1){fileSearchDataStorage.read()}
  }

  @Test
  fun invalidateCacheWorksAsIntended(){
    val result = searchDataProvider.getSearchData()
    Assert.assertEquals(mockMap, result)
    Assert.assertFalse(searchDataProvider.dataNeedsUpdate.value)
    verify(exactly = 1){fileSearchDataStorage.read()}
    searchDataProvider.invalidateCache()
    val result2 = searchDataProvider.getSearchData()
    Assert.assertEquals(mockMap, result2)
    verify(exactly = 2){fileSearchDataStorage.read()}
  }

  @Test
  fun getSearchDataAfterFileOverwriteUpdates(){
    searchDataProvider.getSearchData()
    Assert.assertFalse(searchDataProvider.dataNeedsUpdate.value)
    fileSearchDataStorage.write(emptyMap())
    Assert.assertTrue(searchDataProvider.dataNeedsUpdate.value)
    verify(exactly = 1){fileSearchDataStorage.read()}
    searchDataProvider.invalidateCache()
    searchDataProvider.getSearchData()
    Assert.assertFalse(searchDataProvider.dataNeedsUpdate.value)
    verify(exactly = 2){fileSearchDataStorage.read()}
    searchDataProvider.getSearchData()
    verify(exactly = 2){fileSearchDataStorage.read()}
  }
}