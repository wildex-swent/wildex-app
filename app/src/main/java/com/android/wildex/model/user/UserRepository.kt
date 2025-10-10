package com.android.wildex.model.user

import com.android.wildex.model.utils.Id

/** Represents a repository that manages Users. */
interface UserRepository {

  /** Generates and returns a new unique identifier for a user. */
  fun getNewUid(): String

  /**
   * Retrieves a user by their unique identifier.
   *
   * @param userId The unique identifier of the user to retrieve.
   * @return The User object if found, or `null` if no user exists with the given ID.
   * @throws Exception if the User is not found
   */
  suspend fun getUser(userId: Id): User

  /**
   * Retrieves a simplified version of a user by their unique identifier.
   *
   * @param userId The unique identifier of the user to retrieve.
   * @return The SimpleUser object if found, or `null` if no user exists with the given ID.
   * @throws Exception if the SimpleUser is not found
   */
  suspend fun getSimpleUser(userId: Id): SimpleUser

  /**
   * Creates a new user in the repository.
   *
   * @param user The User object containing the details of the user to create.
   * @return `true` if the user was successfully created, `false` otherwise.
   */
  suspend fun createUser(user: User)

  /**
   * Updates an existing user in the repository.
   *
   * @param newUser The User object containing the updated details of the user.
   * @return `true` if the user was successfully updated, `false` otherwise.
   * @throws Exception if the User is not found
   */
  suspend fun editUser(userId: Id, newUser: User)

  /**
   * Deletes a user from the repository by their unique identifier.
   *
   * @param userId The unique identifier of the user to delete.
   * @return `true` if the user was successfully deleted, `false` otherwise.
   * @throws Exception if the User is not found
   */
  suspend fun deleteUser(userId: Id)
}
