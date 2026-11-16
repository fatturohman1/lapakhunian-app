package com.lapakhunian.app

import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.network.dto.SubscribeResponseDto
import com.lapakhunian.app.core.network.dto.SubscriptionDto
import com.lapakhunian.app.feature.dashboard.AgentPlansViewModel
import io.mockk.coEvery
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
    }

    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun viewModel(response: SubscribeResponseDto): AgentPlansViewModel {
        coEvery { repo.subscribePlan(any(), any()) } returns Result.success(DashboardRepository.SubscribeOutcome(response))
        return AgentPlansViewModel(repo, session).also { store.put("plans", it) }
    }

    @Test fun `paid plan retains invoice identity alongside URL`() = runTest(dispatcher) {
        val vm = viewModel(SubscribeResponseDto(paymentRedirectUrl = "https://payments.example.test/snap", subscriptionId = "new-sub", status = "pending"))
        runCurrent()
        vm.subscribe("plan")
        runCurrent()
        assertEquals("new-sub", vm.state.value.paymentSubscriptionId)
        assertEquals("https://payments.example.test/snap", vm.state.value.paymentUrl)
        vm.clearPaymentUrl()
        assertNull(vm.state.value.paymentUrl)
        assertNull(vm.state.value.paymentSubscriptionId)
    }

    @Test fun `missing invoice identity does not navigate to unverified payment`() = runTest(dispatcher) {
        val vm = viewModel(SubscribeResponseDto(paymentRedirectUrl = "https://payments.example.test/snap", status = "pending"))
        runCurrent()
        vm.subscribe("plan")
        runCurrent()
        assertNull(vm.state.value.paymentUrl)
        assertNotNull(vm.state.value.error)
    }

    @Test fun `free activation keeps its existing success flow`() = runTest(dispatcher) {
        val vm = viewModel(SubscribeResponseDto(subscriptionId = "free-sub", status = "active"))
        runCurrent()
        vm.subscribe("free-plan")
        runCurrent()
        assertNotNull(vm.state.value.successMessage)
        assertNull(vm.state.value.paymentUrl)
    }
}
