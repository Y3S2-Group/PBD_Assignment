package utils

import data.model.Currency

fun formatAmount(currency: Currency, amount: Double): String {
    return "${currency.code} ${String.format("%,.2f", amount)}"
}
