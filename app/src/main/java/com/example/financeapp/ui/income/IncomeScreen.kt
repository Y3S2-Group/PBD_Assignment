package com.example.financeapp.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IncomeScreen() {
    val incomeEntries = incomeMockEntries()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { IncomeHeader() }
            item { SourceBreakdownChart() }
            item { IncomeHistoryList(entries = incomeEntries) }
        }

        FloatingActionButton(
            onClick = {},
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 96.dp)
                .size(64.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}

@Composable
private fun IncomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Vault",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Revenue Streams",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "This Month",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$5,270.50",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "+12.5% vs last month",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SourceBreakdownChart() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                BreakdownSegment(
                    fraction = 0.703f,
                    color = MaterialTheme.colorScheme.secondary,
                    label = "70%",
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                )
                BreakdownSegment(
                    fraction = 0.145f,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    label = "15%",
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                BreakdownSegment(
                    fraction = 0.084f,
                    color = MaterialTheme.colorScheme.primary,
                    label = "8%",
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
                BreakdownSegment(
                    fraction = 0.068f,
                    color = MaterialTheme.colorScheme.tertiary,
                    label = "7%",
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(label = "Salary", color = MaterialTheme.colorScheme.secondary)
                LegendItem(label = "Freelance", color = MaterialTheme.colorScheme.primaryContainer)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendItem(label = "AdSense", color = MaterialTheme.colorScheme.primary)
                LegendItem(label = "Crypto", color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

@Composable
private fun IncomeHistoryList(entries: List<IncomeEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Income",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(onClick = {}) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                IncomeListItem(entry = entry)
            }
        }
    }
}

@Composable
private fun IncomeListItem(entry: IncomeEntry) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = entry.tint.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = entry.tint.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = entry.title,
                        tint = entry.tint
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        entry.status?.let { status ->
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = status)
                        }
                    }
                    Text(
                        text = entry.secondaryAmount,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.primaryAmount,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = entry.dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: IncomeStatus) {
    Box(
        modifier = Modifier
            .background(
                color = status.color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = status.color.copy(alpha = 0.2f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = status.color
        )
    }
}

@Composable
private fun RowScope.BreakdownSegment(
    fraction: Float,
    color: Color,
    label: String,
    contentColor: Color,
    shape: RoundedCornerShape = RoundedCornerShape(0.dp)
) {
    Box(
        modifier = Modifier
            .weight(fraction)
            .fillMaxSize()
            .background(color = color, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Card(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = shape
                )
        ) {
            content()
        }
    }
}

private data class IncomeEntry(
    val title: String,
    val primaryAmount: String,
    val secondaryAmount: String,
    val dateLabel: String,
    val icon: ImageVector,
    val tint: Color,
    val status: IncomeStatus? = null
)

private data class IncomeStatus(val label: String, val color: Color)

@Composable
private fun incomeMockEntries(): List<IncomeEntry> {
    val secondary = MaterialTheme.colorScheme.secondary
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    return listOf(
        IncomeEntry(
            title = "Salary",
            secondaryAmount = "LKR 1,260,000",
            primaryAmount = "$4,200",
            dateLabel = "Today",
            icon = Icons.Outlined.Work,
            tint = secondary
        ),
        IncomeEntry(
            title = "Freelance Project",
            secondaryAmount = "LKR 150,000",
            primaryAmount = "$500 USD",
            dateLabel = "Yesterday",
            icon = Icons.Outlined.BusinessCenter,
            tint = primary,
            status = IncomeStatus("Paid", secondary)
        ),
        IncomeEntry(
            title = "Crypto Dividend",
            secondaryAmount = "$120.50 (LKR 36,150)",
            primaryAmount = "0.05 ETH",
            dateLabel = "Oct 24",
            icon = Icons.Outlined.CurrencyBitcoin,
            tint = tertiary
        ),
        IncomeEntry(
            title = "AdSense",
            secondaryAmount = "LKR 45,000",
            primaryAmount = "$150 USD",
            dateLabel = "Oct 22",
            icon = Icons.Outlined.Campaign,
            tint = primary
        ),
        IncomeEntry(
            title = "Freelance Logo Design",
            secondaryAmount = "LKR 90,000",
            primaryAmount = "$300 USD",
            dateLabel = "Oct 20",
            icon = Icons.Outlined.Palette,
            tint = tertiary,
            status = IncomeStatus("Pending", tertiary)
        )
    )
}




