package com.circle.app.data.repo

import com.circle.app.data.AlertKind
import com.circle.app.data.CircleStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** F5: the Alerts dot, Mark all read, and notification switches deciding which alerts you get. */
class AlertsTest {
    private lateinit var repo: FakeCircleRepository

    @Before fun setUp() {
        CircleStore.reset()
        repo = FakeCircleRepository()
    }

    @Test fun `the dot shows until you mark all read`() = runTest {
        assertTrue(repo.hasUnreadAlerts().first())
        assertTrue(repo.alerts().first().all { it.unread })
        repo.markAllAlertsRead()
        assertFalse(repo.hasUnreadAlerts().first())
        assertTrue(repo.alerts().first().none { it.unread })
    }

    @Test fun `switching likes off hides like alerts`() = runTest {
        assertTrue(repo.alerts().first().any { it.kind == AlertKind.LIKED_YOUR_NOTE })
        repo.setNotificationPrefs(CircleStore.notifications.copy(likes = false))
        assertFalse(repo.alerts().first().any { it.kind == AlertKind.LIKED_YOUR_NOTE })
        assertEquals(5, CircleStore.notifications.onCount)
    }

    @Test fun `quiet hours are stored`() = runTest {
        repo.setNotificationPrefs(CircleStore.notifications.copy(quietHours = true, quietStart = 21 * 60, quietEnd = 8 * 60))
        val p = repo.notificationPrefs().first()
        assertTrue(p.quietHours)
        assertEquals(21 * 60, p.quietStart)
    }
}
