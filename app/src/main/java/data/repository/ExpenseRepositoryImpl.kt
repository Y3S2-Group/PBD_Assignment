package data.repository

import com.google.firebase.firestore.FirebaseFirestore
import data.model.Expense
import data.model.ExpenseCategory
import domain.repository.ExpenseRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ExpenseRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExpenseRepository {
    override suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val docRef = firestore.collection("expenses").document()
            firestore.collection("expenses")
                .document(docRef.id)
                .set(expense.copy(id = docRef.id))
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getExpenses(userId: String): Flow<List<Expense>> = kotlinx.coroutines.flow.callbackFlow {
        val listener = firestore.collection("expenses")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)
                } ?: emptyList()
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getTotalExpenses(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Double {
        return try {
            val snapshot = firestore.collection("expenses")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            snapshot.documents.sumOf { doc ->
                doc.getDouble("amount") ?: 0.0
            }
        } catch (e: Exception) {
            0.0
        }
    }

    override suspend fun getExpensesByCategory(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Map<ExpenseCategory, Double> {
        return try {
            val snapshot = firestore.collection("expenses")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            snapshot.documents
                .mapNotNull { it.toObject(Expense::class.java) }
                .groupBy { it.category }
                .mapValues { (_, expenses) ->
                    expenses.sumOf { it.amount }
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun getExpenses(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<Expense> {
        return try {
            val snapshot = firestore.collection("expenses")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Expense::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
