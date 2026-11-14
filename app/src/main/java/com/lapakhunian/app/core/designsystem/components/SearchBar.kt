package com.lapakhunian.app.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.lapakhunian.app.core.designsystem.AppIcons
import com.lapakhunian.app.core.designsystem.GreenGradient
import com.lapakhunian.app.core.designsystem.LineGray
import com.lapakhunian.app.core.designsystem.MutedGray
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PrimaryDark

/**
 * Search pill (preview M0): full radius, shadow lembut, trailing tombol filter bulat.
 * [onClick] != null → pill utuh bisa diklik (mode "dekoratif", mis. di Home → Explore).
 */
@Composable
fun LapakSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari rumah, apartemen, kota…",
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }
            .shadow(elevation = 8.dp, shape = CircleShape, ambientColor = MutedGray, spotColor = MutedGray)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .padding(start = 20.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhIcon(AppIcons.Search,
            contentDescription = null,
            tint = MutedGray,
            modifier = Modifier.size(21.dp),
        )
        Box(modifier = Modifier
            .weight(1f)
            .padding(horizontal = 10.dp)) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                enabled = enabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(GreenGradient, CircleShape)
                .clickable(onClick = onFilterClick),
            contentAlignment = Alignment.Center,
        ) {
            PhIcon(
                AppIcons.Tune,
                contentDescription = "Filter",
                tint = MaterialTheme.colorScheme.onPrimary,
                size = 20.dp,
            )
        }
    }
}

/**
 * Chip filter (Dijual/Disewa/Kategori/Kota/Amenities).
 * Chip nonaktif diberi border halus agar terpisah dari latar abu muda.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.background(GreenGradient, CircleShape)
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(BorderStroke(1.dp, LineGray), CircleShape)
                },
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MutedGray,
        )
    }
}

@Composable
fun StaticChip(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = 13.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = PrimaryDark,
        )
    }
}
