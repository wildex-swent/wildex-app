package com.android.wildex.model

import com.android.wildex.model.social.PostsRepository
import com.android.wildex.model.social.PostsRepositoryFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/** Provides a single instance of all the repository in the app. */
object RepositoryProvider {

  val postRepository: PostsRepository by lazy { PostsRepositoryFirestore(Firebase.firestore) }
}
