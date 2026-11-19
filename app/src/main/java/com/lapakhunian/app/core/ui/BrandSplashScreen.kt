package com.lapakhunian.app.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lapakhunian.app.R

/**
 * Splash brand dalam app (mengikuti mockup PNG): logo + wordmark di tengah,
 * bar loading beranimasi + tagline di bawah. Muncul hanya saat cold start,
 * menutup konten sebelum app siap (system splash Android 12+ hanya
 * mendukung ikon di lingkaran, jadi layout lengkap dibuat di sini).
 */
@Composable
fun BrandSplashScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-46).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_brand_logo),
                contentDescription = "Logo LapakHunian",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(150.dp)
                    .height(125.dp),
            )
            Image(
                painter = painterResource(R.drawable.ic_brand_wordmark),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .width(262.dp)
                    .height(32.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 108.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLoadingBar(Modifier.width(210.dp))
            Text(
                "Temukan hunian impianmu",
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
                color = Color(0xFF39434D),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** Bar loading ala mockup: pill hijau meluncur di track abu (rounded, berulang). */
@Composable
private fun BrandLoadingBar(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "brandLoading")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1250, easing = LinearEasing)),
        label = "brandProgress",
    )
    BoxWithConstraints(
        modifier = modifier
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(Color(0xFFD8DDE1)),
    ) {
        val fillWidth = maxWidth * 0.32f
        val travel = maxWidth + fillWidth
        Box(
            modifier = Modifier
                .offset(x = travel * progress - fillWidth)
                .width(fillWidth)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF14A94F)),
        )
    }
}
