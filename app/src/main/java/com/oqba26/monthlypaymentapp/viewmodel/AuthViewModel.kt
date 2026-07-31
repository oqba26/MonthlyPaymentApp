package com.oqba26.monthlypaymentapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AuthState {
    data object Loading : AuthState
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val networkRepository: NetworkRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage.asSharedFlow()

    val authState: StateFlow<AuthState> = settingsRepository.authTokenFlow
        .map { token ->
            if (token != null) AuthState.Authenticated else AuthState.Unauthenticated
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

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

    fun logout() = viewModelScope.launch {
        settingsRepository.saveAuthData(null, null)
        _toastMessage.emit("از حساب کاربری خارج شدید.")
    }
}
