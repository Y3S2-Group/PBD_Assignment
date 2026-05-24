package com.example.financeapp.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val today = state.notifications.filter { it.section == NotificationSection.Today }
    val yesterday = state.notifications.filter { it.section == NotificationSection.Yesterday }
    val earlier = state.notifications.filter { it.section == NotificationSection.Earlier }
    val isEmpty = !state.isLoading && state.notifications.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Notifications") },
                navigationIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(28.dp)
                            .clickable { onNavigateBack() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                )
            )
        }
    ) { innerPadding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeaderActions(
                unreadCount = state.notifications.count { !it.isRead },
                onMarkAll = viewModel::markAllAsRead,
            )

            if (isEmpty) {
                EmptyNotificationsCard()
            } else {
                if (today.isNotEmpty()) {
                    NotificationSection(
                        title = "Today",
                        accentColor = MaterialTheme.colorScheme.secondary,
                        items = today,
                        onItemClick = viewModel::markAsRead,
                    )
                }

                if (yesterday.isNotEmpty()) {
                    NotificationSection(
                        title = "Yesterday",
                        accentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        items = yesterday,
                        onItemClick = viewModel::markAsRead,
                    )
                }

                if (earlier.isNotEmpty()) {
                    NotificationSection(
                        title = "Earlier",
                        accentColor = MaterialTheme.colorScheme.outline,
                        items = earlier,
                        onItemClick = viewModel::markAsRead,
                    )
                }
            }

            AiInsightCard(
                title = state.insightTitle,
                body = state.insightText,
            )
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderActions(unreadCount: Int, onMarkAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (unreadCount > 0) "$unreadCount unread" else "You're all caught up",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (unreadCount > 0) {
            TextButton(onClick = onMarkAll) {
                Text(
                    text = "Mark all as read",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyNotificationsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = "No notifications yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Add income, expenses, or budget categories to start receiving smart alerts.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Section ───────────────────────────────────────────────────────────────────

@Composable
private fun NotificationSection(
    title: String,
    accentColor: Color,
    items: List<NotificationItem>,
    onItemClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor
        )
        items.forEach { item ->
            NotificationCard(item = item, onClick = { onItemClick(item.id) })
        }
    }
}

// ── Card ──────────────────────────────────────────────────────────────────────

@Composable
private fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    val iconData = notificationIconData(item.type, item.icon)
    val cardAlpha = if (item.isRead) 0.6f else 1f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = if (item.isRead) 0.5f else 0.7f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (item.isRead) 0.5.dp else 1.dp,
                color = if (item.isRead)
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp),
            )
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(Color.Transparent)
                .clickable { onClick() },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconData.containerColor.copy(alpha = cardAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconData.icon,
                    contentDescription = null,
                    tint = iconData.iconTint.copy(alpha = cardAlpha),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = cardAlpha),
                    )
                    Text(
                        text = item.timestampLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha),
                )
            }
            // Unread dot
            if (!item.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

// ── AI Insight card ───────────────────────────────────────────────────────────

@Composable
private fun AiInsightCard(title: String, body: String) {
    val displayTitle = title.ifBlank { "Financial Insight" }
    val displayBody = body.ifBlank {
        "Log your income and expenses to unlock personalised financial insights and smart alerts."
    }

    val borderGradient = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderGradient, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.7f))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "View Analysis",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Icon mapping ──────────────────────────────────────────────────────────────

private data class NotificationIconData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color,
    val containerColor: Color,
)

@Composable
private fun notificationIconData(
    type: NotificationType,
    icon: NotificationIcon,
): NotificationIconData {
    return when (type) {
        NotificationType.Warning -> NotificationIconData(
            icon = when (icon) {
                NotificationIcon.Goal -> Icons.Rounded.EmojiEvents
                NotificationIcon.Trending -> Icons.Rounded.TrendingDown
                else -> Icons.Rounded.Error
            },
            iconTint = MaterialTheme.colorScheme.error,
            containerColor = MaterialTheme.colorScheme.errorContainer
        )

        NotificationType.Success -> NotificationIconData(
            icon = when (icon) {
                NotificationIcon.Goal -> Icons.Rounded.EmojiEvents
                else -> Icons.Rounded.AccountBalanceWallet
            },
            iconTint = MaterialTheme.colorScheme.secondary,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )

        NotificationType.Info -> {
            val resolvedIcon = when (icon) {
                NotificationIcon.Security -> Icons.Rounded.Security
                NotificationIcon.Receipt -> Icons.AutoMirrored.Rounded.ReceiptLong
                NotificationIcon.Wallet -> Icons.Rounded.AccountBalanceWallet
                NotificationIcon.Goal -> Icons.Rounded.EmojiEvents
                NotificationIcon.Trending -> Icons.Rounded.TrendingDown
                NotificationIcon.Warning -> Icons.Rounded.Error
            }
            NotificationIconData(
                icon = resolvedIcon,
                iconTint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }
}
