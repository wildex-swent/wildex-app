package com.android.wildex.model.social

import com.android.wildex.model.RepositoryProvider
import com.android.wildex.model.user.UserRepository

/**
 * Updates the local search data file with the latest state of the user database.
 *
 * @property userRepository repository used to get the users' data to update the file
 * @property storage local file maintainer used to write to the local search data file
 */
class SearchDataUpdater(
    private val userRepository: UserRepository = RepositoryProvider.userRepository,
    private val storage: FileSearchDataStorage
) {

  /**
   * Updates the search data by fetching all users from the database and writing to the local search
   * data file a map from users' string representation (user's name + user's surname + user's
   * username) to the users's unique identifiers.
   */
  suspend fun updateSearchData() {
    val users = userRepository.getAllUsers()

    val index = mutableMapOf<String, String>()

    users.forEach { user ->
      index[user.name + " " + user.surname + " " + user.username] = user.userId
    }

    storage.write(index)
  }
}
