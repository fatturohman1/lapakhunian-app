package com.lapakhunian.app.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.PhIcon

private const val MAX_ZOOM = 4f
private const val DOUBLE_TAP_ZOOM = 2.5f

/** Galeri layar penuh: pinch/double-tap zoom + pan, geser halaman saat 1x, tombol tutup eksplisit. */
@Composable
internal fun PhotoViewer(
    images: List<String?>,
    initialPage: Int,
    contentDescription: String?,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { images.size }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offset = Offset.Zero
    }

    // foundation 1.7 belum punya canPan: transformable hanya dipasang saat zoom,
    // supaya drag satu jari di 1x tetap dikonsumsi pager (geser halaman)
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, MAX_ZOOM)
        scale = newScale
        offset = if (newScale <= 1f) {
            Offset.Zero
        } else {
            val maxX = (newScale - 1f) * viewportSize.width / 2f
            val maxY = (newScale - 1f) * viewportSize.height / 2f
            Offset(
                (offset.x + panChange.x).coerceIn(-maxX, maxX),
                (offset.y + panChange.y).coerceIn(-maxY, maxY),
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { viewportSize = it },
            contentAlignment = Alignment.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = scale == 1f,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("photo-viewer-pager"),
            ) { page ->
                val image = images[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                        .then(
                            if (scale > 1f) {
                                Modifier.transformable(transformState)
                            } else {
                                Modifier.pointerInput(page) {
                                    awaitEachGesture {
                                        awaitFirstDown(requireUnconsumed = false)
                                        var previousDistance = 0f
                                        do {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            if (pressed.size >= 2) {
                                                val distance = (pressed[0].position - pressed[1].position).getDistance()
                                                if (previousDistance > 0f && distance > previousDistance) {
                                                    scale = (scale * distance / previousDistance).coerceIn(1f, MAX_ZOOM)
                                                    event.changes.forEach { it.consume() }
                                                }
                                                previousDistance = distance
                                            } else {
                                                previousDistance = 0f
                                            }
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                            },
                        )
                        .pointerInput(page) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = DOUBLE_TAP_ZOOM
                                    }
                                },
                                onTap = { if (scale == 1f) onDismiss() },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (image != null) {
                        AsyncImage(
                            model = image,
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(Color.Black))
                    }
                }
            }
            Text(
                text = "${pagerState.currentPage + 1}/${images.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .background(Color(0x8C0D141E), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color(0x8C0D141E)),
            ) {
                PhIcon(
                    name = AppIcons.Close,
                    tint = Color.White,
                    size = 22.dp,
                    contentDescription = "Tutup galeri",
                )
            }
        }
    }
}