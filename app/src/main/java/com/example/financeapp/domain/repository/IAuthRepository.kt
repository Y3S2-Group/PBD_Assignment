package com.example.financeapp.domain.repository

import com.example.financeapp.domain.model.User
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    val currentUser: Flow<User?>
    fun isSignedIn(): Boolean
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, displayName: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signOut()
}
