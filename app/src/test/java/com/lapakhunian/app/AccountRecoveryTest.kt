package com.lapakhunian.app

import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.notifications.NotificationPrefs
import com.lapakhunian.app.feature.auth.AuthViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(ExperimentalCoroutinesApi::class)
class AccountRecoveryTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repo = mockk<PropertyRepository>()
    private lateinit var vm: AuthViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = AuthViewModel(repo, mockk<NotificationPrefs>()).also { store.put("auth", it) }
    }
    @After fun tearDown() { store.clear(); Dispatchers.resetMain() }

    @Test fun `registration retains the submitted email for verification instructions`() = runTest(dispatcher) {
        coEvery { repo.register(any(), any(), any(), any(), any()) } returns Result.success(Unit)
        vm.register("User", " user@example.test ", "password123", "owner", null)
        runCurrent()
        assertTrue(vm.state.value.registerSuccess)
        assertEquals("user@example.test", vm.state.value.registeredEmail)
    }

    @Test fun `invalid recovery email never requests an email`() = runTest(dispatcher) {
        vm.requestPasswordReset("invalid")
        runCurrent()
        assertNotNull(vm.state.value.error)
        coVerify(exactly = 0) { repo.requestPasswordReset(any()) }
    }

    @Test fun `reset uses neutral success copy and prevents repeated sends during cooldown`() = runTest(dispatcher) {
        coEvery { repo.requestPasswordReset(any()) } returns Result.success(Unit)
        vm.requestPasswordReset(" user@example.test ")
        vm.requestPasswordReset("user@example.test")
        runCurrent()
        assertTrue(vm.state.value.emailMessage.orEmpty().contains("Jika email terdaftar"))
        assertEquals(60, vm.state.value.emailCooldownSeconds)
        vm.requestPasswordReset("user@example.test")
        runCurrent()
        coVerify(exactly = 1) { repo.requestPasswordReset("user@example.test") }
        advanceTimeBy(60_000)
        runCurrent()
        vm.requestPasswordReset("user@example.test")
        runCurrent()
        coVerify(exactly = 2) { repo.requestPasswordReset("user@example.test") }
    }

    @Test fun `resend failure can be retried without claiming success`() = runTest(dispatcher) {
        coEvery { repo.resendVerification(any()) } returns Result.failure(IllegalStateException("offline"))
        vm.resendVerification("user@example.test")
        runCurrent()
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.emailMessage)
        assertFalse(vm.state.value.loading)
        coEvery { repo.resendVerification(any()) } returns Result.success(Unit)
        vm.resendVerification("user@example.test")
        runCurrent()
        assertNotNull(vm.state.value.emailMessage)
        assertNull(vm.state.value.error)
    }
}
