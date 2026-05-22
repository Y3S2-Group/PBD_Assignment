package data.model

data class Income(
    val id: String = "",
    val amount: Double = 0.0,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val userId: String = "",
    val source: IncomeSource = IncomeSource.SALARY,
    val currency: Currency = Currency.LKR
)

enum class IncomeSource {
    SALARY,
    FREELANCE,
    ADSENSE,
    CRYPTO
}
