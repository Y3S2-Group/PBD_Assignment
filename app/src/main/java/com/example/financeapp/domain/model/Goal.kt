package com.example.financeapp.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmount: Double,
    val currentSavings: Double,
    val deadlineTimestamp: Long,
    val createdAt: Long
)
