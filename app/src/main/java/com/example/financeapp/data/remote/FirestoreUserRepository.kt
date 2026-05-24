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

    override suspend fun saveUserDetails(uid: String, name: String, memberSince: Long, avatarId: String) {
        val email = firebaseAuth.currentUser?.email ?: ""
        val payload = mapOf(
            "uid" to uid,
            "displayName" to name,
            "email" to email,
            "memberSince" to memberSince,
            "avatarId" to avatarId,
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
                        val authUser = firebaseAuth.currentUser
                        if (authUser == null) {
                            continuation.resume(null)
                            return@addOnSuccessListener
                        }
                        val email = authUser.email.orEmpty()
                        val displayName = authUser.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: email.substringBefore("@").ifBlank { "User" }
                        val memberSince = authUser.metadata?.creationTimestamp
                            ?: System.currentTimeMillis()
                        val payload = mapOf(
                            "uid" to uid,
                            "displayName" to displayName,
                            "email" to email,
                            "memberSince" to memberSince,
                            "avatarId" to "avatar_1",
                        )
                        firestore.collection("users")
                            .document(uid)
                            .set(payload)
                            .addOnSuccessListener {
                                continuation.resume(
                                    UserProfile(
                                        uid = uid,
                                        displayName = displayName,
                                        email = email,
                                        memberSince = memberSince,
                                        avatarId = "avatar_1",
                                    )
                                )
                            }
                            .addOnFailureListener { continuation.resumeWithException(it) }
                        return@addOnSuccessListener
                    }
                    val displayName = snapshot.getString("displayName") ?: ""
                    val email = snapshot.getString("email") ?: ""
                    val memberSince = snapshot.getLong("memberSince") ?: 0L
                    val avatarId = snapshot.getString("avatarId") ?: "avatar_1"
                    continuation.resume(
                        UserProfile(
                            uid = snapshot.id,
                            displayName = displayName,
                            email = email,
                            memberSince = memberSince,
                            avatarId = avatarId,
                        )
                    )
                }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }

    override suspend fun updateAvatar(uid: String, avatarId: String) {
        suspendCancellableCoroutine { continuation ->
            firestore.collection("users")
                .document(uid)
                .update("avatarId", avatarId)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
    }
}
