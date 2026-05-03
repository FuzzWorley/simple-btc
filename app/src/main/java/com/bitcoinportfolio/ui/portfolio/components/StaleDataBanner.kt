package com.bitcoinportfolio.ui.portfolio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val StaleAmber = Color(0xFFFFC107)

@Composable
fun StaleDataBanner(staleAsOf: Long, modifier: Modifier = Modifier) {
    val formatted = remember(staleAsOf) {
        SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault()).format(Date(staleAsOf))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StaleAmber.copy(alpha = 0.12f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = StaleAmber,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Price as of $formatted",
            style = MaterialTheme.typography.labelMedium,
            color = StaleAmber,
        )
    }
}
