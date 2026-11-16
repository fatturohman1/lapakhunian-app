package com.lapakhunian.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.ChatRepository
import com.lapakhunian.app.core.network.dto.MessageDto
import com.lapakhunian.app.feature.chat.ThreadViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class ThreadViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repo = mockk<ChatRepository>()
    private val response = CompletableDeferred<Result<MessageDto>>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repo.conversations() } returns flowOf(Result.success(emptyList()))
        coEvery { repo.sendMessage(any(), any()) } coAnswers { response.await() }
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel() = ThreadViewModel(SavedStateHandle(mapOf("conversationId" to "test-thread")), repo)
        .also { store.put("thread", it) }

    @Test
    fun `failed send preserves text typed while waiting`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.updateDraft("Pesan A")
        vm.send(vm.state.value.draft)
        runCurrent()
        vm.updateDraft("Draf B yang belum selesai")
        response.complete(Result.failure(IllegalStateException("offline")))
        runCurrent()
        assertEquals("Draf B yang belum selesai", vm.state.value.draft)
        assertEquals(listOf("Pesan A"), vm.state.value.failedMessages.map { it.content })
        assertFalse(vm.state.value.sending)
    }

    @Test
    fun `retry sends failed message without clearing the current draft`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.send("Pesan A")
        runCurrent()
        response.complete(Result.failure(IllegalStateException("offline")))
        runCurrent()
        val failedId = requireNotNull(vm.state.value.failedMessages.single().id)
        vm.updateDraft("Draf B")
        coEvery { repo.sendMessage(any(), any()) } returns Result.success(MessageDto(id = "sent-a", content = "Pesan A"))
        vm.retryFailed(failedId)
        runCurrent()
        assertEquals("Draf B", vm.state.value.draft)
        assertEquals(emptyList<MessageDto>(), vm.state.value.failedMessages)
        assertEquals(listOf("Pesan A"), vm.state.value.messages.map { it.content })
    }

    @Test
    fun `repeated failure keeps one recoverable copy and preserves an empty draft`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.send("Pesan A")
        runCurrent()
        response.complete(Result.failure(IllegalStateException("offline")))
        runCurrent()
        vm.retryFailed(requireNotNull(vm.state.value.failedMessages.single().id))
        runCurrent()
        assertEquals("", vm.state.value.draft)
        assertEquals(listOf("Pesan A"), vm.state.value.failedMessages.map { it.content })
        assertFalse(vm.state.value.sending)
    }

    @Test
    fun `sending another message while waiting does not discard its draft`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.send("Pesan A")
        runCurrent()
        vm.updateDraft("Draf B")
        vm.send(vm.state.value.draft)
        assertEquals("Draf B", vm.state.value.draft)
        response.complete(Result.failure(IllegalStateException("offline")))
        runCurrent()
        assertEquals(listOf("Pesan A"), vm.state.value.failedMessages.map { it.content })
    }

    @Test
    fun `successful send preserves text typed while waiting`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.updateDraft("Pesan A")
        vm.send(vm.state.value.draft)
        runCurrent()
        vm.updateDraft("Draf B")
        response.complete(Result.success(MessageDto(id = "sent-a", content = "Pesan A")))
        runCurrent()
        assertEquals("Draf B", vm.state.value.draft)
        assertEquals(listOf("sent-a"), vm.state.value.messages.map { it.id })
        assertFalse(vm.state.value.sending)
    }
}
