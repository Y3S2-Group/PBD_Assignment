package utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import data.model.ExpenseCategory

fun getCategoryIcon(category: ExpenseCategory): ImageVector {
    return when (category) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.TRANSPORT -> Icons.Default.DirectionsCar
        ExpenseCategory.UTILITIES -> Icons.Default.Bolt
        ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
        ExpenseCategory.HEALTH -> Icons.Default.HealthAndSafety
        ExpenseCategory.RENT -> Icons.Default.Home
        ExpenseCategory.OTHER -> Icons.Default.MoreHoriz
    }
}
