package com.oqba26.monthlypaymentapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.monthlypaymentapp.core.manager.BackupManager
import com.oqba26.monthlypaymentapp.core.manager.SnapshotInfo
import com.oqba26.monthlypaymentapp.data.model.BackupData
import com.oqba26.monthlypaymentapp.data.model.DeviceRecord
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localPersonRepository: LocalPersonRepository,
    private val backupManager: BackupManager,
    private val networkRepository: NetworkRepository,
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

    val reminderDay: StateFlow<Int?> = settingsRepository.reminderDayFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val cardNumbers: StateFlow<List<String>> = settingsRepository.cardNumbersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val isOperatorMode: StateFlow<Boolean> = settingsRepository.isOperatorModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialValue = false)

    val adminPin: StateFlow<String> = settingsRepository.adminPinFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "1234")

    val isMasterDevice: StateFlow<Boolean> = settingsRepository.isMasterDeviceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialValue = false)

    val masterKey: StateFlow<String> = settingsRepository.masterKeyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SettingsRepository.DEFAULT_MASTER_KEY)

    val allowOperatorBackup: StateFlow<Boolean> = settingsRepository.allowOperatorBackupFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialValue = false)

    val allowOperatorLogout: StateFlow<Boolean> = settingsRepository.allowOperatorLogoutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initialValue = false)

    val allDevices: StateFlow<List<DeviceRecord>> = networkRepository.getDevicesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    fun initDeviceSession(deviceId: String, deviceName: String) {
        viewModelScope.launch {
            val isMaster = settingsRepository.isMasterDeviceFlow.first()
            networkRepository.registerOrUpdateCurrentDevice(deviceId, deviceName, isMaster = isMaster)

            // همگام‌سازی زنده با رکوردهای سرور
            networkRepository.getDevicesFlow().collectLatest { devices ->
                val myDevice = devices.find { (_, dId) -> dId == deviceId }
                if (myDevice != null) {
                    val currentIsMaster = settingsRepository.isMasterDeviceFlow.first()
                    if (!currentIsMaster) {
                        // اگر دستگاه مستر نیست، وضعیتش از سرور خوانده می‌شود
                        val shouldBeOperator = !myDevice.isAdmin
                        settingsRepository.setOperatorMode(shouldBeOperator)
                        settingsRepository.setAllowOperatorBackup(myDevice.allowBackup)
                        settingsRepository.setAllowOperatorLogout(myDevice.allowLogout)
                    }
                }
            }
        }
    }

    fun verifyAndActivateMasterDevice(enteredKey: String, currentDeviceId: String, currentDeviceName: String) {
        viewModelScope.launch {
            if (enteredKey == masterKey.value) {
                settingsRepository.setMasterDevice(isMaster = true, currentDeviceId)
                settingsRepository.setOperatorMode(false)
                networkRepository.registerOrUpdateCurrentDevice(currentDeviceId, currentDeviceName, isMaster = true)
                _toastMessage.emit("این دستگاه به عنوان دستگاه مدیر ارشد تأیید و فعال شد")
            } else {
                _toastMessage.emit("رمز ارشد (Master Key) نادرست است")
            }
        }
    }

    fun toggleDeviceAdmin(device: DeviceRecord) {
        viewModelScope.launch {
            val newAdmin = !device.isAdmin
            networkRepository.updateDevicePermissions(device.id, isAdmin = newAdmin, allowBackup = device.allowBackup, allowLogout = device.allowLogout)
            val name = device.deviceName.ifBlank { "دستگاه" }
            _toastMessage.emit(if (newAdmin) "دسترسی مدیریت به $name داده شد" else "دسترسی مدیریت از $name گرفته شد")
        }
    }

    fun toggleDeviceBackup(device: DeviceRecord) {
        viewModelScope.launch {
            val newBackup = !device.allowBackup
            networkRepository.updateDevicePermissions(device.id, isAdmin = device.isAdmin, allowBackup = newBackup, allowLogout = device.allowLogout)
            val name = device.deviceName.ifBlank { "دستگاه" }
            _toastMessage.emit(if (newBackup) "دسترسی پشتیبان‌گیری برای $name فعال شد" else "دسترسی پشتیبان‌گیری برای $name غیرفعال شد")
        }
    }

    fun toggleDeviceLogout(device: DeviceRecord) {
        viewModelScope.launch {
            val newLogout = !device.allowLogout
            networkRepository.updateDevicePermissions(device.id, isAdmin = device.isAdmin, allowBackup = device.allowBackup, allowLogout = newLogout)
            val name = device.deviceName.ifBlank { "دستگاه" }
            _toastMessage.emit(if (newLogout) "دسترسی خروج از حساب برای $name فعال شد" else "دسترسی خروج از حساب برای $name غیرفعال شد")
        }
    }

    fun deleteDevice(device: DeviceRecord) {
        viewModelScope.launch {
            networkRepository.deleteDeviceRecord(device.id)
            _toastMessage.emit("دستگاه از لیست حذف شد")
        }
    }

    fun deactivateMasterDevice() {
        viewModelScope.launch {
            settingsRepository.setMasterDevice(isMaster = false)
            _toastMessage.emit("وضعیت مدیر ارشد از این دستگاه لغو شد")
        }
    }

    fun changeMasterKey(oldKey: String, newKey: String) {
        viewModelScope.launch {
            if (oldKey != masterKey.value) {
                _toastMessage.emit("رمز ارشد فعلی نادرست است")
                return@launch
            }
            if (newKey.length < 4) {
                _toastMessage.emit("رمز ارشد جدید باید حداقل ۴ کاراکتر باشد")
                return@launch
            }
            settingsRepository.setMasterKey(newKey)
            _toastMessage.emit("رمز ارشد با موفقیت به‌روزرسانی شد")
        }
    }

    fun setOperatorMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOperatorMode(enabled)
            _toastMessage.emit(if (enabled) "حالت اپراتور (محدود) فعال شد" else "حالت مدیریت فعال شد")
        }
    }

    fun setAdminPin(newPin: String) {
        viewModelScope.launch {
            settingsRepository.setAdminPin(newPin)
            _toastMessage.emit("رمز مدیریت با موفقیت تغییر یافت")
        }
    }

    fun saveReminderDay(day: String) {
        viewModelScope.launch {
            val dayInt = day.toIntOrNull()
            if (dayInt == null || dayInt in 1..31) {
                settingsRepository.saveReminderDay(dayInt)
                _toastMessage.emit(if (dayInt == null) "یادآور غیرفعال شد" else "یادآور برای روز $day ماه تنظیم شد")
            } else {
                _toastMessage.emit("روز وارد شده نامعتبر است (۱ تا ۳۱)")
            }
        }
    }

    fun addCardNumber(cardNumber: String) {
        viewModelScope.launch {
            if (cardNumber.isBlank()) return@launch
            val current = cardNumbers.value.toMutableList()
            if (!current.contains(cardNumber)) {
                current.add(cardNumber)
                settingsRepository.saveCardNumbers(current)
                _toastMessage.emit("شماره کارت اضافه شد")
            }
        }
    }

    fun removeCardNumber(cardNumber: String) {
        viewModelScope.launch {
            val current = cardNumbers.value.toMutableList()
            if (current.remove(cardNumber)) {
                settingsRepository.saveCardNumbers(current)
                _toastMessage.emit("شماره کارت حذف شد")
            }
        }
    }

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

    // ---------------------------------------------- نسخه‌های پشتیبان خودکار (تور نجات)

    private val _snapshots = MutableStateFlow<List<SnapshotInfo>>(emptyList())
    val snapshots: StateFlow<List<SnapshotInfo>> = _snapshots.asStateFlow()

    fun loadSnapshots() {
        viewModelScope.launch { _snapshots.value = backupManager.listSnapshots() }
    }

    fun restoreSnapshot(info: SnapshotInfo) {
        viewModelScope.launch {
            val ok = backupManager.restoreSnapshot(info.file)
            _toastMessage.emit(
                if (ok) "اطلاعات از نسخه‌ی خودکار بازگردانی شد. لطفاً برنامه را مجدد اجرا کنید."
                else "بازگردانی ناموفق بود؛ فایل پشتیبان سالم نیست."
            )
            loadSnapshots()
        }
    }

    fun restoreFromBackupJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val backupData = json.decodeFromString<BackupData>(jsonString)
                // قبل از جایگزینی، وضعیت فعلی نگه داشته می‌شود تا راه برگشت بسته نشود.
                backupManager.createSnapshot(BackupManager.REASON_BEFORE_RESTORE)
                localPersonRepository.restoreBackup(backupData)
                _toastMessage.emit("اطلاعات با موفقیت بازیابی شد! لطفاً برنامه را مجدد اجرا کنید.")
            } catch (_: Exception) {
                _toastMessage.emit("خطا در بازیابی اطلاعات: فایل نامعتبر است.")
            }
        }
    }
}