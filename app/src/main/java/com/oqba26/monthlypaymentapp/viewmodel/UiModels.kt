package com.oqba26.monthlypaymentapp.viewmodel

import android.content.Context
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person

data class PersonUiModel(
    val id: String,
    val name: String,
    val hasPaidThisMonth: Boolean,
    val displayOrder: Long?,
    val createdAt: Long?,
    val debtCount: Int = 0,
    val totalDebtAmount: Double = 0.0,
    val phoneNumber: String? = null,
    val unpaidMonthsNames: List<String> = emptyList()
)

data class PersonListUiState(
    val unpaidPersons: List<PersonUiModel> = emptyList(),
    val paidPersons: List<PersonUiModel> = emptyList(),
    val archivedPersons: List<PersonUiModel> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val bulkSmsQueue: List<PersonUiModel> = emptyList(),
    val currentBulkIndex: Int = -1,
    val selectedCardForBulk: String? = null,
    val contactSuggestions: List<ContactSuggestion> = emptyList()
)

data class ContactSuggestion(
    val personId: String,
    val personName: String,
    val matches: List<ContactMatch>
)

data class ContactMatch(
    val nameInContacts: String,
    val phoneNumber: String
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

enum class PersonListType {
    UNPAID,
    PAID
}

sealed class PersonScreenEvent {
    data object RefreshData : PersonScreenEvent()
    data class AddPerson(val name: String, val phoneNumber: String?, val context: Context) : PersonScreenEvent()
    data class UpdatePerson(val personId: String, val name: String, val phoneNumber: String?) : PersonScreenEvent()
    data class DeletePerson(val personId: String) : PersonScreenEvent()
    data class ArchivePerson(val personId: String) : PersonScreenEvent()
    data class RestorePerson(val personId: String) : PersonScreenEvent()

    data class MovePersonNew(
        val fromIndex: Int,
        val toIndex: Int,
        val listType: PersonListType
    ) : PersonScreenEvent()

    data class CommitReorder(
        val listType: PersonListType
    ) : PersonScreenEvent()

    data class AddQuickPayment(
        val personId: String,
        val amount: Double,
        val description: String
    ) : PersonScreenEvent()

    data class SelectPerson(val personId: String) : PersonScreenEvent()
    data class ChangeYear(val offset: Int) : PersonScreenEvent()
    data class AddPaymentForMonth(
        val personId: String,
        val month: Int,
        val year: Int,
        val amount: Double,
        val description: String
    ) : PersonScreenEvent()

    data class UpdatePayment(
        val payment: PaymentRecord,
        val newAmount: Double,
        val newDescription: String
    ) : PersonScreenEvent()

    data class DeletePayment(val payment: PaymentRecord) : PersonScreenEvent()
    data class AddBulkPayments(
        val personId: String,
        val months: List<Int>,
        val year: Int,
        val amount: Double
    ) : PersonScreenEvent()
    data class ExportYearlyReport(val year: Int, val context: Context) : PersonScreenEvent()

    data class ToggleSelection(val personId: String) : PersonScreenEvent()
    data class MoveSelected(val direction: Int, val listType: PersonListType) : PersonScreenEvent()
    data object ClearSelection : PersonScreenEvent()

    data class ConfirmContactSuggestion(val personId: String, val personName: String, val phoneNumber: String) : PersonScreenEvent()
    data class DismissContactSuggestion(val suggestion: ContactSuggestion) : PersonScreenEvent()
}

data class DashboardUiModel(
    val paidCount: Int = 0,
    val totalCount: Int = 0,
    val totalIncome: Double = 0.0,
    val progress: Float = 0f
)
