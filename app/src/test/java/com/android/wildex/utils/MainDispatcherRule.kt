// kotlin
package com.android.wildex.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) :
    TestWatcher() {
  private val scope = TestScope(dispatcher)

  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }

  fun runTest(block: suspend TestScope.() -> Unit) {
    scope.runTest(testBody = block)
  }
}
