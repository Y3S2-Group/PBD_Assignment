package com.example.financeapp.data.remote

import com.example.financeapp.domain.model.User
import com.example.financeapp.domain.repository.IAuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : IAuthRepository {

    override val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.toDomain())
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    override fun isSignedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun signIn(email: String, password: String): Result<User> =
        suspendCancellableCoroutine { continuation ->
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user?.toDomain()
                    if (user != null) {
                        continuation.resume(Result.success(user))
                    } else {
                        continuation.resume(Result.failure(Exception("Authentication failed")))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<User> = suspendCancellableCoroutine { continuation ->
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user
                if (firebaseUser == null) {
                    continuation.resume(Result.failure(Exception("Registration failed")))
                    return@addOnSuccessListener
                }
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                firebaseUser.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        continuation.resume(
                            Result.success(firebaseUser.toDomain().copy(displayName = displayName))
                        )
                    }
                    .addOnFailureListener {
                        // Profile update failed but account was created – still succeed
                        continuation.resume(Result.success(firebaseUser.toDomain()))
                    }
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toDomain() = User(
        uid = uid,
        email = email ?: "",
        displayName = displayName ?: "",
    )
}
