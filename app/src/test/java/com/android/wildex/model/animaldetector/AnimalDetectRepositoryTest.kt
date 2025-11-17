package com.android.wildex.model.animaldetector

import android.content.ContentResolver
import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.mockito.Mockito

abstract class AnimalDetectRepositoryTest {

  protected lateinit var mockWebServer: MockWebServer
  protected lateinit var client: OkHttpClient
  protected lateinit var repository: AnimalInfoRepositoryHttp
  protected lateinit var context: Context
  protected lateinit var contentResolver: ContentResolver
  abstract val urlPropName: String

  @Before
  open fun setUp() {
    mockWebServer = MockWebServer()
    mockWebServer.start()

    client = OkHttpClient()
    repository = AnimalInfoRepositoryHttp(client)

    // Mock context and its content resolver
    context = Mockito.mock(Context::class.java)
    contentResolver = Mockito.mock(ContentResolver::class.java)
    Mockito.`when`(context.contentResolver).thenReturn(contentResolver)

    // Override client
    val field = repository.javaClass.getDeclaredField("client")
    field.isAccessible = true
    field.set(repository, client)

    // Override baseUrl. Path matching is optional in this case.
    val mockUrl = mockWebServer.url("").toString()
    val urlField = repository.javaClass.getDeclaredField(urlPropName)
    urlField.isAccessible = true
    urlField.set(repository, mockUrl)
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }
}
