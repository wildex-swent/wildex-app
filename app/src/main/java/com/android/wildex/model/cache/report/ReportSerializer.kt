package com.android.wildex.model.cache.report

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.android.wildex.datastore.ReportCacheStorage
import com.android.wildex.datastore.ReportProto
import com.android.wildex.model.cache.location.toLocation
import com.android.wildex.model.cache.location.toProto
import com.android.wildex.model.report.Report
import com.google.firebase.Timestamp
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/** Serializer for ReportCacheStorage to handle reading and writing of report cache data. */
object ReportCacheSerializer : Serializer<ReportCacheStorage> {
  override val defaultValue: ReportCacheStorage = ReportCacheStorage.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): ReportCacheStorage {
    try {
      return ReportCacheStorage.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto (report)", e)
    }
  }

  override suspend fun writeTo(t: ReportCacheStorage, output: OutputStream) {
    t.writeTo(output)
  }
}

val Context.reportDataStore: DataStore<ReportCacheStorage> by
    dataStore(fileName = "report_cache.pb", serializer = ReportCacheSerializer)

fun Report.toProto(): ReportProto {
  return ReportProto.newBuilder()
      .setReportId(this.reportId)
      .setImageUrl(this.imageURL)
      .setLocation(this.location.toProto())
      .setDate(this.date.seconds)
      .setDescription(this.description)
      .setAuthorId(this.authorId)
      .setAssigneeId(this.assigneeId ?: "")
      .setLastUpdated(System.currentTimeMillis())
      .build()
}

fun ReportProto.toReport(): Report {
  return Report(
      reportId = this.reportId,
      imageURL = this.imageUrl,
      location = this.location.toLocation(),
      date = Timestamp(this.date, 0),
      description = this.description,
      authorId = this.authorId,
      assigneeId = this.assigneeId.ifEmpty { null },
  )
}
