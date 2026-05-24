package com.example.financeapp.ui.notifications

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(NotificationUiState())
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    init {
        _state.value = NotificationUiState(
            notifications = listOf(
                NotificationItem(
                    id = "1",
                    title = "Budget Alert",
                    description = "You reached 80% of your Dining budget.",
                    type = NotificationType.Warning,
                    icon = NotificationIcon.Warning,
                    timestampLabel = "2h ago",
                    isRead = false,
                    section = NotificationSection.Today,
                ),
                NotificationItem(
                    id = "2",
                    title = "Income Received",
                    description = "Freelance project payment ($500) credited.",
                    type = NotificationType.Success,
                    icon = NotificationIcon.Wallet,
                    timestampLabel = "5h ago",
                    isRead = false,
                    section = NotificationSection.Today,
                ),
                NotificationItem(
                    id = "3",
                    title = "Security",
                    description = "New login from Chrome on Mac.",
                    type = NotificationType.Info,
                    icon = NotificationIcon.Security,
                    timestampLabel = "22h ago",
                    isRead = true,
                    section = NotificationSection.Yesterday,
                ),
                NotificationItem(
                    id = "4",
                    title = "Expense Logged",
                    description = "Recurring subscription: Cloud Storage ($9.99).",
                    type = NotificationType.Info,
                    icon = NotificationIcon.Receipt,
                    timestampLabel = "1d ago",
                    isRead = true,
                    section = NotificationSection.Yesterday,
                ),
            )
        )
    }

    fun markAllAsRead() {
        _state.update { current ->
            current.copy(
                notifications = current.notifications.map { it.copy(isRead = true) }
            )
        }
    }
}

data class NotificationUiState(
    val notifications: List<NotificationItem> = emptyList(),
)

data class NotificationItem(
    val id: String,
    val title: String,
    val description: String,
    val type: NotificationType,
    val icon: NotificationIcon,
    val timestampLabel: String,
    val isRead: Boolean,
    val section: NotificationSection,
)

enum class NotificationType {
    Warning,
    Success,
    Info,
}

enum class NotificationIcon {
    Warning,
    Wallet,
    Security,
    Receipt,
}

enum class NotificationSection {
    Today,
    Yesterday,
}
