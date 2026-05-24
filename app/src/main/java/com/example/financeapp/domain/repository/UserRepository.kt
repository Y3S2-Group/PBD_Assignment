package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.UserProfile

interface UserRepository {
    suspend fun saveUserDetails(uid: String, name: String, memberSince: Long, avatarId: String = "avatar_1")
    suspend fun getUserDetails(uid: String): UserProfile?
    suspend fun updateAvatar(uid: String, avatarId: String)
}

