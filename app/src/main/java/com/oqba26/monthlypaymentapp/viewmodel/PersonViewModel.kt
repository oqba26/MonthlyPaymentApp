package com.oqba26.monthlypaymentapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.monthlypaymentapp.core.manager.ExportManager
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.utils.formatTimestampToPersianDateTime
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiDay
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiMonth
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear
import com.oqba26.monthlypaymentapp.utils.getPersianMonthName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val localPersonRepository: LocalPersonRepository,
    private val settingsRepository: SettingsRepository,
    private val exportManager: ExportManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedPersonId = MutableStateFlow<String?>(null)
    private val _selectedYear = MutableStateFlow(getCurrentShamsiYear())

    private val _dashboardData = MutableStateFlow(DashboardUiModel())
    val dashboardData: StateFlow<DashboardUiModel> = _dashboardData.asStateFlow()

    val defaultPaymentAmountFlow = settingsRepository.defaultPaymentAmountFlow
    val cardNumbersFlow = settingsRepository.cardNumbersFlow

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    private val _infoMessage = MutableSharedFlow<String>()
    val infoMessage = _infoMessage.asSharedFlow()

    private val _uiState = MutableStateFlow(PersonListUiState())
    val uiState: StateFlow<PersonListUiState> = _uiState.asStateFlow()

    private val _showAddPersonDialog = MutableStateFlow(false)
    val showAddPersonDialog: StateFlow<Boolean> = _showAddPersonDialog.asStateFlow()

    private val _personForPaymentDialog = MutableStateFlow<PersonUiModel?>(null)
    val personForPaymentDialog: StateFlow<PersonUiModel?> = _personForPaymentDialog.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    val personDetailState: StateFlow<PersonDetailUiState> = _selectedPersonId
        .flatMapLatest { personId ->
            if (personId == null) {
                flowOf(PersonDetailUiState())
            } else {
                combine(
                    localPersonRepository.getAllPersonsFlow(),
                    localPersonRepository.getAllPaymentsFlow(),
                    _selectedYear
                ) { persons, allPayments, year ->
                    val person = persons.find { it.id == personId }
                    val paymentsForPersonInYear = allPayments.filter {
                        it.personId == personId && it.shamsiYear == year
                    }
                    val currentYear = getCurrentShamsiYear()
                    val currentMonth = getCurrentShamsiMonth()
                    val currentDay = getCurrentShamsiDay()
                    val monthStates = (1..12).map { month ->
                        val payment = paymentsForPersonInYear.find { it.shamsiMonth == month }
                        val status = when {
                            year < currentYear -> MonthStatus.PAST_YEAR
                            year > currentYear -> MonthStatus.FUTURE_YEAR
                            month > currentMonth || (month == currentMonth && currentDay < 20) -> MonthStatus.FUTURE_MONTH
                            payment != null -> MonthStatus.PAID
                            else -> MonthStatus.AVAILABLE
                        }
                        MonthUiModel(month, payment, status)
                    }
                    PersonDetailUiState(person, year, monthStates)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())

    init {
        // ۱. منطق همگام‌سازی شبکه به دیتابیس محلی
        viewModelScope.launch {
            settingsRepository.authTokenFlow.collectLatest { token ->
                if (token != null) {
                    networkRepository.observeRealtimeChanges()
                    networkRepository.refresh()

                    combine(
                        networkRepository.getPersonsFlow(),
                        networkRepository.getPaymentsFlow()
                    ) { persons, payments ->
                        if (persons.isNotEmpty()) {
                            localPersonRepository.syncAll(persons, payments)
                            true
                        } else false
                    }.collect { isUpdated ->
                        if (isUpdated) {
                            _toastMessage.emit("اطلاعات با سرور همگام‌سازی شد")
                        }
                    }
                }
            }
        }

        // ۲. منطق نمایش UI
        viewModelScope.launch {
            combine(
                localPersonRepository.getAllPersonsFlow(),
                localPersonRepository.getAllPaymentsFlow(),
                _searchQuery
            ) { persons, allPayments, query ->
                val currentMonth = getCurrentShamsiMonth()
                val currentYear = getCurrentShamsiYear()
                val currentDay = getCurrentShamsiDay()

                val targetMonth = if (currentDay >= 20) currentMonth else if (currentMonth == 1) 12 else currentMonth - 1
                val targetYear = if (currentDay < 20 && currentMonth == 1) currentYear - 1 else currentYear

                val relevantPayments = allPayments.filter {
                    it.shamsiMonth == targetMonth && it.shamsiYear == targetYear
                }

                val activePersons = persons
                    .filter { !it.isArchived }
                    .sortedWith(compareBy({ it.displayOrder }, { it.createdAt }))

                val uiModels = activePersons.map { person ->
                    val hasPaid = relevantPayments.any { it.personId == person.id }
                    val debtEndMonth = if (currentDay >= 20) currentMonth else currentMonth - 1
                    val unpaidMonthsInCurrentYear = (1..debtEndMonth).filter { m ->
                        allPayments.none { it.personId == person.id && it.shamsiYear == currentYear && it.shamsiMonth == m }
                    }
                    
                    PersonUiModel(
                        id = person.id,
                        name = person.name,
                        hasPaidThisMonth = hasPaid,
                        displayOrder = person.displayOrder,
                        createdAt = person.createdAt,
                        debtCount = unpaidMonthsInCurrentYear.size,
                        totalDebtAmount = unpaidMonthsInCurrentYear.size * (settingsRepository.defaultPaymentAmountFlow.first()),
                        phoneNumber = person.phoneNumber,
                        unpaidMonthsNames = unpaidMonthsInCurrentYear.map { getPersianMonthName(it) }
                    )
                }

                updateDashboard(uiModels.size, relevantPayments)

                val (paidModels, unpaidModels) = uiModels.partition { it.hasPaidThisMonth }

                val filteredPaid = if (query.isBlank()) paidModels else paidModels.filter { it.name.contains(query, ignoreCase = true) }
                val filteredUnpaid = if (query.isBlank()) unpaidModels else unpaidModels.filter { it.name.contains(query, ignoreCase = true) }

                val archivedPersons = persons.filter { it.isArchived }.map {
                    PersonUiModel(it.id, it.name, false, it.displayOrder, it.createdAt)
                }

                PersonListUiState(
                    unpaidPersons = filteredUnpaid,
                    paidPersons = filteredPaid,
                    archivedPersons = archivedPersons,
                    payments = allPayments
                )
            }.collect { newUiState ->
                _uiState.value = newUiState
            }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onAddPersonClicked() { _showAddPersonDialog.value = true }
    fun onDismissAddPersonDialog() { _showAddPersonDialog.value = false }
    fun onQuickPayClicked(person: PersonUiModel) { _personForPaymentDialog.value = person }
    fun onDismissPaymentDialog() { _personForPaymentDialog.value = null }

    fun onEvent(event: PersonScreenEvent) {
        viewModelScope.launch {
            when (event) {
                is PersonScreenEvent.MovePersonNew -> {
                    val fromIndex = event.fromIndex
                    val toIndex = event.toIndex
                    val list = if (event.listType == PersonListType.UNPAID) _uiState.value.unpaidPersons else _uiState.value.paidPersons
                    if (fromIndex !in list.indices || toIndex !in list.indices) return@launch

                    val reorderedList = list.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
                    _uiState.update {
                        if (event.listType == PersonListType.UNPAID) it.copy(unpaidPersons = reorderedList)
                        else it.copy(paidPersons = reorderedList)
                    }
                }

                is PersonScreenEvent.AddPerson -> {
                    if (event.name.isNotBlank()) {
                        val person = Person(name = event.name, phoneNumber = event.phoneNumber, displayOrder = 0)
                        val statusCode = networkRepository.addPerson(person)
                        if (statusCode == 409) _toastMessage.emit("خطا: این نام از قبل وجود دارد.")
                        else if (statusCode != 200 && statusCode != 201) _toastMessage.emit("خطا در افزودن شخص.")
                        else networkRepository.refresh()
                    }
                }

                is PersonScreenEvent.AddQuickPayment -> {
                    val currentDay = getCurrentShamsiDay()
                    val currentMonth = getCurrentShamsiMonth()
                    val currentYear = getCurrentShamsiYear()
                    val targetMonth = if (currentDay >= 20) currentMonth else if (currentMonth == 1) 12 else currentMonth - 1
                    val targetYear = if (currentDay < 20 && currentMonth == 1) currentYear - 1 else currentYear

                    val record = createPaymentRecord(event.personId, targetMonth, targetYear, event.amount, event.description)
                    if (networkRepository.addPayment(record)) networkRepository.refresh()
                    else _toastMessage.emit("خطا در ثبت پرداخت.")
                }

                is PersonScreenEvent.SelectPerson -> {
                    _selectedPersonId.value = event.personId
                    _selectedYear.value = getCurrentShamsiYear()
                }

                is PersonScreenEvent.DeletePerson -> {
                    if (networkRepository.deletePersonAndPayments(event.personId)) networkRepository.refresh()
                }

                is PersonScreenEvent.ArchivePerson -> {
                    if (networkRepository.updatePersonArchivedStatus(event.personId, true)) {
                        _toastMessage.emit("شخص به آرشیو انتقال یافت.")
                        networkRepository.refresh()
                    }
                }

                is PersonScreenEvent.RestorePerson -> {
                    if (networkRepository.updatePersonArchivedStatus(event.personId, false)) {
                        _toastMessage.emit("شخص از آرشیو بازیابی شد.")
                        networkRepository.refresh()
                    }
                }

                is PersonScreenEvent.UpdatePerson -> {
                    val statusCode = networkRepository.updatePerson(event.personId, event.name, event.phoneNumber)
                    if (statusCode == 409) _toastMessage.emit("خطا: این نام از قبل وجود دارد.")
                    else if (statusCode == 200) networkRepository.refresh()
                    else _toastMessage.emit("خطا در ویرایش اطلاعات.")
                }

                is PersonScreenEvent.ChangeYear -> {
                    val currentYear = getCurrentShamsiYear()
                    if (event.offset == 1) {
                        if (_selectedYear.value < currentYear) _selectedYear.update { it + 1 }
                        else _infoMessage.emit("امکان مشاهده سال آینده وجود ندارد.")
                    } else {
                        _selectedYear.update { it - 1 }
                    }
                }

                is PersonScreenEvent.AddPaymentForMonth -> {
                    val record = createPaymentRecord(event.personId, event.month, event.year, event.amount, event.description)
                    if (networkRepository.addPayment(record)) networkRepository.refresh()
                    else _toastMessage.emit("خطا در ثبت پرداخت")
                }

                is PersonScreenEvent.UpdatePayment -> {
                    val updatedRecord = event.payment.copy(amount = event.newAmount, description = event.newDescription, timestamp = System.currentTimeMillis())
                    if (networkRepository.addPayment(updatedRecord)) networkRepository.refresh()
                    else _toastMessage.emit("خطا در ویرایش پرداخت")
                }

                is PersonScreenEvent.DeletePayment -> {
                    if (networkRepository.deletePayment(event.payment.id)) networkRepository.refresh()
                    else _toastMessage.emit("خطا در حذف پرداخت")
                }

                is PersonScreenEvent.ToggleSelection -> {
                    _selectedIds.update { current ->
                        if (current.contains(event.personId)) {
                            val newSet = current - event.personId
                            if (newSet.isEmpty()) _isSelectionMode.value = false
                            newSet
                        } else {
                            _isSelectionMode.value = true
                            current + event.personId
                        }
                    }
                }

                is PersonScreenEvent.ClearSelection -> {
                    _selectedIds.value = emptySet()
                    _isSelectionMode.value = false
                }

                is PersonScreenEvent.ExportYearlyReport -> exportYearlyReport(event.year, event.context)

                is PersonScreenEvent.CommitReorder -> {
                    val list = if (event.listType == PersonListType.UNPAID) _uiState.value.unpaidPersons else _uiState.value.paidPersons
                    if (list.size < 2) return@launch
                    try {
                        list.forEachIndexed { index, person ->
                            val newOrder = (index + 1) * 10_000L
                            if (person.displayOrder != newOrder) {
                                networkRepository.updatePersonDisplayOrder(person.id, newOrder)
                            }
                        }
                        networkRepository.refresh()
                    } catch (_: Exception) {
                        _toastMessage.emit("خطا در ذخیره ترتیب")
                        networkRepository.refresh()
                    }
                }

                PersonScreenEvent.RefreshData -> {
                    _isRefreshing.value = true
                    networkRepository.refresh()
                    _isRefreshing.value = false
                }
                else -> {}
            }
        }
    }

    private fun exportYearlyReport(year: Int, context: Context) {
        viewModelScope.launch {
            val persons = localPersonRepository.getAllPersonsFlow().first()
            val payments = localPersonRepository.getAllPaymentsFlow().first().filter { it.shamsiYear == year }
            if (!exportManager.exportYearlyReport(year, persons, payments)) {
                _toastMessage.emit("خطا در ایجاد گزارش")
            }
        }
    }

    fun shareReceipt(payment: PaymentRecord, personName: String) {
        exportManager.shareReceipt(payment, personName)
    }

    private fun updateDashboard(totalPersonCount: Int, paymentsThisMonth: List<PaymentRecord>) {
        val paidCount = paymentsThisMonth.map { it.personId }.distinct().size
        val totalIncome = paymentsThisMonth.sumOf { it.amount }
        val progress = if (totalPersonCount > 0) paidCount.toFloat() / totalPersonCount.toFloat() else 0f
        _dashboardData.value = DashboardUiModel(paidCount, totalPersonCount, totalIncome, progress)
    }

    private fun createPaymentRecord(personId: String, month: Int, year: Int, amount: Double, description: String): PaymentRecord {
        return PaymentRecord(personId = personId, amount = amount, shamsiYear = year, shamsiMonth = month, description = description)
    }
}
