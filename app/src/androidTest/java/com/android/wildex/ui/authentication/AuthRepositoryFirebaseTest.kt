package com.android.wildex.ui.authentication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.wildex.model.authentication.AuthRepositoryFirebase
import com.android.wildex.utils.FakeCredentialManager
import com.android.wildex.utils.FakeJwtGenerator
import com.android.wildex.utils.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthRepositoryFirebaseTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var repository: AuthRepositoryFirebase

    @Before
    fun setUp() {
        auth = FirebaseEmulator.auth
        auth.signOut()
        repository = AuthRepositoryFirebase(auth)
    }

    @Test
    fun google_sign_in_is_configured() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val resourceId =
            context.resources.getIdentifier("default_web_client_id", "string", context.packageName)

        assumeTrue("Google Sign-In not configured - skipping test", resourceId != 0)

        val clientId = context.getString(resourceId)

        assertTrue(
            "Invalid Google client ID format: $clientId", clientId.endsWith(".googleusercontent.com")
        )
    }
}
