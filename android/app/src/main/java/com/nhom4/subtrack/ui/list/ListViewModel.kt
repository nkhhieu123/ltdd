package com.nhom4.subtrack.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom4.subtrack.SubTrackApp
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Subscription
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.data.Tag
import com.nhom4.subtrack.domain.BillingMath
import com.nhom4.subtrack.ui.label
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class SortOrder { DATE, PRICE, NAME }

data class ListFilters(
    val query: String = "",
    val status: SubscriptionStatus? = null,
    val category: Category? = null,
    val sort: SortOrder = SortOrder.DATE,
) {
    val isActive get() = query.isNotBlank() || status != null || category != null
}

data class ListUiState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val totalCount: Int = 0,
    val items: List<Subscription> = emptyList(),
    val tags: Map<Long, Tag> = emptyMap(),
    val currency: Currency = Currency.VND,
    val monthlyTotal: Double = 0.0,
    val otherCurrencyCount: Int = 0,
    val activeCount: Int = 0,
    val dueSoonCount: Int = 0,
)

class ListViewModel(private val app: SubTrackApp) : ViewModel() {
    private val repo = app.repository
    private val _filters = MutableStateFlow(ListFilters())
    val filters: StateFlow<ListFilters> = _filters
    private val error = MutableStateFlow(false)

    val state: StateFlow<ListUiState> = combine(
        repo.subscriptions, repo.tags, app.settings.settings, _filters, error,
    ) { subs, tags, settings, filters, hasError ->
        if (subs == null) return@combine ListUiState(loading = !hasError, error = hasError)
        val today = LocalDate.now()
        val tagMap = tags.associateBy { it.id }
        val active = subs.filter { it.status == SubscriptionStatus.ACTIVE }
        ListUiState(
            loading = false,
            totalCount = subs.size,
            items = subs.filter { matches(it, filters, tagMap) }.sortedWith(comparator(filters.sort)),
            tags = tagMap,
            currency = settings.defaultCurrency,
            monthlyTotal = BillingMath.totalMonthly(subs, settings.defaultCurrency),
            otherCurrencyCount = active.count { it.currency != settings.defaultCurrency },
            activeCount = active.size,
            dueSoonCount = active.count { BillingMath.daysUntil(it.nextPaymentDate, today) in 0..7 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListUiState())

    init {
        load()
    }

    fun load() {
        error.value = false
        viewModelScope.launch {
            runCatching { repo.refresh() }.onFailure { error.value = true }
        }
    }

    fun setQuery(q: String) = _filters.update { it.copy(query = q) }
    fun setStatus(s: SubscriptionStatus?) = _filters.update { it.copy(status = s) }
    fun setCategory(c: Category?) = _filters.update { it.copy(category = c) }
    fun setSort(s: SortOrder) = _filters.update { it.copy(sort = s) }
    fun clearFilters() = _filters.update { ListFilters(sort = it.sort) }

    private inline fun MutableStateFlow<ListFilters>.update(f: (ListFilters) -> ListFilters) {
        value = f(value)
    }

    private fun matches(sub: Subscription, f: ListFilters, tags: Map<Long, Tag>): Boolean {
        if (f.status != null && sub.status != f.status) return false
        if (f.category != null && sub.category != f.category) return false
        val q = f.query.trim()
        if (q.isEmpty()) return true
        val haystack = buildList {
            add(sub.name)
            sub.tagIds.mapNotNullTo(this) { tags[it]?.name }
            sub.platforms.mapTo(this) { app.getString(it.label) }
            add(app.getString(sub.category.label))
        }
        return haystack.any { it.contains(q, ignoreCase = true) }
    }

    private fun comparator(sort: SortOrder): Comparator<Subscription> = when (sort) {
        SortOrder.DATE -> compareBy<Subscription> { it.status != SubscriptionStatus.ACTIVE }.thenBy { it.nextPaymentDate }
        SortOrder.PRICE -> compareByDescending<Subscription> { BillingMath.monthlyCost(it.amount, it.cycle) }
        SortOrder.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
}
