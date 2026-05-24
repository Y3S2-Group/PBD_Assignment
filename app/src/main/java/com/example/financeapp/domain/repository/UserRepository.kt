package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.UserProfile

interface UserRepository {
    suspend fun saveUserDetails(uid: String, name: String, memberSince: Long)
    suspend fun getUserDetails(uid: String): UserProfile?
}

