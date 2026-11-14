package com.lapakhunian.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lapakhunian.app.core.designsystem.AmberBg
import com.lapakhunian.app.core.designsystem.AmberWarning
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.BlueBg
import com.lapakhunian.app.core.designsystem.BlueInfo
import com.lapakhunian.app.core.designsystem.ErrorRed
import com.lapakhunian.app.core.designsystem.GrayBg
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.OkBg
import com.lapakhunian.app.core.designsystem.OkGreen
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PhosphorWeight
import com.lapakhunian.app.core.designsystem.RedBg
import com.lapakhunian.app.core.domain.model.ListingStatus

private data class BadgeStyle(val bg: Color, val fg: Color, val icon: String?)

@Composable
fun StatusBadge(status: ListingStatus, modifier: Modifier = Modifier) {
    val style = when (status) {
        ListingStatus.APPROVED -> BadgeStyle(OkBg, OkGreen, AppIcons.CheckCircle)
        ListingStatus.PENDING, ListingStatus.PENDING_PAYMENT ->
            BadgeStyle(AmberBg, AmberWarning, AppIcons.Calendar)
        ListingStatus.REJECTED -> BadgeStyle(RedBg, ErrorRed, AppIcons.Close)
        ListingStatus.EXPIRED -> BadgeStyle(GrayBg, MutedGray, AppIcons.Calendar)
        ListingStatus.SOLD -> BadgeStyle(BlueBg, BlueInfo, AppIcons.Sell)
    }
    Row(
        modifier = modifier
            .background(style.bg, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        style.icon?.let {
            PhIcon(
                name = it,
                tint = style.fg,
                size = 12.dp,
                weight = PhosphorWeight.REGULAR,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Text(
            text = status.label.uppercase(),
            color = style.fg,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.W800,
            letterSpacing = 0.5.sp,
        )
    }
}
