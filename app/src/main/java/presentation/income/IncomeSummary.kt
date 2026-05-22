package presentation.income

import data.model.Currency
import data.model.IncomeSource

data class IncomeSummary(
    val totalByCurrency: Map<Currency, Double> = emptyMap(),
    val bySource: Map<IncomeSource, Map<Currency, Double>> = emptyMap()
)
