package com.oqba26.monthlypaymentapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiMonth
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
}

enum class PersonListType {
    UNPAID,
    PAID
}

@OptIn(ExperimentalCoroutinesApi::class)
class PersonViewModel(
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    @Suppress("unused")
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedPersonId = MutableStateFlow<String?>(null)
    private val _selectedYear = MutableStateFlow(getCurrentShamsiYear())

    private val _dashboardData = MutableStateFlow(DashboardUiModel())
    val dashboardData: StateFlow<DashboardUiModel> = _dashboardData.asStateFlow()

    val defaultPaymentAmountFlow = settingsRepository.defaultPaymentAmountFlow

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


    val authState: StateFlow<AuthState> = settingsRepository.authTokenFlow
        .map { token ->
            if (token != null) AuthState.Authenticated else AuthState.Unauthenticated
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    val personDetailState: StateFlow<PersonDetailUiState> = _selectedPersonId
        .flatMapLatest { personId ->
            if (personId == null) {
                flowOf(PersonDetailUiState())
            } else {
                combine(
                    networkRepository.getPersonsFlow(),
                    networkRepository.getPaymentsFlow(),
                    _selectedYear
                ) { persons, allPayments, year ->
                    val person = persons.find { it.id == personId }
                    val paymentsForPersonInYear = allPayments.filter {
                        it.personId == personId && it.shamsiYear == year
                    }
                    val currentYear = getCurrentShamsiYear()
                    val currentMonth = getCurrentShamsiMonth()
                    val monthStates = (1..12).map { month ->
                        val payment = paymentsForPersonInYear.find { it.shamsiMonth == month }
                        val status = when {
                            year < currentYear -> MonthStatus.PAST_YEAR
                            year > currentYear -> MonthStatus.FUTURE_YEAR
                            month > currentMonth -> MonthStatus.FUTURE_MONTH
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
        viewModelScope.launch {
            authState.collectLatest { authState ->
                if (authState is AuthState.Authenticated) {
                    // اولین بار دیتای سرور
                    networkRepository.refresh()

                    // گوش دادن به تغییرات اشخاص + پرداخت‌ها + سرچ
                    combine(
                        networkRepository.getPersonsFlow(),
                        networkRepository.getPaymentsFlow(),
                        _searchQuery
                    ) { persons, allPayments, query ->
                        val currentMonth = getCurrentShamsiMonth()
                        val currentYear = getCurrentShamsiYear()

                        val paymentsThisMonth = allPayments.filter {
                            it.shamsiMonth == currentMonth && it.shamsiYear == currentYear
                        }

                        val activePersons = persons
                            .filter { !it.isArchived }
                            .sortedWith(compareBy({ it.displayOrder }, { it.createdAt }))

                        val uiModels = activePersons.map { person ->
                            val hasPaid = paymentsThisMonth.any { it.personId == person.id }
                            PersonUiModel(
                                id = person.id,
                                name = person.name,
                                hasPaidThisMonth = hasPaid,
                                displayOrder = person.displayOrder,
                                createdAt = person.createdAt
                            )
                        }

                        updateDashboard(uiModels.size, paymentsThisMonth)

                        val (paidModels, unpaidModels) =
                            uiModels.partition { it.hasPaidThisMonth }

                        val filteredPaid =
                            if (query.isBlank()) paidModels
                            else paidModels.filter { it.name.contains(query, ignoreCase = true) }

                        val filteredUnpaid =
                            if (query.isBlank()) unpaidModels
                            else unpaidModels.filter { it.name.contains(query, ignoreCase = true) }

                        val archivedPersons = persons
                            .filter { it.isArchived }
                            .sortedBy { it.displayOrder }
                            .map {
                                PersonUiModel(
                                    id = it.id,
                                    name = it.name,
                                    hasPaidThisMonth = false,
                                    displayOrder = it.displayOrder,
                                    createdAt = it.createdAt
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
                } else {
                    _uiState.value = PersonListUiState()
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onAddPersonClicked() {
        _showAddPersonDialog.value = true
    }

    fun onDismissAddPersonDialog() {
        _showAddPersonDialog.value = false
    }

    fun onQuickPayClicked(person: PersonUiModel) {
        _personForPaymentDialog.value = person
    }

    fun onDismissPaymentDialog() {
        _personForPaymentDialog.value = null
    }


    fun onEvent(event: PersonScreenEvent) {
        viewModelScope.launch {
            when (event) {
                is PersonScreenEvent.MovePersonNew -> {
                    // فقط جابه‌جا کردن لیست لوکال (بدون شبکه)
                    val fromIndex = event.fromIndex
                    val toIndex = event.toIndex
                    val currentListState = _uiState.value

                    val list = when (event.listType) {
                        PersonListType.UNPAID -> currentListState.unpaidPersons
                        PersonListType.PAID -> currentListState.paidPersons
                    }

                    if (fromIndex !in list.indices || toIndex !in list.indices) return@launch

                    val reorderedList = list.toMutableList().apply {
                        add(toIndex, removeAt(fromIndex))
                    }

                    _uiState.update {
                        when (event.listType) {
                            PersonListType.UNPAID -> it.copy(unpaidPersons = reorderedList)
                            PersonListType.PAID -> it.copy(paidPersons = reorderedList)
                        }
                    }
                }

                is PersonScreenEvent.AddPerson -> {
                    if (event.name.isNotBlank()) {
                        val person = Person(name = event.name, displayOrder = 0)
                        val statusCode = networkRepository.addPerson(person)
                        when (statusCode) {
                            409 -> {
                                _toastMessage.emit("خطا: این نام از قبل وجود دارد.")
                            }
                            null, !in 200..299 -> {
                                _toastMessage.emit("خطا در افزودن شخص.")
                            }
                            else -> {
                                networkRepository.refresh()
                            }
                        }
                    }
                }
                is PersonScreenEvent.AddQuickPayment -> {
                    val record = createPaymentRecord(
                        event.personId,
                        getCurrentShamsiMonth(),
                        getCurrentShamsiYear(),
                        event.amount,
                        event.description
                    )

                    if (networkRepository.addPayment(record)) {
                        networkRepository.refresh()
                    } else {
                        _toastMessage.emit("خطا در ثبت پرداخت. لطفا دوباره تلاش کنید.")
                    }
                }

                else -> handleOtherEvents(event)
            }
        }
    }

    private fun handleOtherEvents(event: PersonScreenEvent) {
        viewModelScope.launch {
            when (event) {
                is PersonScreenEvent.SelectPerson -> {
                    _selectedPersonId.value = event.personId
                    _selectedYear.value = getCurrentShamsiYear()
                }

                is PersonScreenEvent.DeletePerson -> {
                    if (networkRepository.deletePersonAndPayments(event.personId)) {
                        networkRepository.refresh()
                    }
                }

                is PersonScreenEvent.ArchivePerson -> {
                    if (networkRepository.updatePersonArchivedStatus(event.personId, true)) {
                        _toastMessage.emit("شخص به آرشیو انتقال یافت.")
                        networkRepository.refresh()
                    } else {
                        _toastMessage.emit("خطا در آرشیو کردن.")
                    }
                }

                is PersonScreenEvent.RestorePerson -> {
                    if (networkRepository.updatePersonArchivedStatus(event.personId, false)) {
                        _toastMessage.emit("شخص از آرشیو بازیابی شد.")
                        networkRepository.refresh()
                    } else {
                        _toastMessage.emit("خطا در بازیابی.")
                    }
                }

                is PersonScreenEvent.UpdatePerson -> {
                    if (networkRepository.updatePerson(event.personId, event.name)) {
                        networkRepository.refresh()
                    }
                }

                is PersonScreenEvent.Login -> login(AuthRequest(event.username, event.password))

                is PersonScreenEvent.Register -> register(AuthRequest(event.username, event.password))

                is PersonScreenEvent.ChangeYear -> {
                    when (event.offset) {
                        1 -> {
                            if (_selectedYear.value < getCurrentShamsiYear()) {
                                _selectedYear.update { it + 1 }
                            } else {
                                _infoMessage.emit("امکان مشاهده سال آینده وجود ندارد.")
                            }
                        }

                        -1 -> {
                            val personId = _selectedPersonId.value ?: return@launch
                            val hasPaymentsInPast = uiState.value.payments.any {
                                it.personId == personId && it.shamsiYear < _selectedYear.value
                            }
                            if (hasPaymentsInPast) {
                                _selectedYear.update { it - 1 }
                            } else {
                                _infoMessage.emit("هیچ پرداختی برای سال‌های گذشته ثبت نشده است.")
                            }
                        }
                    }
                }

                is PersonScreenEvent.AddPaymentForMonth -> {
                    val record = createPaymentRecord(
                        event.personId,
                        event.month,
                        event.year,
                        event.amount,
                        event.description
                    )
                    if (networkRepository.addPayment(record)) {
                        networkRepository.refresh()
                    } else {
                        _toastMessage.emit("خطا در ثبت پرداخت")
                    }
                }

                is PersonScreenEvent.UpdatePayment -> {
                    val updatedRecord = event.payment.copy(
                        amount = event.newAmount,
                        description = event.newDescription,
                        timestamp = System.currentTimeMillis()
                    )
                    if (networkRepository.addPayment(updatedRecord)) {
                        networkRepository.refresh()
                    } else {
                        _toastMessage.emit("خطا در ویرایش پرداخت")
                    }
                }

                is PersonScreenEvent.DeletePayment -> {
                    if (networkRepository.deletePayment(event.payment.id)) {
                        networkRepository.refresh()
                    } else {
                        _toastMessage.emit("خطا در حذف پرداخت")
                    }
                }

                is PersonScreenEvent.CommitReorder -> {
                    // بعد از اتمام درگ: ترتیب فعلی لیست را به عنوان displayOrder جدید ذخیره کن
                    val currentListState = _uiState.value
                    val list = when (event.listType) {
                        PersonListType.UNPAID -> currentListState.unpaidPersons
                        PersonListType.PAID -> currentListState.paidPersons
                    }

                    if (list.size < 2) return@launch

                    try {
                        list.forEachIndexed { index, person ->
                            val newOrder = (index + 1) * 10_000L
                            if (person.displayOrder != newOrder) {
                                val success = networkRepository.updatePersonDisplayOrder(
                                    person.id,
                                    newOrder
                                )
                                if (!success) {
                                    throw Exception("update failed")
                                }
                            }
                        }
                        // بعد از ذخیره، از سرور رفرش کن تا همه‌چیز باهم سینک بشه
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

    private fun createPaymentRecord(
        personId: String,
        month: Int,
        year: Int,
        amount: Double,
        description: String
    ): PaymentRecord {
        return PaymentRecord(
            personId = personId,
            amount = amount,
            shamsiYear = year,
            shamsiMonth = month,
            description = description
        )
    }

    private fun updateDashboard(totalPersonCount: Int, paymentsThisMonth: List<PaymentRecord>) {
        val paidCount = paymentsThisMonth.map { it.personId }.distinct().size
        val totalIncome = paymentsThisMonth.sumOf { it.amount }
        val progress =
            if (totalPersonCount > 0) paidCount.toFloat() / totalPersonCount.toFloat() else 0f
        _dashboardData.value =
            DashboardUiModel(paidCount, totalPersonCount, totalIncome, progress)
    }

    fun logout() = viewModelScope.launch {
        settingsRepository.saveAuthData(null, null)
        _toastMessage.emit("از حساب کاربری خارج شدید.")
    }

    fun login(request: AuthRequest) = viewModelScope.launch {
        val response = networkRepository.login(request)
        if (response != null && response.token.isNotBlank()) {
            settingsRepository.saveAuthData(response.token, response.userId)
            _toastMessage.emit("ورود موفقیت‌آمیز بود")
            networkRepository.refresh()
        } else {
            _toastMessage.emit("نام کاربری یا رمز عبور اشتباه است.")
        }
    }

    fun register(request: AuthRequest) = viewModelScope.launch {
        val response = networkRepository.register(request)
        if (response != null && response.token.isNotBlank()) {
            settingsRepository.saveAuthData(response.token, response.userId)
            _toastMessage.emit("ثبت نام و ورود موفقیت‌آمیز بود")
            networkRepository.refresh()
        } else {
            _toastMessage.emit("خطا در ثبت نام.")
        }
    }
}

sealed class PersonScreenEvent {
    data object RefreshData : PersonScreenEvent()
    data class AddPerson(val name: String) : PersonScreenEvent()
    data class UpdatePerson(val personId: String, val name: String) : PersonScreenEvent()
    data class DeletePerson(val personId: String) : PersonScreenEvent()
    data class ArchivePerson(val personId: String) : PersonScreenEvent()
    data class RestorePerson(val personId: String) : PersonScreenEvent()

    // درگ اند دراپ
    data class MovePersonNew(
        val fromIndex: Int,
        val toIndex: Int,
        val listType: PersonListType
    ) : PersonScreenEvent()

    // پایان درگ: ذخیره ترتیب
    data class CommitReorder(
        val listType: PersonListType
    ) : PersonScreenEvent()

    data class AddQuickPayment(
        val personId: String,
        val amount: Double,
        val description: String
    ) : PersonScreenEvent()

    data class Login(val username: String, val password: String) : PersonScreenEvent()
    data class Register(val username: String, val password: String) : PersonScreenEvent()

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
}
