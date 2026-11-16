package com.lapakhunian.app

import android.content.Context
import com.lapakhunian.app.core.ui.UpdateCheckPolicy
import com.lapakhunian.app.core.util.UpdateInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class UpdateCheckPolicyTest {
    private val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("update-test", Context.MODE_PRIVATE).also { it.edit().clear().commit() }

    @Test fun `offline failure permits retry after a minute rather than a day`() = runTest {
        var now = 1_000_000L
        var requests = 0
        val policy = UpdateCheckPolicy(prefs, { now }) {
            requests++
            if (requests == 1) error("offline") else UpdateInfo("1.0.0-beta.6", "")
        }
        assertNull(policy.checkIfDue())
        assertNull(policy.checkIfDue())
        assertEquals(1, requests)
        now += 60_001
        assertEquals("1.0.0-beta.6", policy.checkIfDue()?.version)
    }

    @Test fun `a successful check suppresses requests for 24 hours even without an update`() = runTest {
        var now = 1_000_000L
        var requests = 0
        val policy = UpdateCheckPolicy(prefs, { now }) { requests++; null }
        policy.checkIfDue()
        now += 23 * 60 * 60 * 1_000L
        policy.checkIfDue()
        assertEquals(1, requests)
        now += 60 * 60 * 1_000L
        policy.checkIfDue()
        assertEquals(2, requests)
    }

    @Test fun `concurrent checks share the in-flight request`() = runTest {
        var requests = 0
        val gate = CompletableDeferred<Unit>()
        val policy = UpdateCheckPolicy(prefs, { 1_000_000L }) { requests++; gate.await(); UpdateInfo("new", "") }
        val first = async { policy.checkIfDue() }
        runCurrent()
        assertNull(policy.checkIfDue())
        gate.complete(Unit)
        assertEquals("new", first.await()?.version)
        assertEquals(1, requests)
    }

    @Test fun `cancellation is not recorded as a successful check`() = runTest {
        var now = 1_000_000L
        var cancel = true
        val policy = UpdateCheckPolicy(prefs, { now }) {
            if (cancel) throw CancellationException("background") else UpdateInfo("new", "")
        }
        try { policy.checkIfDue(); fail("Cancellation should propagate") } catch (_: CancellationException) { }
        cancel = false
        now += 60_001
        assertEquals("new", policy.checkIfDue()?.version)
    }
}
