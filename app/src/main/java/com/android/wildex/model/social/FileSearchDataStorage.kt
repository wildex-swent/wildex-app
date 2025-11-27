package com.android.wildex.model.social

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.IOException

class FileSearchDataStorage (
  context: Context
){
  private val file: File = File(context.cacheDir, "search_data.json")

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

      map
    } catch (e: Exception) {
      e.printStackTrace()
      emptyMap()
    }
  }

  fun write(data: Map<String, String>){
    try {
      val jsonObject = JSONObject()
      data.forEach { (key, value) ->
        jsonObject.put(key, value)
      }

      val tempFile = File(file.absolutePath + ".tmp")
      tempFile.writeText(jsonObject.toString())

      if (file.exists()) file.delete()
      tempFile.renameTo(file)

    } catch (e: IOException) {
      e.printStackTrace()
    }
  }
}