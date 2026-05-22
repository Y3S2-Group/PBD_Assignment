package data.model

enum class Currency(val code: String, val symbol: String) {
    LKR("LKR", "Rs"),
    USD("USD", "$"),
    EUR("EUR", "€"),
    GBP("GBP", "£")
}
