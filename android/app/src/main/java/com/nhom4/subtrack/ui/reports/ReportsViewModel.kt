package com.nhom4.subtrack.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom4.subtrack.SubTrackApp
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Subscription
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.domain.BillingMath
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Slice(val label: String?, val monthly: Double, val share: Double)
data class CategoryTotal(val category: Category, val monthly: Double, val share: Double)

data class ReportsUiState(
    val loading: Boolean = true,
    val currency: Currency = Currency.VND,
    val monthly: Double = 0.0,
    val activeCount: Int = 0,
    val excludedCount: Int = 0,
    /** Tối đa 6 lát; lát cuối có label null = "Các gói khác". */
    val slices: List<Slice> = emptyList(),
    val categories: List<CategoryTotal> = emptyList(),
    val top: List<Subscription> = emptyList(),
) {
    val weekly get() = BillingMath.weeklyFromMonthly(monthly)
    val quarterly get() = BillingMath.quarterlyFromMonthly(monthly)
    val yearly get() = BillingMath.yearlyFromMonthly(monthly)
}

class ReportsViewModel(app: SubTrackApp) : ViewModel() {
    private val repo = app.repository
    /** null = dùng tiền tệ mặc định trong Cài đặt. */
    private val chosenCurrency = MutableStateFlow<Currency?>(null)

    val state: StateFlow<ReportsUiState> =
        combine(repo.subscriptions, app.settings.settings, chosenCurrency) { subs, settings, chosen ->
            if (subs == null) return@combine ReportsUiState()
            build(subs, chosen ?: settings.defaultCurrency)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    init {
        viewModelScope.launch { if (repo.subscriptions.value == null) runCatching { repo.refresh() } }
    }

    fun setCurrency(c: Currency) {
        chosenCurrency.value = c
    }

    companion object {
        fun build(subs: List<Subscription>, currency: Currency): ReportsUiState {
            val active = subs.filter { it.status == SubscriptionStatus.ACTIVE }
            val inCurrency = active.filter { it.currency == currency }
            val costs = inCurrency.map { it to BillingMath.monthlyCost(it.amount, it.cycle) }.sortedByDescending { it.second }
            val total = costs.sumOf { it.second }
            fun share(v: Double) = if (total > 0) v / total * 100 else 0.0

            val slices = if (costs.size <= 6) {
                costs.map { (s, m) -> Slice(s.name, m, share(m)) }
            } else {
                costs.take(5).map { (s, m) -> Slice(s.name, m, share(m)) } +
                    costs.drop(5).sumOf { it.second }.let { Slice(null, it, share(it)) }
            }
            val categories = inCurrency.groupBy { it.category }
                .map { (c, list) -> list.sumOf { BillingMath.monthlyCost(it.amount, it.cycle) }.let { CategoryTotal(c, it, share(it)) } }
                .sortedByDescending { it.monthly }

            return ReportsUiState(
                loading = false,
                currency = currency,
                monthly = total,
                activeCount = inCurrency.size,
                excludedCount = active.size - inCurrency.size,
                slices = slices,
                categories = categories,
                top = costs.take(3).map { it.first },
            )
        }
    }
}
