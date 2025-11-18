package com.android.wildex.model.animaldetector

import android.content.ContentResolver
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before

abstract class AnimalInfoRepositoryTest {

  protected lateinit var mockWebServer: MockWebServer
  protected lateinit var client: OkHttpClient
  protected lateinit var repository: AnimalInfoRepositoryHttp
  protected lateinit var context: Context
  protected lateinit var contentResolver: ContentResolver
  abstract val urlPropName: String

  @Before
  open fun setup() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    client = OkHttpClient()
    repository = AnimalInfoRepositoryHttp(client)

    // Mock context and its content resolver
    context = mockk()
    contentResolver = mockk()
    every { context.contentResolver } returns contentResolver

    // Override baseUrl. Path matching is optional in this case.
    val mockUrl = mockWebServer.url("").toString()
    val urlField = repository.javaClass.getDeclaredField(urlPropName)
    urlField.isAccessible = true
    urlField.set(repository, mockUrl)
  }

  @After
  fun teardown() {
    mockWebServer.shutdown()
  }
}
