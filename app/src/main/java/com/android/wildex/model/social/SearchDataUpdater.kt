package com.android.wildex.model.social

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.user.UserRepository

class SearchDataUpdater(
  private val userRepository: UserRepository = RepositoryProvider.userRepository,
  private val storage: FileSearchDataStorage
) {

  suspend fun updateSearchData(){
    val users = userRepository.getAllUsers()

    val index = mutableMapOf<String, String>()

    users.forEach { user ->
      index[user.name + " " + user.surname + " " + user.username] = user.userId
    }

    storage.write(index)
  }

}