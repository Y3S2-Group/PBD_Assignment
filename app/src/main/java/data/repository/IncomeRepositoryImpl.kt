package data.repository

import com.google.firebase.firestore.FirebaseFirestore
import data.model.Currency
import data.model.Income
import data.model.IncomeSource
import domain.repository.IncomeRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class IncomeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : IncomeRepository {
    override suspend fun addIncome(income: Income): Result<String> {
        return try {
            val docRef = firestore.collection("incomes").document()
            firestore.collection("incomes")
                .document(docRef.id)
                .set(income.copy(id = docRef.id))
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getIncomes(userId: String): Flow<List<Income>> = callbackFlow {
        val listener = firestore.collection("incomes")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val incomes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Income::class.java)
                } ?: emptyList()
                trySend(incomes)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getTotalIncomeByCurrency(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Map<Currency, Double> {
        return try {
            getIncomes(userId, startDate, endDate)
                .groupBy { it.currency }
                .mapValues { (_, incomes) -> incomes.sumOf { it.amount } }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun getIncomeBySource(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Map<IncomeSource, Map<Currency, Double>> {
        return try {
            getIncomes(userId, startDate, endDate)
                .groupBy { it.source }
                .mapValues { (_, incomes) ->
                    incomes.groupBy { it.currency }
                        .mapValues { (_, list) -> list.sumOf { it.amount } }
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun getIncomes(
        userId: String,
        startDate: Long,
        endDate: Long
    ): List<Income> {
        return try {
            val snapshot = firestore.collection("incomes")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.toObject(Income::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
