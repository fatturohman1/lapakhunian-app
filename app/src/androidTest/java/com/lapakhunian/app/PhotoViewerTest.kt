package com.lapakhunian.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.lapakhunian.app.core.designsystem.LapakHunianTheme
import com.lapakhunian.app.feature.detail.PhotoViewer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PhotoViewerTest {
    @get:Rule val compose = createComposeRule()

    private val visible = androidx.compose.runtime.mutableStateOf(true)

    private fun openViewer(onDismiss: () -> Unit = {}) {
        compose.setContent {
            LapakHunianTheme {
                if (visible.value) {
                    PhotoViewer(
                        images = listOf(null, null),
                        initialPage = 0,
                        contentDescription = "Foto properti uji",
                        onDismiss = {
                            visible.value = false
                            onDismiss()
                        },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun closeButtonDismissesViewer() {
        var dismissed = false
        openViewer { dismissed = true }
        compose.onNodeWithContentDescription("Tutup galeri").assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertTrue(dismissed)
        compose.onNodeWithContentDescription("Tutup galeri").assertDoesNotExist()
    }

    @Test
    fun tapAtNormalZoomStillDismisses() {
        var dismissed = false
        openViewer { dismissed = true }
        compose.onNodeWithTag("photo-viewer-pager").performClick()
        // onTap menunggu lewat timeout double-tap dulu sebelum fires
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun swipeChangesPageAtNormalZoom() {
        openViewer()
        compose.onNodeWithText("1/2").assertIsDisplayed()
        compose.onNodeWithTag("photo-viewer-pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("2/2").assertIsDisplayed()
    }

    @Test
    fun doubleTapZoomsAndLocksPagingUntilZoomReset() {
        openViewer()
        val pager = compose.onNodeWithTag("photo-viewer-pager")
        pager.performTouchInput { doubleClick() }
        compose.waitForIdle()
        // Saat zoom, geser horizontal = pan foto, bukan pindah halaman
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("1/2").assertIsDisplayed()
        // Double-tap lagi kembali ke 1x → paging aktif lagi
        pager.performTouchInput { doubleClick() }
        compose.waitForIdle()
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("2/2").assertIsDisplayed()
    }

    @Test
    fun pinchZoomLocksPaging() {
        openViewer()
        val pager = compose.onNodeWithTag("photo-viewer-pager")
        pager.performTouchInput {
            val y = center.y
            down(0, Offset(center.x - 40f, y))
            down(1, Offset(center.x + 40f, y))
            advanceEventTime(16)
            updatePointerTo(0, Offset(center.x - 80f, y))
            updatePointerTo(1, Offset(center.x + 80f, y))
            move()
            advanceEventTime(16)
            updatePointerTo(0, Offset(center.x - 160f, y))
            updatePointerTo(1, Offset(center.x + 160f, y))
            move()
            advanceEventTime(16)
            up(0)
            up(1)
        }
        compose.waitForIdle()
        pager.performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("1/2").assertIsDisplayed()
    }
}