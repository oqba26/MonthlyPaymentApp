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
        val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
        val USER_ID_KEY = stringPreferencesKey("user_id")
        val REMINDER_DAY = stringPreferencesKey("reminder_day")
        val CARD_NUMBERS = stringPreferencesKey("card_numbers")
        val IGNORED_CONTACT_SUGGESTIONS = stringPreferencesKey("ignored_contact_suggestions")

        const val FALLBACK_AMOUNT = 200000.0
        const val DEFAULT_FONT = "Estedad"
    }

    val reminderDayFlow: Flow<Int?> = dataStore.data.map {
        it[REMINDER_DAY]?.toIntOrNull()
    }

    val cardNumbersFlow: Flow<List<String>> = dataStore.data.map {
        val raw = it[CARD_NUMBERS] ?: ""
        if (raw.isBlank()) emptyList() else raw.split("|")
    }

    suspend fun saveCardNumbers(cards: List<String>) {
        dataStore.edit { it[CARD_NUMBERS] = cards.joinToString("|") }
    }

    suspend fun saveReminderDay(day: Int?) {
        dataStore.edit { preferences ->
            if (day != null) {
                preferences[REMINDER_DAY] = day.toString()
            } else {
                preferences.remove(REMINDER_DAY)
            }
        }
    }

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

    val authTokenFlow: Flow<String?> = dataStore.data.map {
        it[AUTH_TOKEN_KEY]
    }

    @Suppress("unused")
    val userIdFlow: Flow<String?> = dataStore.data.map {
        it[USER_ID_KEY]
    }

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

    val ignoredContactSuggestionsFlow: Flow<Set<String>> = dataStore.data.map {
        val raw = it[IGNORED_CONTACT_SUGGESTIONS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").toSet()
    }

    suspend fun ignoreContactSuggestion(personId: String) {
        dataStore.edit { preferences ->
            val current = preferences[IGNORED_CONTACT_SUGGESTIONS] ?: ""
            val set = if (current.isBlank()) mutableSetOf() else current.split(",").toMutableSet()
            set.add(personId)
            preferences[IGNORED_CONTACT_SUGGESTIONS] = set.joinToString(",")
        }
    }
}
