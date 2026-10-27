package com.nhom4.subtrack.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val defaultCurrency: Currency = Currency.VND,
    val remindersEnabled: Boolean = true,
    val onboardingDone: Boolean = false,
)

/** Cài đặt người dùng lưu trong SharedPreferences. */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun read() = AppSettings(
        defaultCurrency = runCatching { Currency.valueOf(prefs.getString(KEY_CURRENCY, null) ?: "VND") }
            .getOrDefault(Currency.VND),
        remindersEnabled = prefs.getBoolean(KEY_REMINDERS, true),
        onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false),
    )

    fun setDefaultCurrency(currency: Currency) = edit { putString(KEY_CURRENCY, currency.name) }
    fun setRemindersEnabled(enabled: Boolean) = edit { putBoolean(KEY_REMINDERS, enabled) }
    fun setOnboardingDone() = edit { putBoolean(KEY_ONBOARDING, true) }

    /** Đánh dấu đã nhắc gói [subId] cho kỳ thanh toán [epochDay]; trả về false nếu đã nhắc rồi. */
    fun markReminded(subId: Long, epochDay: Long): Boolean {
        val key = "reminded_$subId"
        if (prefs.getLong(key, Long.MIN_VALUE) == epochDay) return false
        prefs.edit().putLong(key, epochDay).apply()
        return true
    }

    private inline fun edit(block: android.content.SharedPreferences.Editor.() -> Unit) {
        prefs.edit().apply(block).apply()
        _settings.value = read()
    }

    private companion object {
        const val KEY_CURRENCY = "default_currency"
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_ONBOARDING = "onboarding_done"
    }
}
