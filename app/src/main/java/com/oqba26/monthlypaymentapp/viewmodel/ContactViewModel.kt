package com.oqba26.monthlypaymentapp.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.monthlypaymentapp.core.manager.SmsManager
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.utils.normalizePhoneNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactUiState(
    val contactSuggestions: List<ContactSuggestion> = emptyList(),
    val bulkSmsQueue: List<PersonUiModel> = emptyList(),
    val currentBulkIndex: Int = -1,
    val selectedCardForBulk: String? = null,
    val showBulkSmsDialog: Boolean = false,
    val personForSmsDialog: PersonUiModel? = null,
    val bulkSmsQueueForDialog: List<PersonUiModel> = emptyList(),
)

@HiltViewModel
class ContactViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository,
    private val smsManager: SmsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactUiState())
    val uiState = _uiState.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun onBulkSmsClicked() {
        _uiState.update { it.copy(showBulkSmsDialog = true) }
    }

    fun onBulkSelectionConfirmed(selected: List<PersonUiModel>) {
        _uiState.update { it.copy(showBulkSmsDialog = false, bulkSmsQueueForDialog = selected) }
    }

    fun onDismissBulkSmsDialog() {
        _uiState.update { it.copy(showBulkSmsDialog = false) }
    }

    fun onDismissSmsDialog() {
        _uiState.update { it.copy(personForSmsDialog = null, bulkSmsQueueForDialog = emptyList()) }
    }

    fun onSmsClicked(person: PersonUiModel) {
        _uiState.update { it.copy(personForSmsDialog = person) }
    }

    fun sendSmsReminder(person: PersonUiModel, selectedCard: String? = null) {
        smsManager.sendSmsReminder(person, selectedCard)
    }

    fun startBulkSms(selected: List<PersonUiModel>, selectedCard: String? = null) {
        if (selected.isEmpty()) return
        _uiState.update { it.copy(bulkSmsQueue = selected, currentBulkIndex = 0, selectedCardForBulk = selectedCard) }
    }

    fun processNextBulkSms() {
        val state = _uiState.value
        if (state.currentBulkIndex in state.bulkSmsQueue.indices) {
            val person = state.bulkSmsQueue[state.currentBulkIndex]
            val intent = smsManager.getSmsIntent(person, state.selectedCardForBulk)
            if (intent != null) {
                // We still need to start the activity. Since we have application context in manager, it uses FLAG_ACTIVITY_NEW_TASK.
                smsManager.sendSmsReminder(person, state.selectedCardForBulk)
                _uiState.update { it.copy(currentBulkIndex = it.currentBulkIndex + 1) }
            } else {
                skipBulkSms()
                processNextBulkSms()
            }
        } else {
            cancelBulkSms()
            viewModelScope.launch { _toastMessage.emit("ارسال گروهی به پایان رسید.") }
        }
    }

    fun skipBulkSms() {
        _uiState.update { 
            val nextIndex = it.currentBulkIndex + 1
            if (nextIndex >= it.bulkSmsQueue.size) {
                it.copy(bulkSmsQueue = emptyList(), currentBulkIndex = -1)
            } else {
                it.copy(currentBulkIndex = nextIndex)
            }
        }
    }

    fun cancelBulkSms() {
        _uiState.update { it.copy(bulkSmsQueue = emptyList(), currentBulkIndex = -1) }
    }

    fun checkContactsForMissingNumbers(persons: List<PersonUiModel>) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return

        viewModelScope.launch {
            val ignoredIds = settingsRepository.ignoredContactSuggestionsFlow.first()
            val personsWithoutPhone = persons.filter { 
                it.phoneNumber.isNullOrBlank() && !ignoredIds.contains(it.id) 
            }
            
            if (personsWithoutPhone.isEmpty()) {
                _uiState.update { it.copy(contactSuggestions = emptyList()) }
                return@launch
            }

            val suggestions = mutableListOf<ContactSuggestion>()
            personsWithoutPhone.forEach { person ->
                val matches = findSimilarContacts(person.name)
                if (matches.isNotEmpty()) {
                    suggestions.add(ContactSuggestion(person.id, person.name, matches))
                }
            }
            _uiState.update { it.copy(contactSuggestions = suggestions) }
        }
    }

    fun findSimilarContacts(name: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (cursor.moveToNext()) {
                if ((nameIndex != -1) && (numberIndex != -1)) {
                    val contactName = cursor.getString(nameIndex) ?: ""
                    val rawNumber = cursor.getString(numberIndex) ?: ""
                    
                    if (com.oqba26.monthlypaymentapp.utils.areNamesSimilar(name, contactName)) {
                        matches.add(ContactMatch(contactName, rawNumber.normalizePhoneNumber()))
                    }
                }
            }
        }
        return matches.distinctBy { it.phoneNumber }
    }

    fun confirmContactSuggestion(personId: String, personName: String, phoneNumber: String) {
        viewModelScope.launch {
            // دریافت اطلاعات فعلی شخص برای حفظ مقادیر دیگر
            val currentPerson = networkRepository.getPersonsFlow().first().find { it.id == personId }
            val commitment = currentPerson?.monthlyCommitment ?: 0.0
            val startMonth = currentPerson?.startMonth ?: 1
            val startYear = currentPerson?.startYear ?: 1403
            
            val statusCode = networkRepository.updatePerson(
                personId, 
                personName, 
                phoneNumber, 
                commitment,
                startMonth,
                startYear
            )
            if (statusCode == 200) {
                networkRepository.refresh()
                _uiState.update { state ->
                    state.copy(contactSuggestions = state.contactSuggestions.filter { it.personId != personId })
                }
            } else if (statusCode == 409) {
                _toastMessage.emit("خطا: این نام از قبل وجود دارد.")
            }
        }
    }

    fun dismissContactSuggestion(suggestion: ContactSuggestion) {
        viewModelScope.launch {
            settingsRepository.ignoreContactSuggestion(suggestion.personId)
            _uiState.update { state ->
                state.copy(contactSuggestions = state.contactSuggestions.filter { it.personId != suggestion.personId })
            }
        }
    }
}
