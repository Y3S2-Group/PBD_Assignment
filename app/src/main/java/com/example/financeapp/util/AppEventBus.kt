package com.example.financeapp.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-wide event bus for cross-ViewModel data-change notifications.
 *
 * A ViewModel that writes to the database emits the relevant [DataChangeEvent].
 * Any ViewModel that displays data derived from that table collects these events
 * and re-runs its refresh logic, keeping all screens in sync without converting
 * every DAO to a reactive Flow.
 *
 * Usage:
 *   // Writer ViewModel
 *   eventBus.send(DataChangeEvent.INCOME)
 *
 *   // Reader ViewModel (in init)
 *   viewModelScope.launch {
 *       eventBus.events.collect { refresh() }
 *   }
 */
@Singleton
class AppEventBus @Inject constructor() {

    private val _events = MutableSharedFlow<DataChangeEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<DataChangeEvent> = _events.asSharedFlow()

    fun send(event: DataChangeEvent) {
        _events.tryEmit(event)
    }
}

enum class DataChangeEvent {
    /** Income table was inserted, updated, or deleted. */
    INCOME,

    /** Expense table was inserted. */
    EXPENSE,

    /** Budget categories or savings goal were created / updated. */
    BUDGET_GOAL,
}
