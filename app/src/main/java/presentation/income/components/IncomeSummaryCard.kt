package presentation.income.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.model.Currency
import data.model.IncomeSource
import presentation.income.IncomeSummary
import utils.formatAmount

@Composable
fun IncomeSummaryCard(
    summary: IncomeSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Income This Month",
                style = MaterialTheme.typography.labelMedium
            )
            if (summary.totalByCurrency.isEmpty()) {
                Text(
                    text = "No income recorded",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                summary.totalByCurrency.forEach { (currency, total) ->
                    Text(
                        text = formatAmount(currency, total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            IncomeSource.values().forEach { source ->
                val amounts = summary.bySource[source]
                if (!amounts.isNullOrEmpty()) {
                    Text(
                        text = buildSourceLine(source, amounts),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun buildSourceLine(
    source: IncomeSource,
    amounts: Map<Currency, Double>
): String {
    val parts = amounts.map { (currency, amount) ->
        formatAmount(currency, amount)
    }
    return "${source.name}: ${parts.joinToString(" · ")}"
}
