package com.example.financeapp.data.remote

import com.example.financeapp.domain.model.BudgetCategory
import com.example.financeapp.domain.model.Goal
import com.example.financeapp.domain.model.SavingsDeposit
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreGoalRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    private fun goalsRef(uid: String) =
        firestore.collection("users").document(uid).collection("goals")

    private fun categoriesRef(uid: String) =
        firestore.collection("users").document(uid).collection("budget_categories")

    private fun depositsRef(uid: String) =
        firestore.collection("users").document(uid).collection("savings_deposits")

    // ── Goals ─────────────────────────────────────────────────────────────────

    suspend fun saveGoal(uid: String, goal: Goal): Unit =
        suspendCancellableCoroutine { cont ->
            goalsRef(uid).document(goal.id)
                .set(goal.toFirestoreMap())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun updateGoalSavings(uid: String, goalId: String, newSavings: Double): Unit =
        suspendCancellableCoroutine { cont ->
            goalsRef(uid).document(goalId)
                .update("currentSavings", newSavings)
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun getAllGoals(uid: String): List<Goal> =
        suspendCancellableCoroutine { cont ->
            goalsRef(uid).get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.documents.mapNotNull { it.toGoal() })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    // ── Budget categories ──────────────────────────────────────────────────────

    suspend fun saveBudgetCategory(uid: String, category: BudgetCategory): Unit =
        suspendCancellableCoroutine { cont ->
            categoriesRef(uid).document(category.id)
                .set(category.toFirestoreMap())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun getAllBudgetCategories(uid: String): List<BudgetCategory> =
        suspendCancellableCoroutine { cont ->
            categoriesRef(uid).get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.documents.mapNotNull { it.toBudgetCategory() })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    // ── Savings deposits ───────────────────────────────────────────────────────

    suspend fun saveDeposit(uid: String, deposit: SavingsDeposit): Unit =
        suspendCancellableCoroutine { cont ->
            depositsRef(uid).document(deposit.id)
                .set(deposit.toFirestoreMap())
                .addOnSuccessListener { cont.resume(Unit) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

    suspend fun getAllDeposits(uid: String): List<SavingsDeposit> =
        suspendCancellableCoroutine { cont ->
            depositsRef(uid).get()
                .addOnSuccessListener { snapshot ->
                    cont.resume(snapshot.documents.mapNotNull { it.toSavingsDeposit() })
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}

// ── Mapping helpers ────────────────────────────────────────────────────────────

private fun Goal.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "targetAmount" to targetAmount,
    "currentSavings" to currentSavings,
    "currency" to currency,
    "deadlineTimestamp" to deadlineTimestamp,
    "createdAt" to createdAt,
)

private fun DocumentSnapshot.toGoal(): Goal? {
    val name = getString("name") ?: return null
    return try {
        Goal(
            id = id,
            name = name,
            targetAmount = getDouble("targetAmount") ?: 0.0,
            currentSavings = getDouble("currentSavings") ?: 0.0,
            currency = getString("currency") ?: "LKR",
            deadlineTimestamp = getLong("deadlineTimestamp") ?: 0L,
            createdAt = getLong("createdAt") ?: 0L,
        )
    } catch (e: Exception) {
        null
    }
}

private fun BudgetCategory.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "categoryName" to categoryName,
    "allocatedAmount" to allocatedAmount,
    "monthYear" to monthYear,
)

private fun DocumentSnapshot.toBudgetCategory(): BudgetCategory? {
    val categoryName = getString("categoryName") ?: return null
    val monthYear = getString("monthYear") ?: return null
    return try {
        BudgetCategory(
            id = id,
            categoryName = categoryName,
            allocatedAmount = getDouble("allocatedAmount") ?: 0.0,
            monthYear = monthYear,
        )
    } catch (e: Exception) {
        null
    }
}

private fun SavingsDeposit.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "goalId" to goalId,
    "amount" to amount,
    "timestamp" to timestamp,
)

private fun DocumentSnapshot.toSavingsDeposit(): SavingsDeposit? {
    val goalId = getString("goalId") ?: return null
    return try {
        SavingsDeposit(
            id = id,
            goalId = goalId,
            amount = getDouble("amount") ?: 0.0,
            timestamp = getLong("timestamp") ?: 0L,
        )
    } catch (e: Exception) {
        null
    }
}
