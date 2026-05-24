package com.example.financeapp.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val memberSince: Long,
    val avatarId: String = "avatar_1",
)

