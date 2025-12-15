package com.android.wildex.model.cache.user

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.android.wildex.datastore.OnBoardingStageProto
import com.android.wildex.datastore.UserCacheStorage
import com.android.wildex.datastore.UserProto
import com.android.wildex.datastore.UserTypeProto
import com.android.wildex.model.user.OnBoardingStage
import com.android.wildex.model.user.User
import com.android.wildex.model.user.UserType
import com.google.firebase.Timestamp
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object UserCacheSerializer : Serializer<UserCacheStorage> {
  override val defaultValue: UserCacheStorage = UserCacheStorage.getDefaultInstance()

  override suspend fun readFrom(input: InputStream): UserCacheStorage {
    try {
      return UserCacheStorage.parseFrom(input)
    } catch (e: InvalidProtocolBufferException) {
      throw CorruptionException("Cannot read proto (user)", e)
    }
  }

  override suspend fun writeTo(t: UserCacheStorage, output: OutputStream) {
    t.writeTo(output)
  }
}

val Context.userDataStore: DataStore<UserCacheStorage> by
    dataStore(fileName = "user_cache.pb", serializer = UserCacheSerializer)

fun User.toProto(): UserProto {
  return UserProto.newBuilder()
      .setUserId(this.userId)
      .setUserName(this.username)
      .setName(this.name)
      .setSurname(this.surname)
      .setBio(this.bio)
      .setProfilePictureUrl(this.profilePictureURL)
      .setUserType(
          when (this.userType) {
            UserType.REGULAR -> UserTypeProto.REGULAR
            UserType.PROFESSIONAL -> UserTypeProto.PROFESSIONAL
          })
      .setCreationDate(this.creationDate.seconds)
      .setOnBoardingStage(
          when (this.onBoardingStage) {
            OnBoardingStage.NAMING -> OnBoardingStageProto.NAMING
            OnBoardingStage.OPTIONAL -> OnBoardingStageProto.OPTIONAL
            OnBoardingStage.USER_TYPE -> OnBoardingStageProto.USER_TYPE
            OnBoardingStage.COMPLETE -> OnBoardingStageProto.COMPLETE
          })
      .setCountry(this.country)
      .setLastUpdated(System.currentTimeMillis())
      .build()
}

fun UserProto.toUser(): User {
  return User(
      userId = this.userId,
      username = this.userName,
      name = this.name,
      surname = this.surname,
      bio = this.bio,
      profilePictureURL = this.profilePictureUrl,
      userType =
          when (this.userType) {
            UserTypeProto.REGULAR -> UserType.REGULAR
            UserTypeProto.PROFESSIONAL -> UserType.PROFESSIONAL
            else -> UserType.REGULAR
          },
      creationDate = Timestamp(this.creationDate, 0),
      country = this.country,
      onBoardingStage =
          when (this.onBoardingStage) {
            OnBoardingStageProto.NAMING -> OnBoardingStage.NAMING
            OnBoardingStageProto.OPTIONAL -> OnBoardingStage.OPTIONAL
            OnBoardingStageProto.USER_TYPE -> OnBoardingStage.USER_TYPE
            OnBoardingStageProto.COMPLETE -> OnBoardingStage.COMPLETE
            else -> OnBoardingStage.COMPLETE
          },
  )
}
