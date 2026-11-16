package com.lapakhunian.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.lapakhunian.app.core.data.repository.DashboardRepository
import com.lapakhunian.app.core.data.repository.PropertyRepository
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.network.PropertyApi
import com.lapakhunian.app.feature.payments.PaymentViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private var currentSubscription = """{"id":"new-sub","status":"pending"}"""
    private var offline = false
    private val requests = mutableListOf<String>()
    private lateinit var client: HttpClient
    private lateinit var repository: DashboardRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val session = mockk<SessionManager>()
        coEvery { session.token() } returns "test-session"
        client = HttpClient(MockEngine(MockEngineConfig().apply {
            dispatcher = this@PaymentViewModelTest.dispatcher
            addHandler { request ->
            requests += request.url.encodedPath
            if (offline) throw IOException("offline")
            val data = when (request.url.encodedPath) {
                "/api/subscription" -> """{"isActive":true}"""
                "/api/agent/subscribe" -> currentSubscription
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
            respond("""{"success":true,"data":$data}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        })) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        repository = DashboardRepository(PropertyApi(client, "https://api.example.test"), session)
    }

    @After
    fun tearDown() {
        store.clear()
        client.close()
        Dispatchers.resetMain()
    }

    private fun viewModel(subscriptionId: String? = "new-sub"): PaymentViewModel = PaymentViewModel(
        SavedStateHandle(mapOf("paymentUrl" to "https://payments.example.test/snap", "subscriptionId" to subscriptionId)),
        repository,
        mockk<PropertyRepository>(),
    ).also { store.put("payment", it) }

    @Test
    fun `active account does not confirm a pending invoice`() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertFalse(vm.state.value.paid)
    }

    @Test
    fun `only the requested subscription can confirm payment`() = runTest(dispatcher) {
        currentSubscription = """{"id":"old-sub","status":"active"}"""
        val vm = viewModel()
        runCurrent()
        vm.checkNow()
        runCurrent()
        assertFalse(vm.state.value.paid)
    }

    @Test
    fun `matching active subscription confirms payment`() = runTest(dispatcher) {
        currentSubscription = """{"id":"new-sub","status":"active"}"""
        val vm = viewModel()
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertTrue(vm.state.value.paid)
    }

    @Test
    fun `pending then active confirms only after activation`() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        vm.checkNow()
        runCurrent()
        assertFalse(vm.state.value.paid)
        currentSubscription = """{"id":"new-sub","status":"active"}"""
        advanceTimeBy(3_000)
        runCurrent()
        assertTrue(vm.state.value.paid)
    }

    @Test
    fun `missing subscription and failed states never confirm payment`() = runTest(dispatcher) {
        val vm = viewModel()
        runCurrent()
        for (data in listOf("null", """{"id":"new-sub","status":"pending"}""", """{"id":"new-sub","status":"cancelled"}""")) {
            currentSubscription = data
            vm.checkNow()
            runCurrent()
            assertFalse(vm.state.value.paid)
        }
    }

    @Test
    fun `missing invoice identity does not check unrelated subscriptions`() = runTest(dispatcher) {
        val vm = viewModel(null)
        runCurrent()
        vm.checkNow()
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertFalse(vm.state.value.paid)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `network failure never confirms payment and retry can recover`() = runTest(dispatcher) {
        offline = true
        val vm = viewModel()
        runCurrent()
        vm.checkNow()
        runCurrent()
        assertFalse(vm.state.value.paid)
        offline = false
        currentSubscription = """{"id":"new-sub","status":"active"}"""
        vm.checkNow()
        runCurrent()
        assertTrue(vm.state.value.paid)
    }
}
