package com.android.wildex.model.social

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileSearchDataStorageTest {
  private val mockMap = mapOf(
    "jonathan pilemand jona82" to "jonaUserId",
    "paul atreides muaddib" to "paulUserId",
    "rania hida ainar" to "raniaUserId",
    "youssef benhayoun youssef-9511" to "youssefUserId"
  )

  private val mockMapJson = JSONObject()
    .put("jonathan pilemand jona82", "jonaUserId")
    .put("paul atreides muaddib", "paulUserId")
    .put("rania hida ainar", "raniaUserId")
    .put("youssef benhayoun youssef-9511", "youssefUserId")

  private val mockMap2 = mapOf(
    "jonathan pilemand jona82" to "jonaUserId",
    "john cena uCantSeeMe" to "johnUserId",
    "rania hida ainar" to "raniaUserId",
  )

  private val mockMap2Json = JSONObject()
    .put("jonathan pilemand jona82", "jonaUserId")
    .put("john cena uCantSeeMe", "johnUserId")
    .put("rania hida ainar", "raniaUserId")

  private val context = InstrumentationRegistry
    .getInstrumentation()
    .targetContext

  private val fileSearchDataStorage = FileSearchDataStorage(context)

  private val file: File = File(context.cacheDir, "search_data.json")

  @Test
  fun readingWhenNoFileExistsReturnsEmptyMap(){
    val result = fileSearchDataStorage.read()
    Assert.assertEquals(emptyMap<String, String>(), result)
    Assert.assertTrue(!file.exists())
  }

  @Test
  fun initiallyDataIsNotUpdated(){
    Assert.assertFalse(fileSearchDataStorage.updated.value)
  }

  @Test
  fun writeUpdatesDataWhenNoFileExists(){
    fileSearchDataStorage.write(mockMap)
    Assert.assertTrue(fileSearchDataStorage.updated.value)
    Assert.assertEquals(mockMapJson.toString(), file.readText())
  }

  @Test
  fun writeUpdatesDataWhenOverwriting(){
    fileSearchDataStorage.write(mockMap)
    Assert.assertEquals(mockMapJson.toString(), file.readText())
    fileSearchDataStorage.write(mockMap2)
    Assert.assertEquals(mockMap2Json.toString(), file.readText())
    Assert.assertTrue(fileSearchDataStorage.updated.value)
  }

  @Test
  fun writingThenReadingUpdatesCorrectly(){
    fileSearchDataStorage.write(mockMap)
    Assert.assertTrue(fileSearchDataStorage.updated.value)
    val result = fileSearchDataStorage.read()
    Assert.assertFalse(fileSearchDataStorage.updated.value)
    Assert.assertEquals(mockMap, result)
  }

  @Test
  fun canReadAfterOverwrite(){
    fileSearchDataStorage.write(mockMap)
    fileSearchDataStorage.write(mockMap2)
    val result = fileSearchDataStorage.read()
    Assert.assertEquals(mockMap2, result)
    Assert.assertFalse(fileSearchDataStorage.updated.value)
  }

  @Test
  fun temporaryFileIsDeleted(){
    val tempFile = File(context.cacheDir, "search_data.tmp")
    fileSearchDataStorage.write(mockMap)
    Assert.assertFalse(tempFile.exists())
  }
}