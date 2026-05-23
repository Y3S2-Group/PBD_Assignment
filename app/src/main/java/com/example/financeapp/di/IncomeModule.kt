package com.example.financeapp.di

import android.content.Context
import androidx.room.Room
import com.example.financeapp.data.local.IncomeDao
import com.example.financeapp.data.local.IncomeDatabase
import com.example.financeapp.data.local.ExpenseDao
import com.example.financeapp.data.local.BudgetDao
import com.example.financeapp.data.local.GoalDao
import com.example.financeapp.data.repository.IncomeRepositoryImpl
import com.example.financeapp.domain.repository.IncomeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class IncomeModule {
    @Binds
    @Singleton
    abstract fun bindIncomeRepository(impl: IncomeRepositoryImpl): IncomeRepository

    companion object {
        @Provides
        @Singleton
        fun provideIncomeDatabase(
            @ApplicationContext context: Context
        ): IncomeDatabase {
            return Room.databaseBuilder(
                context,
                IncomeDatabase::class.java,
                "income.db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        @Provides
        fun provideIncomeDao(database: IncomeDatabase): IncomeDao = database.incomeDao()

        @Provides
        fun provideExpenseDao(database: IncomeDatabase): ExpenseDao = database.expenseDao()

        @Provides
        fun provideGoalDao(database: IncomeDatabase): GoalDao = database.goalDao()

        @Provides
        fun provideBudgetDao(database: IncomeDatabase): BudgetDao = database.budgetDao()
    }
}
