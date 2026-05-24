package com.example.financeapp.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.financeapp.domain.repository.AnalyticsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalyticsComputeService : Service() {
    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            try {
                analyticsRepository.refreshDashboardSummary(System.currentTimeMillis())
            } finally {
                stopSelfResult(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun enqueue(context: Context) {
            val intent = Intent(context, AnalyticsComputeService::class.java)
            context.startService(intent)
        }
    }
}
