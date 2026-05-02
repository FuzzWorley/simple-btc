package com.bitcoinportfolio.domain.model

enum class TimeRange(val days: Int, val label: String) {
    ONE_DAY(1, "1D"),
    ONE_WEEK(7, "1W"),
    ONE_MONTH(30, "1M"),
    THREE_MONTHS(90, "3M"),
    SIX_MONTHS(180, "6M"),
    ONE_YEAR(365, "1Y"),
    TWO_YEARS(730, "2Y"),
    THREE_YEARS(1095, "3Y"),
    FIVE_YEARS(1825, "5Y"),
    TEN_YEARS(3650, "10Y")
}
