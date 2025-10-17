package com.android.wildex

import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import okhttp3.OkHttpClient
import org.junit.Test

class MainActivityUnitTest {

  @Test
  fun httpClientProviderValidClient() {
    val client = HttpClientProvider.client

    assertNotNull(client)
    assertTrue(client is OkHttpClient)
  }
}
