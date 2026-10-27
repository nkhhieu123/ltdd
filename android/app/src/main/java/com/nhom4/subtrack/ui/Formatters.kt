package com.nhom4.subtrack.ui

import androidx.annotation.StringRes
import com.nhom4.subtrack.R
import com.nhom4.subtrack.data.BillingCycle
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Platform
import com.nhom4.subtrack.data.SubscriptionStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val VietnameseLocale: Locale = Locale.forLanguageTag("vi-VN")

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", VietnameseLocale)

fun formatDate(date: LocalDate): String = date.format(dateFormatter)

/** 260000 VND → "260.000 ₫"; 9.99 USD → "9,99 US$" theo quy ước vi-VN. */
fun formatMoney(amount: Double, currency: Currency): String {
    val nf = NumberFormat.getCurrencyInstance(VietnameseLocale)
    nf.currency = java.util.Currency.getInstance(currency.code)
    nf.maximumFractionDigits = if (currency == Currency.VND) 0 else 2
    nf.minimumFractionDigits = if (currency == Currency.VND) 0 else 2
    return nf.format(amount)
}

/** Số nhập vào ô số tiền khi sửa (không ký hiệu tiền tệ). */
fun formatAmountInput(amount: Double, currency: Currency): String =
    if (currency == Currency.VND) amount.toLong().toString()
    else String.format(Locale.US, "%.2f", amount)

fun formatPercent(value: Double): String = String.format(VietnameseLocale, "%.1f", value)

@get:StringRes
val BillingCycle.label: Int
    get() = when (this) {
        BillingCycle.WEEKLY -> R.string.cycle_weekly
        BillingCycle.MONTHLY -> R.string.cycle_monthly
        BillingCycle.QUARTERLY -> R.string.cycle_quarterly
        BillingCycle.YEARLY -> R.string.cycle_yearly
    }

@get:StringRes
val SubscriptionStatus.label: Int
    get() = when (this) {
        SubscriptionStatus.ACTIVE -> R.string.status_active
        SubscriptionStatus.PAUSED -> R.string.status_paused
        SubscriptionStatus.CANCELLED -> R.string.status_cancelled
    }

@get:StringRes
val Category.label: Int
    get() = when (this) {
        Category.ENTERTAINMENT -> R.string.category_entertainment
        Category.MUSIC -> R.string.category_music
        Category.EDUCATION -> R.string.category_education
        Category.STORAGE -> R.string.category_storage
        Category.WORK -> R.string.category_work
        Category.UTILITIES -> R.string.category_utilities
        Category.OTHER -> R.string.category_other
    }

@get:StringRes
val Platform.label: Int
    get() = when (this) {
        Platform.ANDROID -> R.string.platform_android
        Platform.IPHONE -> R.string.platform_iphone
        Platform.IPAD -> R.string.platform_ipad
        Platform.TABLET -> R.string.platform_tablet
        Platform.WEB -> R.string.platform_web
        Platform.WINDOWS -> R.string.platform_windows
        Platform.MAC -> R.string.platform_mac
        Platform.SMART_TV -> R.string.platform_smart_tv
        Platform.PLAYSTATION -> R.string.platform_playstation
        Platform.XBOX -> R.string.platform_xbox
        Platform.NINTENDO -> R.string.platform_nintendo
        Platform.OTHER -> R.string.platform_other
    }

@get:StringRes
val Currency.label: Int
    get() = when (this) {
        Currency.VND -> R.string.currency_vnd
        Currency.USD -> R.string.currency_usd
        Currency.EUR -> R.string.currency_eur
    }
