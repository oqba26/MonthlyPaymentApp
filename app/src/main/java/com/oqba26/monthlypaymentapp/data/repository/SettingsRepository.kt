package com.oqba26.monthlypaymentapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val DEFAULT_PAYMENT_AMOUNT = doublePreferencesKey("default_payment_amount")
        val SELECTED_FONT_NAME = stringPreferencesKey("selected_font_name")
        // ⭐️ جدید: کلیدهای مربوط به احراز هویت
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")

        const val FALLBACK_AMOUNT = 200000.0
        const val DEFAULT_FONT = "Estedad"
    }

    // --- جریان‌های تنظیمات عمومی ---
    val defaultPaymentAmountFlow: Flow<Double> = dataStore.data.map {
        it[DEFAULT_PAYMENT_AMOUNT] ?: FALLBACK_AMOUNT
    }

    val selectedFontFlow: Flow<String> = dataStore.data.map {
        it[SELECTED_FONT_NAME] ?: DEFAULT_FONT
    }

    suspend fun saveDefaultPaymentAmount(amount: Double) {
        dataStore.edit { it[DEFAULT_PAYMENT_AMOUNT] = amount }
    }

    suspend fun saveSelectedFont(fontName: String) {
        dataStore.edit { it[SELECTED_FONT_NAME] = fontName }
    }


    // --- جریان‌ها و متدهای مربوط به احراز هویت ---
    val authTokenFlow: Flow<String?> = dataStore.data.map {
        it[AUTH_TOKEN_KEY]
    }

    val userIdFlow: Flow<String?> = dataStore.data.map {
        it[USER_ID_KEY]
    }

    /**
     * توکن و ID کاربر را ذخیره می‌کند. برای پاک کردن، null ارسال کنید.
     */
    suspend fun saveAuthData(token: String?, userId: String?) {
        dataStore.edit { preferences ->
            if (token != null) {
                preferences[AUTH_TOKEN_KEY] = token
            } else {
                preferences.remove(AUTH_TOKEN_KEY)
            }
            if (userId != null) {
                preferences[USER_ID_KEY] = userId
            } else {
                preferences.remove(USER_ID_KEY)
            }
        }
    }
}
