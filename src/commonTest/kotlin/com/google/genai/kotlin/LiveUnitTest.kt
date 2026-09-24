/*
 * Copyright 2026 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.genai.kotlin

import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.websocket.Frame
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest

private const val TEST_MODEL = "gemini-3.1-flash-live-preview"

/**
 * Tests for [Live] that need no server, using a mocked WebSocket session. See [LiveTest] for the
 * record/replay tests.
 */
class LiveUnitTest {

  private val job = Job()

  private val socket =
    mockk<DefaultClientWebSocketSession>().also { every { it.coroutineContext } returns job }

  private fun live(): Live {
    val apiClient = spyk(ApiClient(apiKey = "test-api-key"))
    coEvery { apiClient.openWebSocketSession(any()) } returns socket
    return Live(apiClient)
  }

  @Test
  fun testConnect_setupFailureClosesTheSocket() = runTest {
    coEvery { socket.send(any<Frame>()) } throws IllegalStateException("setup send failed")

    val thrown = assertFailsWith<IllegalStateException> { live().connect(TEST_MODEL) }

    assertEquals("setup send failed", thrown.message, "the original failure must not be masked")
    assertTrue(job.isCancelled, "the open socket must not be left behind")
  }

  @Test
  fun testConnect_successLeavesTheSocketOpen() = runTest {
    coEvery { socket.send(any<Frame>()) } returns Unit

    live().connect(TEST_MODEL)

    assertFalse(job.isCancelled)
  }
}
