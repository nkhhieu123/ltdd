package com.nhom4.subtrack.ui.organize

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhom4.subtrack.SubTrackApp
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Platform
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.data.Tag
import com.nhom4.subtrack.domain.BillingMath
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagRow(val tag: Tag, val count: Int)
data class PlatformRow(val platform: Platform, val count: Int)
data class CategoryRow(val category: Category, val count: Int, val monthly: Double)

data class OrganizeUiState(
    val loading: Boolean = true,
    val tags: List<TagRow> = emptyList(),
    val platforms: List<PlatformRow> = emptyList(),
    val categories: List<CategoryRow> = emptyList(),
    val currency: Currency = Currency.VND,
)

class OrganizeViewModel(app: SubTrackApp) : ViewModel() {
    private val repo = app.repository

    val state: StateFlow<OrganizeUiState> = combine(repo.subscriptions, repo.tags, app.settings.settings) { subs, tags, settings ->
        if (subs == null) return@combine OrganizeUiState()
        val active = subs.filter { it.status == SubscriptionStatus.ACTIVE && it.currency == settings.defaultCurrency }
        OrganizeUiState(
            loading = false,
            tags = tags.map { t -> TagRow(t, subs.count { t.id in it.tagIds }) },
            platforms = Platform.entries.map { p -> PlatformRow(p, subs.count { p in it.platforms }) }
                .filter { it.count > 0 }.sortedByDescending { it.count },
            categories = Category.entries.map { c ->
                val inCat = active.filter { it.category == c }
                CategoryRow(c, inCat.size, inCat.sumOf { BillingMath.monthlyCost(it.amount, it.cycle) })
            }.filter { it.count > 0 }.sortedByDescending { it.monthly },
            currency = settings.defaultCurrency,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrganizeUiState())

    init {
        viewModelScope.launch { if (repo.subscriptions.value == null) runCatching { repo.refresh() } }
    }

    fun saveTag(tag: Tag, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(runCatching { repo.saveTag(tag) }.getOrDefault(false)) }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch { runCatching { repo.deleteTag(id) } }
    }
}
