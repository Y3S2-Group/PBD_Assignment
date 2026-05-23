package com.example.financeapp.di

import com.example.financeapp.data.repository.BudgetGoalRepositoryImpl
import com.example.financeapp.domain.repository.BudgetGoalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BudgetGoalModule {
    @Binds
    @Singleton
    abstract fun bindBudgetGoalRepository(impl: BudgetGoalRepositoryImpl): BudgetGoalRepository
}

