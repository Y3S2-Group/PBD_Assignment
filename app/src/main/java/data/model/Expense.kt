package data.model

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val userId: String = ""
)
