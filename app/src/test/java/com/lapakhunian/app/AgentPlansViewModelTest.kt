package com.lapakhunian.app

import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.network.dto.AgentSubscriptionDto
import com.lapakhunian.app.core.network.dto.SubscriptionDto
import com.lapakhunian.app.feature.dashboard.AgentPlansViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Pembayaran (subscribe) kini dilakukan di NativePaymentScreen (VA/QRIS native).
 * VM plans tinggal memuat paket + kartu tagihan pending + aksi batalkan.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AgentPlansViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val repo = mockk<DashboardRepository>()
    private val session = mockk<SessionManager>()

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { session.token() } returns "test-session"
        coEvery { repo.subscription() } returns Result.success(SubscriptionDto(isActive = true))
        coEvery { repo.agentPlans() } returns Result.success(emptyList())
        coEvery { repo.agentSubscription() } returns Result.success(null)
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(): AgentPlansViewModel =
        AgentPlansViewModel(repo, session).also { store.put("plans", it) }

    @Test fun `pending subscription surfaces as pending card`() = runTest(dispatcher) {
        coEvery { repo.agentSubscription() } returns Result.success(
            AgentSubscriptionDto(id = "sub-1", status = "pending", paymentRedirectUrl = null),
        )
        val vm = viewModel()
        vm.retry() // VM tidak load di init — layar memuat via ON_RESUME
        runCurrent()
        assertEquals("sub-1", vm.state.value.pendingSub?.id)
    }

    @Test fun `active subscription hides pending card`() = runTest(dispatcher) {
        coEvery { repo.agentSubscription() } returns Result.success(
            AgentSubscriptionDto(id = "sub-2", status = "active"),
        )
        val vm = viewModel()
        vm.retry()
        runCurrent()
        assertNull(vm.state.value.pendingSub)
    }

    @Test fun `cancel pending succeeds then reloads plans`() = runTest(dispatcher) {
        coEvery { repo.agentSubscription() } returnsMany listOf(
            Result.success(AgentSubscriptionDto(id = "sub-3", status = "pending")),
            Result.success(null),
        )
        coEvery { repo.cancelPendingSubscription() } returns Result.success(Unit)
        val vm = viewModel()
        vm.retry()
        runCurrent()
        vm.cancelPendingPayment()
        runCurrent()
        coVerify(exactly = 1) { repo.cancelPendingSubscription() }
        assertTrue(vm.state.value.successMessage?.contains("dibatalkan") == true)
        assertNull(vm.state.value.pendingSub)
    }

    @Test fun `cancel failure shows error and keeps pending card`() = runTest(dispatcher) {
        coEvery { repo.agentSubscription() } returns Result.success(
            AgentSubscriptionDto(id = "sub-4", status = "pending"),
        )
        coEvery { repo.cancelPendingSubscription() } returns Result.failure(IOException("Gagal membatalkan"))
        val vm = viewModel()
        vm.retry()
        runCurrent()
        vm.cancelPendingPayment()
        runCurrent()
        assertNotNull(vm.state.value.error)
        assertEquals("sub-4", vm.state.value.pendingSub?.id)
    }
}
