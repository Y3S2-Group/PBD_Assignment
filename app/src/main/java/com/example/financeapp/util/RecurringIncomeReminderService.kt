package com.example.financeapp.util

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.financeapp.domain.repository.IncomeRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Hilt-injected Service responsible for rescheduling all active recurring income alarms.
 *
 * It is started by [RecurringIncomeReceiver] after a device reboot, and by the
 * [com.example.financeapp.ui.income.IncomeViewModel] whenever recurring income templates change.
 *
 * Lifecycle: starts, does its work on a coroutine, then calls stopSelf().
 */
@AndroidEntryPoint
class RecurringIncomeReminderService : Service() {

    @Inject
    lateinit var incomeRepository: IncomeRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RESCHEDULE -> rescheduleAll(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun rescheduleAll(startId: Int) {
        serviceScope.launch {
            runCatching {
                val actives = incomeRepository.getActiveRecurringIncomes()
                actives.forEach { recurring ->
                    RecurringIncomeScheduler.schedule(applicationContext, recurring)
                }
            }
            stopSelf(startId)
        }
    }

    companion object {
        const val ACTION_RESCHEDULE = "com.example.financeapp.ACTION_RESCHEDULE_RECURRING"
    }
}
