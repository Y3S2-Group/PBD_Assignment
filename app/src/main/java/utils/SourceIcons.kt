package utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import data.model.IncomeSource

fun getSourceIcon(source: IncomeSource): ImageVector {
    return when (source) {
        IncomeSource.SALARY -> Icons.Default.Work
        IncomeSource.FREELANCE -> Icons.Default.Computer
        IncomeSource.ADSENSE -> Icons.Default.MonetizationOn
        IncomeSource.CRYPTO -> Icons.Default.Savings
    }
}
