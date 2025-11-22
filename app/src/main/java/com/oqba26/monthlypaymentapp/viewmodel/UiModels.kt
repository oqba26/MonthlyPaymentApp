package com.oqba26.monthlypaymentapp.viewmodel

import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person

data class PersonUiModel(
    val id: String,
    val name: String,
    val hasPaidThisMonth: Boolean,
    val displayOrder: Long, // FIX: Changed from Int to Long
    val createdAt: Long
)

data class PersonListUiState(
    val unpaidPersons: List<PersonUiModel> = emptyList(),
    val paidPersons: List<PersonUiModel> = emptyList(),
    val archivedPersons: List<PersonUiModel> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
)

data class PersonDetailUiState(
    val person: Person? = null,
    val selectedYear: Int = 0,
    val monthStates: List<MonthUiModel> = emptyList()
)

data class MonthUiModel(
    val month: Int,
    val payment: PaymentRecord?,
    val status: MonthStatus
)

enum class MonthStatus {
    PAID,
    AVAILABLE,
    FUTURE_MONTH,
    PAST_YEAR,
    FUTURE_YEAR
}

data class DashboardUiModel(
    val paidCount: Int = 0,
    val totalCount: Int = 0,
    val totalIncome: Double = 0.0,
    val progress: Float = 0f
)
