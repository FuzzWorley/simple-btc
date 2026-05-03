package com.bitcoinportfolio.ui.portfolio.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bitcoinportfolio.domain.model.TimeRange

private val BitcoinOrange = Color(0xFFF7931A)

@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(TimeRange.entries.toList()) { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(range.label) },
                modifier = Modifier.padding(end = 8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BitcoinOrange,
                    selectedLabelColor = Color.Black,
                ),
            )
        }
    }
}
