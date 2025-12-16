package com.android.wildex.model.social

import android.content.Context
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Maintains a local search data file to allow fast user searching
 *
 * @param context Android application environment to access the device's cache directory where the
 *   file is stored
 */
class FileSearchDataStorage(context: Context) {
  private val file: File = File(context.cacheDir, "search_data.json")

  private val _updated = MutableStateFlow(false)

  /** Boolean state flow, true if the file was written to but not yet read, false otherwise */
  val updated = _updated.asStateFlow()

  /**
   * Reads the local file, which contains a Json map, and turns it into a Map from user string
   * representation to user id.
   *
   * @return the mapping from user string representation to user id
   */
  fun read(): Map<String, String> {
    if (!file.exists()) return emptyMap()

    return try {
      val jsonText = file.readText()
      val jsonObject = JSONObject(jsonText)

      val map = mutableMapOf<String, String>()
      val keys = jsonObject.keys()

      while (keys.hasNext()) {
        val key = keys.next()
        map[key] = jsonObject.getString(key)
      }

      // since we read the file, reset the updated flag
      _updated.value = false
      map
    } catch (e: Exception) {
      e.printStackTrace()
      emptyMap()
    }
  }

  /**
   * Writes the given mapping to a temporary file for security before deleting the old file and
   * renaming the temporary one
   *
   * @param data the map to write to the file
   */
  fun write(data: Map<String, String>) {
    try {
      val jsonObject = JSONObject()
      data.forEach { (key, value) -> jsonObject.put(key, value) }

      val tempFile = File(file.absolutePath + ".tmp")
      tempFile.writeText(jsonObject.toString())

      Files.move(
          tempFile.toPath(),
          file.toPath(),
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.ATOMIC_MOVE)

      // since we wrote to the file, set the updated flag
      _updated.value = true
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
}
