package com.nhom4.subtrack.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom4.subtrack.R
import com.nhom4.subtrack.SubTrackApp
import com.nhom4.subtrack.data.AppSettings
import com.nhom4.subtrack.data.BillingCycle
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Platform
import com.nhom4.subtrack.data.Subscription
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.domain.BillingMath
import com.nhom4.subtrack.domain.CsvWriter
import com.nhom4.subtrack.reminder.Reminders
import com.nhom4.subtrack.ui.formatAmountInput
import com.nhom4.subtrack.ui.formatDate
import com.nhom4.subtrack.ui.formatMoney
import com.nhom4.subtrack.ui.label
import com.nhom4.subtrack.ui.reports.ReportsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
    data class Share(val text: String) : SettingsEvent
}

class SettingsViewModel(private val app: SubTrackApp) : ViewModel() {
    private val repo = app.repository
    val settings: StateFlow<AppSettings> = app.settings.settings
    val subscriptionCount get() = repo.subscriptions.value?.size ?: 0

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { if (repo.subscriptions.value == null) runCatching { repo.refresh() } }
    }

    private fun msg(id: Int, vararg args: Any) = _events.trySend(SettingsEvent.Message(app.getString(id, *args)))

    fun setCurrency(c: Currency) = app.settings.setDefaultCurrency(c)

    fun setReminders(enabled: Boolean) {
        app.settings.setRemindersEnabled(enabled)
        if (enabled) {
            Reminders.schedule(app)
            Reminders.checkNow(app)
        } else {
            Reminders.cancel(app)
        }
    }

    fun sendTestNotification() {
        if (Reminders.notifyTest(app)) msg(R.string.test_notification_sent) else msg(R.string.notif_permission_denied)
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val subs = repo.subscriptions.value.orEmpty()
            val tags = repo.tags.value.associateBy { it.id }
            val result = runCatching {
                val rows = subs.sortedBy { it.name.lowercase() }.map { s ->
                    listOf(
                        s.name,
                        formatAmountInput(s.amount, s.currency),
                        s.currency.code,
                        app.getString(s.cycle.label),
                        formatDate(s.nextPaymentDate),
                        app.getString(s.category.label),
                        app.getString(s.status.label),
                        s.remindDaysBefore?.toString().orEmpty(),
                        s.tagIds.mapNotNull { tags[it]?.name }.joinToString("; "),
                        s.platforms.joinToString("; ") { app.getString(it.label) },
                        s.managementUrl.orEmpty(),
                        s.notes.orEmpty(),
                    )
                }
                val csv = CsvWriter.build(app.getString(R.string.csv_header), rows)
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")!!.use { out ->
                        // BOM để Excel nhận đúng tiếng Việt UTF-8
                        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }
                rows.size
            }
            result.onSuccess { n -> _events.send(SettingsEvent.Message(app.resources.getQuantityString(R.plurals.export_done, n, n))) }
                .onFailure { msg(R.string.export_failed) }
        }
    }

    fun shareReport() {
        val subs = repo.subscriptions.value.orEmpty()
        val currency = settings.value.defaultCurrency
        val report = ReportsViewModel.build(subs, currency)
        if (report.activeCount == 0) {
            msg(R.string.reports_empty)
            return
        }
        val text = buildString {
            appendLine(app.getString(R.string.share_report_header, formatDate(LocalDate.now())))
            appendLine(app.getString(R.string.share_report_totals, formatMoney(report.monthly, currency), formatMoney(report.yearly, currency)))
            appendLine(app.getString(R.string.share_report_count, report.activeCount))
            subs.filter { it.status == SubscriptionStatus.ACTIVE && it.currency == currency }
                .sortedByDescending { BillingMath.monthlyCost(it.amount, it.cycle) }
                .forEach {
                    appendLine(app.getString(R.string.share_report_line, it.name, formatMoney(it.amount, it.currency), app.getString(it.cycle.label)))
                }
        }
        _events.trySend(SettingsEvent.Share(text.trim()))
    }

    /** 5 gói minh họa, ghi chú rõ là dữ liệu mẫu. */
    fun addDemoData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val note = app.getString(R.string.demo_note)
            val demo = listOf(
                Subscription(name = "Netflix", amount = 260_000.0, currency = Currency.VND, cycle = BillingCycle.MONTHLY,
                    nextPaymentDate = today.plusDays(3), category = Category.ENTERTAINMENT,
                    platforms = listOf(Platform.SMART_TV, Platform.ANDROID), managementUrl = "https://www.netflix.com/account", notes = note),
                Subscription(name = "Spotify Premium", amount = 59_000.0, currency = Currency.VND, cycle = BillingCycle.MONTHLY,
                    nextPaymentDate = today.plusDays(12), category = Category.MUSIC, platforms = listOf(Platform.ANDROID, Platform.WINDOWS), notes = note),
                Subscription(name = "YouTube Premium", amount = 79_000.0, currency = Currency.VND, cycle = BillingCycle.MONTHLY,
                    nextPaymentDate = today.plusDays(20), category = Category.ENTERTAINMENT, platforms = listOf(Platform.ANDROID, Platform.WEB), notes = note),
                Subscription(name = "Google One 100 GB", amount = 450_000.0, currency = Currency.VND, cycle = BillingCycle.YEARLY,
                    nextPaymentDate = today.plusMonths(5), category = Category.STORAGE, platforms = listOf(Platform.ANDROID, Platform.WEB), notes = note),
                Subscription(name = "Duolingo Super", amount = 1_190_000.0, currency = Currency.VND, cycle = BillingCycle.YEARLY,
                    nextPaymentDate = today.plusMonths(8), category = Category.EDUCATION, platforms = listOf(Platform.ANDROID), notes = note),
            )
            runCatching { demo.forEach { repo.save(it) } }
                .onSuccess { msg(R.string.demo_added) }
                .onFailure { msg(R.string.save_failed) }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            runCatching { repo.clearAll() }
                .onSuccess { msg(R.string.cleared) }
                .onFailure { msg(R.string.error_generic) }
        }
    }
}
