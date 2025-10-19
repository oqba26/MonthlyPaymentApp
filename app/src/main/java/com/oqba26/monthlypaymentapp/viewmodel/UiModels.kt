package com.oqba26.monthlypaymentapp.viewmodel

data class PersonUiModel(
    val id: String,
    val name: String,
    val hasPaidThisMonth: Boolean
)

data class DashboardUiModel(
    val paidCount: Int = 0,
    val totalCount: Int = 0,
    val totalIncome: Double = 0.0,
    val progress: Float = 0f
)

enum class MonthStatus { PAID, AVAILABLE, FUTURE_YEAR, PAST_YEAR, FUTURE_MONTH }