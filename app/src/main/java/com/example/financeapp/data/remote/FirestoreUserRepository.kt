package com.example.financeapp.data.remote

import com.example.financeapp.domain.model.UserProfile
import com.example.financeapp.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : UserRepository {

    override suspend fun saveUserDetails(uid: String, name: String, memberSince: Long) {
        val email = firebaseAuth.currentUser?.email ?: ""
        val payload = mapOf(
            "uid" to uid,
            "displayName" to name,
            "email" to email,
            "memberSince" to memberSince,
        )
        suspendCancellableCoroutine { continuation ->
            firestore.collection("users")
                .document(uid)
                .set(payload)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }

    override suspend fun getUserDetails(uid: String): UserProfile? =
        suspendCancellableCoroutine { continuation ->
            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists()) {
                        continuation.resume(null)
                        return@addOnSuccessListener
                    }
                    val displayName = snapshot.getString("displayName") ?: ""
                    val email = snapshot.getString("email") ?: ""
                    val memberSince = snapshot.getLong("memberSince") ?: 0L
                    continuation.resume(
                        UserProfile(
                            uid = snapshot.id,
                            displayName = displayName,
                            email = email,
                            memberSince = memberSince,
                        )
                    )
                }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
}

