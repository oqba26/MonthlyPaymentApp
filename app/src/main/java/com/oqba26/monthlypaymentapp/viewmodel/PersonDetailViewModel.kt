package com.oqba26.monthlypaymentapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.oqba26.monthlypaymentapp.core.PaymentApplication
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.utils.getCurrentShamsiYear
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PersonDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val networkRepository: NetworkRepository, // ⭐️ FIX: localRepository removed
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val personId: String = requireNotNull(savedStateHandle.get<String>("personId"))

    // ⭐️ FIX: Fetch person from the network flow
    val person: StateFlow<Person?> = networkRepository.getPersonsFlow()
        .map { persons -> persons.find { it.id == personId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedYear = kotlinx.coroutines.flow.MutableStateFlow(getCurrentShamsiYear())
    val selectedYear = _selectedYear.asStateFlow()

    // ⭐️ FIX: Fetch payment history from the correct network flow
    val paymentHistory: StateFlow<List<PaymentRecord>> = networkRepository.getPaymentsFlow()
        .map { payments -> payments.filter { it.personId == personId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    val defaultPaymentAmount = settingsRepository.defaultPaymentAmountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun changeYear(offset: Int) { _selectedYear.update { it + offset } }

    fun addPaymentForMonth(month: Int, year: Int, amount: Double) {
        viewModelScope.launch {
            val newPayment = PaymentRecord(
                amount = amount,
                personId = personId,
                shamsiYear = year,
                shamsiMonth = month
            )
            val ok = networkRepository.addPayment(newPayment)
            if (!ok) _toastMessage.emit("ثبت پرداخت با خطا مواجه شد.")
        }
    }

    fun updatePayment(payment: PaymentRecord, newAmount: Double) {
        viewModelScope.launch {
            val updatedPayment = payment.copy(amount = newAmount)
            val ok = networkRepository.addPayment(updatedPayment)
            if (!ok) _toastMessage.emit("به‌روزرسانی پرداخت با خطا مواجه شد.")
        }
    }

    fun deletePayment(payment: PaymentRecord) {
        viewModelScope.launch {
            val ok = networkRepository.deletePayment(payment.id)
            if (!ok) _toastMessage.emit("حذف پرداخت با خطا مواجه شد.")
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]) as PaymentApplication
                val saved = extras.createSavedStateHandle()
                // ⭐️ FIX: Provide the correct dependencies
                return PersonDetailViewModel(
                    saved,
                    app.networkRepository,
                    app.settingsRepository
                ) as T
            }
        }
    }
}
