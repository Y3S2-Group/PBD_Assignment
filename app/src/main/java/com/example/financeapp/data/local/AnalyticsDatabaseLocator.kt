package com.example.financeapp.data.local

import android.content.Context
import androidx.room.Room

object AnalyticsDatabaseLocator {
    @Volatile
    private var instance: IncomeDatabase? = null

    fun get(context: Context): IncomeDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                IncomeDatabase::class.java,
                "income.db"
            )
                .fallbackToDestructiveMigration(false)
                .build()
                .also { instance = it }
        }
    }
}
