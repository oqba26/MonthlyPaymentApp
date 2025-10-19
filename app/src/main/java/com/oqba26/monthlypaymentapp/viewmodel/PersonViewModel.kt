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

class PersonViewModel(
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _expandedPersonId = MutableStateFlow<String?>(null)
    val expandedPersonId = _expandedPersonId.asStateFlow()

    private val _selectedYear = MutableStateFlow(getCurrentShamsiYear())
    val selectedYear = _selectedYear.asStateFlow()

    private val _dashboardData = MutableStateFlow(DashboardUiModel())
    val dashboardData = _dashboardData.asStateFlow()

    val defaultPaymentAmountFlow = settingsRepository.defaultPaymentAmountFlow

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    val authState: StateFlow<AuthState> = settingsRepository.authTokenFlow.map { token ->
        if (token != null) AuthState.Authenticated else AuthState.Unauthenticated
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PersonListUiState> = authState.flatMapLatest { auth ->
        when (auth) {
            is AuthState.Authenticated -> {
                combine(
                    networkRepository.getPersonsFlow(),
                    networkRepository.getPaymentsFlow(),
                    _searchQuery
                ) { persons, allPayments, query ->
                    val currentMonth = getCurrentShamsiMonth()
                    val currentYear = getCurrentShamsiYear()
                    val paymentsThisMonth = allPayments.filter { it.shamsiMonth == currentMonth && it.shamsiYear == currentYear }

                    val uiModels = persons.map { person ->
                        val hasPaid = paymentsThisMonth.any { it.personId == person.id }
                        PersonUiModel(id = person.id, name = person.name, hasPaidThisMonth = hasPaid)
                    }

                    updateDashboard(uiModels.size, paymentsThisMonth)

                    val filtered = if (query.isBlank()) uiModels
                    else uiModels.filter { it.name.contains(query, ignoreCase = true) }

                    PersonListUiState(persons = filtered, payments = allPayments)
                }
            }
            else -> {
                flowOf(PersonListUiState()) // Emit empty state if not authenticated
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PersonListUiState())

    init {
        viewModelScope.launch {
            // This will trigger a refresh whenever the user becomes authenticated.
            authState.collectLatest { state ->
                if (state is AuthState.Authenticated) {
                    networkRepository.refresh()
                }
            }
        }
    }

    // Auth Actions
    fun login(request: AuthRequest) = viewModelScope.launch {
        val response = networkRepository.login(request)
        if (response != null && response.token.isNotBlank()) {
            settingsRepository.saveAuthData(response.token, response.userId)
            _toastMessage.emit("ورود موفقیت‌آمیز بود")
        } else {
            _toastMessage.emit("نام کاربری یا رمز عبور اشتباه است.")
        }
    }

    fun register(request: AuthRequest) = viewModelScope.launch {
        val response = networkRepository.register(request)
        if (response != null && response.token.isNotBlank()) {
            settingsRepository.saveAuthData(response.token, response.userId)
            _toastMessage.emit("ثبت نام و ورود موفقیت‌آمیز بود")
        } else {
            _toastMessage.emit("خطا در ثبت نام.")
        }
    }

    fun logout() = viewModelScope.launch {
        settingsRepository.saveAuthData(null, null)
        _toastMessage.emit("از حساب کاربری خارج شدید.")
    }

    // UI Events
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun onEvent(event: PersonScreenEvent) {
        viewModelScope.launch {
            when (event) {
                is PersonScreenEvent.AddPerson -> {
                    if (event.name.isNotBlank()) {
                        val statusCode = networkRepository.addPerson(Person(name = event.name))
                        if (statusCode == 409) {
                            _toastMessage.emit("خطا: این نام از قبل وجود دارد.")
                        } else if (statusCode == null || statusCode !in 200..299) {
                            _toastMessage.emit("خطا در افزودن شخص.")
                        }
                    }
                }
                is PersonScreenEvent.DeletePerson -> {
                    networkRepository.deletePersonAndPayments(event.personId)
                }
                is PersonScreenEvent.AddQuickPayment -> {
                    val record = createPaymentRecord(event.personId, getCurrentShamsiMonth(), getCurrentShamsiYear(), event.amount)
                    networkRepository.addPayment(record)
                }
                is PersonScreenEvent.UpdatePerson -> {
                    networkRepository.updatePerson(event.personId, event.name)
                }
                is PersonScreenEvent.Login -> login(AuthRequest(event.username, event.password))
                is PersonScreenEvent.Register -> register(AuthRequest(event.username, event.password))
                is PersonScreenEvent.TogglePersonExpansion -> {
                    val currentId = _expandedPersonId.value
                    if (currentId == event.personId) {
                        _expandedPersonId.value = null
                    } else {
                        _expandedPersonId.value = event.personId
                        _selectedYear.value = getCurrentShamsiYear() // Reset year when opening a new item
                    }
                }
                is PersonScreenEvent.ChangeYear -> {
                    _selectedYear.update { it + event.offset }
                }
                is PersonScreenEvent.AddPaymentForMonth -> {
                    val record = createPaymentRecord(event.personId, event.month, event.year, event.amount)
                    val success = networkRepository.addPayment(record)
                    if (!success) _toastMessage.emit("خطا در ثبت پرداخت")
                }
                is PersonScreenEvent.UpdatePayment -> {
                    val updatedRecord = event.payment.copy(amount = event.newAmount)
                    val success = networkRepository.addPayment(updatedRecord)
                    if (!success) _toastMessage.emit("خطا در ویرایش پرداخت")
                }
                is PersonScreenEvent.DeletePayment -> {
                    val success = networkRepository.deletePayment(event.payment.id)
                    if (!success) _toastMessage.emit("خطا در حذف پرداخت")
                }

                PersonScreenEvent.RefreshData -> {
                    _isRefreshing.value = true
                    networkRepository.refresh()
                    _isRefreshing.value = false
                }
            }
        }
    }

    private fun updateDashboard(totalPersonCount: Int, paymentsThisMonth: List<PaymentRecord>) {
        val paidCount = paymentsThisMonth.map { it.personId }.distinct().size
        val totalIncome = paymentsThisMonth.sumOf { it.amount }
        val progress = if (totalPersonCount > 0) paidCount.toFloat() / totalPersonCount.toFloat() else 0f
        _dashboardData.value = DashboardUiModel(paidCount, totalPersonCount, totalIncome, progress)
    }

    private fun createPaymentRecord(personId: String, month: Int, year: Int, amount: Double): PaymentRecord {
        return PaymentRecord(personId = personId, amount = amount, shamsiYear = year, shamsiMonth = month)
    }
}

data class PersonListUiState(
    val persons: List<PersonUiModel> = emptyList(),
    val payments: List<PaymentRecord> = emptyList()
)

sealed class PersonScreenEvent {
    data object RefreshData : PersonScreenEvent()
    data class AddPerson(val name: String) : PersonScreenEvent()
    data class UpdatePerson(val personId: String, val name: String) : PersonScreenEvent()
    data class DeletePerson(val personId: String) : PersonScreenEvent()
    data class AddQuickPayment(val personId: String, val amount: Double) : PersonScreenEvent()
    data class Login(val username: String, val password: String) : PersonScreenEvent()
    data class Register(val username: String, val password: String) : PersonScreenEvent()
    data class TogglePersonExpansion(val personId: String) : PersonScreenEvent()

    // Events for the detail section
    data class ChangeYear(val offset: Int) : PersonScreenEvent()
    data class AddPaymentForMonth(val personId: String, val month: Int, val year: Int, val amount: Double) : PersonScreenEvent()
    data class UpdatePayment(val payment: PaymentRecord, val newAmount: Double) : PersonScreenEvent()
    data class DeletePayment(val payment: PaymentRecord) : PersonScreenEvent()
}
