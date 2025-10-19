package com.oqba26.monthlypaymentapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.monthlypaymentapp.data.model.BackupData
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val localPersonRepository: LocalPersonRepository
) : ViewModel() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        // اگر JSON قدیمی بود و کلیدی نباشد، مقدار پیش‌فرض اعمال می‌شود
    }

    val defaultPaymentAmount: StateFlow<Double> = settingsRepository.defaultPaymentAmountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SettingsRepository.FALLBACK_AMOUNT)

    val selectedFont: StateFlow<String> = settingsRepository.selectedFontFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SettingsRepository.DEFAULT_FONT)

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun saveDefaultPaymentAmount(amount: String) {
        viewModelScope.launch {
            val amountAsDouble = amount.toDoubleOrNull() ?: return@launch
            settingsRepository.saveDefaultPaymentAmount(amountAsDouble)
            _toastMessage.emit("مبلغ پیش‌فرض ذخیره شد")
        }
    }

    fun onFontSelected(fontName: String) {
        viewModelScope.launch {
            settingsRepository.saveSelectedFont(fontName)
            _toastMessage.emit("فونت برنامه به $fontName تغییر یافت. برای مشاهده تغییرات برنامه را مجدد اجرا کنید.")
        }
    }

    suspend fun createBackupJsonSuspend(): String {
        val backupData = localPersonRepository.getDataForBackup()
        return json.encodeToString(backupData)
    }

    fun restoreFromBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val backupData = json.decodeFromString<BackupData>(jsonString)
                localPersonRepository.restoreBackup(backupData)
                _toastMessage.emit("اطلاعات با موفقیت بازیابی شد! لطفاً برنامه را مجدد اجرا کنید.")
            } catch (e: Exception) {
                _toastMessage.emit("خطا در بازیابی اطلاعات: فایل نامعتبر است.")
            }
        }
    }
}