package com.oqba26.monthlypaymentapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.monthlypaymentapp.core.manager.BackupManager
import com.oqba26.monthlypaymentapp.core.manager.ExportManager
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
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
    private val exportManager: ExportManager,
    private val backupManager: BackupManager,
) : ViewModel() {

    /** فقط یک بار در هر اجرا snapshot قبل از ادغام گرفته می‌شود. */
    private var snapshotTakenThisSession = false

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedPersonId = MutableStateFlow<String?>(null)
    private val _selectedYear = MutableStateFlow(getCurrentShamsiYear())
    private val _currentCategory = MutableStateFlow("salary")
    val currentCategory: StateFlow<String> = _currentCategory.asStateFlow()

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
                    _selectedYear,
                    _currentCategory
                ) { persons, allPayments, year, category ->
                    val person = persons.find { it.id == personId }
                    val personCategory = person?.category ?: category
                    val paymentsForPersonInYear = allPayments.filter {
                        (it.personId == personId) && (it.shamsiYear == year) && (it.category == personCategory)
                    }
                    val currentYear = getCurrentShamsiYear()
                    val currentMonth = getCurrentShamsiMonth()
                    val currentDay = getCurrentShamsiDay()
                    val monthStates = (1..12).map { month ->
                        val payment = paymentsForPersonInYear.find { it.shamsiMonth == month }
                        
                        // بررسی شروع تعهد
                        val isBeforeCommitment = if (person != null) {
                            year < person.startYear || (year == person.startYear && month < person.startMonth)
                        } else false

                        val status = when {
                            payment != null -> MonthStatus.PAID
                            isBeforeCommitment -> MonthStatus.NOT_COMMITTED
                            year < currentYear -> MonthStatus.PAST_YEAR
                            year > currentYear -> MonthStatus.FUTURE_YEAR
                            month > currentMonth || (month == currentMonth && currentDay < 20) -> MonthStatus.FUTURE_MONTH
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

                    // شرط قبلی `persons.isNotEmpty()` بود که دو مشکل داشت: هم لیست خالیِ
                    // معتبر سرور را نادیده می‌گرفت، و هم اگر واکشی شکست می‌خورد (چون refresh
                    // خطا را می‌بلعید) روی داده‌ی قدیمی/ناقص merge می‌کرد.
                    // حالا فقط بعد از یک refresh موفق merge می‌کنیم و خودِ merge هم غیرمخرب است.
                    combine(
                        networkRepository.getPersonsFlow(),
                        networkRepository.getPaymentsFlow(),
                        networkRepository.hasServerDataFlow()
                    ) { persons, payments, hasServerData ->
                        if (hasServerData) {
                            if (!snapshotTakenThisSession) {
                                // آخرین خط دفاع: قبل از اولین ادغام هر اجرا، وضعیت فعلی
                                // ذخیره می‌شود تا اگر با وجود همه‌ی محافظت‌ها چیزی خراب شد،
                                // کاربر بتواند از تنظیمات برش گرداند.
                                backupManager.createSnapshot(BackupManager.REASON_BEFORE_SYNC)
                                snapshotTakenThisSession = true
                            }
                            localPersonRepository.mergeFromServer(persons, payments)
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
                _searchQuery,
                _currentCategory
            ) { persons, allPayments, query, category ->
                val currentMonth = getCurrentShamsiMonth()
                val currentYear = getCurrentShamsiYear()
                val currentDay = getCurrentShamsiDay()

                val targetMonth = if (currentDay >= 20) currentMonth else if (currentMonth == 1) 12 else currentMonth - 1
                val targetYear = if (currentDay < 20 && currentMonth == 1) currentYear - 1 else currentYear

                val relevantPayments = allPayments.filter {
                    it.shamsiMonth == targetMonth && it.shamsiYear == targetYear && it.category == category
                }

                val activePersons = persons
                    .filter { !it.isArchived && it.category == category }
                    .sortedWith(compareBy({ it.displayOrder }, { it.createdAt }))

                val uiModels = activePersons.map { person ->
                    val hasPaid = relevantPayments.any { it.personId == person.id }
                    
                    val (debtCount, totalDebtAmount, unpaidMonths) = if (category == "mosque") {
                        if (person.monthlyCommitment > 0) {
                            val debtEndMonth = if (currentDay >= 20) currentMonth else currentMonth - 1
                            val debtStartMonth = if (person.startYear == currentYear) person.startMonth else 1
                            
                            val unpaid = (debtStartMonth..debtEndMonth).filter { m ->
                                allPayments.none { it.personId == person.id && it.shamsiYear == currentYear && it.shamsiMonth == m && it.category == category }
                            }
                            Triple(unpaid.size, unpaid.size * person.monthlyCommitment, unpaid.map { getPersianMonthName(it) })
                        } else {
                            Triple(0, 0.0, emptyList())
                        }
                    } else {
                        // Salary logic
                        val debtEndMonth = if (currentDay >= 20) currentMonth else currentMonth - 1
                        val debtStartMonth = if (person.startYear == currentYear) person.startMonth else 1
                        
                        val unpaid = (debtStartMonth..debtEndMonth).filter { m ->
                            allPayments.none { it.personId == person.id && it.shamsiYear == currentYear && it.shamsiMonth == m && it.category == category }
                        }
                        val defaultAmount = settingsRepository.defaultPaymentAmountFlow.first()
                        Triple(unpaid.size, unpaid.size * defaultAmount, unpaid.map { getPersianMonthName(it) })
                    }

                    val displayName = if (person.isAnonymous && category == "mosque") {
                        "خیر ناشناس (${person.name})"
                    } else {
                        person.name
                    }

                    PersonUiModel(
                        id = person.id,
                        name = displayName,
                        hasPaidThisMonth = hasPaid,
                        displayOrder = person.displayOrder,
                        createdAt = person.createdAt,
                        debtCount = debtCount,
                        totalDebtAmount = totalDebtAmount,
                        phoneNumber = person.phoneNumber,
                        isAnonymous = person.isAnonymous,
                        monthlyCommitment = person.monthlyCommitment,
                        unpaidMonthsNames = unpaidMonths,
                        needsSync = person.needsSync
                    )
                }

                updateDashboard(uiModels.size, relevantPayments)

                val (paidModels, unpaidModels) = uiModels.partition { it.hasPaidThisMonth }

                val filteredPaid = if (query.isBlank()) paidModels else paidModels.filter { it.name.contains(query, ignoreCase = true) }
                val filteredUnpaid = if (query.isBlank()) unpaidModels else unpaidModels.filter { it.name.contains(query, ignoreCase = true) }

                val archivedPersons = persons.filter { it.isArchived && it.category == category }.map { person ->
                    val displayName = if (person.isAnonymous && category == "mosque") {
                        "خیر ناشناس (${person.name})"
                    } else {
                        person.name
                    }
                    PersonUiModel(
                        id = person.id,
                        name = displayName,
                        hasPaidThisMonth = false,
                        displayOrder = person.displayOrder,
                        createdAt = person.createdAt,
                        isAnonymous = person.isAnonymous,
                        needsSync = person.needsSync
                    )
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
                    if (event.isAnonymous || event.name.isNotBlank()) {
                        val persons = localPersonRepository.getAllPersonsFlow().first()
                        val finalName = if (event.isAnonymous && event.name.isBlank()) "ناشناس" else event.name
                        
                        val isDuplicate = if (!event.isAnonymous) {
                            persons.any { it.category == _currentCategory.value && it.name.trim().equals(finalName.trim(), ignoreCase = true) }
                        } else false
                        
                        if (isDuplicate) {
                            _toastMessage.emit("خطا: این نام از قبل وجود دارد.")
                        } else {
                            val personId = java.util.UUID.randomUUID().toString()
                            val person = Person(
                                id = personId,
                                name = finalName,
                                phoneNumber = event.phoneNumber,
                                displayOrder = 0,
                                category = _currentCategory.value,
                                isAnonymous = event.isAnonymous,
                                monthlyCommitment = if (event.isAnonymous) 0.0 else event.monthlyCommitment,
                                startMonth = event.startMonth,
                                startYear = event.startYear,
                                createdAt = System.currentTimeMillis()
                            )
                            localPersonRepository.insertPersonLocally(person)
                            
                            // اگر مبلغ اولیه وارد شده باشد (مخصوصا برای ناشناس)
                            if (event.initialPaymentAmount > 0) {
                                val currentMonth = getCurrentShamsiMonth()
                                val currentYear = getCurrentShamsiYear()
                                val record = createPaymentRecord(
                                    personId = personId,
                                    month = currentMonth,
                                    year = currentYear,
                                    amount = event.initialPaymentAmount,
                                    description = if (event.isAnonymous) "کمک آنی خیر ناشناس" else "پرداخت اولیه"
                                ).copy(category = _currentCategory.value)
                                localPersonRepository.insertPaymentLocally(record)
                            }
                        }
                    }
                }

                is PersonScreenEvent.AddQuickPayment -> {
                    val currentDay = getCurrentShamsiDay()
                    val currentMonth = getCurrentShamsiMonth()
                    val currentYear = getCurrentShamsiYear()
                    val targetMonth = if (currentDay >= 20) currentMonth else if (currentMonth == 1) 12 else currentMonth - 1
                    val targetYear = if (currentDay < 20 && currentMonth == 1) currentYear - 1 else currentYear

                    val record = createPaymentRecord(event.personId, targetMonth, targetYear, event.amount, event.description)
                        .copy(category = _currentCategory.value)
                    localPersonRepository.insertPaymentLocally(record)
                }

                is PersonScreenEvent.SelectPerson -> {
                    _selectedPersonId.value = event.personId
                    _selectedYear.value = getCurrentShamsiYear()
                }

                is PersonScreenEvent.DeletePerson -> {
                    localPersonRepository.deletePersonLocally(event.personId)
                }

                is PersonScreenEvent.ArchivePerson -> {
                    localPersonRepository.archivePersonLocally(event.personId, true)
                    _toastMessage.emit("شخص به آرشیو انتقال یافت.")
                }

                is PersonScreenEvent.RestorePerson -> {
                    localPersonRepository.archivePersonLocally(event.personId, false)
                    _toastMessage.emit("شخص از آرشیو بازیابی شد.")
                }

                is PersonScreenEvent.UpdatePerson -> {
                    val persons = localPersonRepository.getAllPersonsFlow().first()
                    val isDuplicate = persons.any { it.id != event.personId && it.category == _currentCategory.value && it.name.trim().equals(event.name.trim(), ignoreCase = true) }
                    
                    if (isDuplicate) {
                        _toastMessage.emit("خطا: این نام از قبل وجود دارد.")
                    } else {
                        val currentPerson = persons.find { it.id == event.personId }
                        if (currentPerson != null) {
                            val updatedPerson = currentPerson.copy(
                                name = event.name,
                                phoneNumber = event.phoneNumber,
                                monthlyCommitment = event.monthlyCommitment,
                                startMonth = event.startMonth,
                                startYear = event.startYear
                            )
                            localPersonRepository.updatePersonLocally(updatedPerson)
                        }
                    }
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
                    val person = localPersonRepository.getAllPersonsFlow().first().find { it.id == event.personId }
                    val category = person?.category ?: _currentCategory.value
                    val record = createPaymentRecord(event.personId, event.month, event.year, event.amount, event.description)
                        .copy(category = category)
                    localPersonRepository.insertPaymentLocally(record)
                }

                is PersonScreenEvent.UpdatePayment -> {
                    val updatedRecord = event.payment.copy(amount = event.newAmount, description = event.newDescription, timestamp = System.currentTimeMillis())
                    localPersonRepository.insertPaymentLocally(updatedRecord)
                }

                is PersonScreenEvent.DeletePayment -> {
                    localPersonRepository.deletePaymentLocally(event.payment.id)
                    _toastMessage.emit("پرداخت با موفقیت حذف شد")
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

                is PersonScreenEvent.ExportYearlyReport -> exportYearlyReport(event.year)

                is PersonScreenEvent.AddBulkPayments -> {
                    try {
                        val person = localPersonRepository.getAllPersonsFlow().first().find { it.id == event.personId }
                        val category = person?.category ?: _currentCategory.value
                        event.months.forEach { month ->
                            val record = createPaymentRecord(
                                personId = event.personId,
                                month = month,
                                year = event.year,
                                amount = event.amount,
                                description = "پرداخت دسته جمعی"
                            ).copy(category = category)
                            localPersonRepository.insertPaymentLocally(record)
                        }
                    } catch (_: Exception) {
                        _toastMessage.emit("خطا در ثبت پرداخت‌های دسته جمعی")
                    }
                }

                is PersonScreenEvent.SetCategory -> {
                    _currentCategory.value = event.category
                    _selectedIds.value = emptySet()
                    _isSelectionMode.value = false
                }

                is PersonScreenEvent.CommitReorder -> {
                    val list = if (event.listType == PersonListType.UNPAID) _uiState.value.unpaidPersons else _uiState.value.paidPersons
                    if (list.size < 2) return@launch
                    try {
                        list.forEachIndexed { index, personUi ->
                            val newOrder = (index + 1) * 10_000L
                            if (personUi.displayOrder != newOrder) {
                                val person = localPersonRepository.getAllPersonsFlow().first().find { it.id == personUi.id }
                                person?.let {
                                    localPersonRepository.updatePersonLocally(it.copy(displayOrder = newOrder))
                                }
                            }
                        }
                    } catch (_: Exception) {
                        _toastMessage.emit("خطا در ذخیره ترتیب")
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

    private fun exportYearlyReport(year: Int) {
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
