@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nhom4.subtrack.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhom4.subtrack.R
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Subscription
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.data.Tag
import com.nhom4.subtrack.domain.BillingMath
import com.nhom4.subtrack.domain.RenewalLevel
import com.nhom4.subtrack.ui.AppViewModels
import com.nhom4.subtrack.ui.ColorChip
import com.nhom4.subtrack.ui.EmptyState
import com.nhom4.subtrack.ui.ErrorState
import com.nhom4.subtrack.ui.LoadingState
import com.nhom4.subtrack.ui.formatDate
import com.nhom4.subtrack.ui.formatMoney
import com.nhom4.subtrack.ui.label
import com.nhom4.subtrack.ui.theme.MonthOrange
import com.nhom4.subtrack.ui.theme.SoonRed

@Composable
fun ListScreen(
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    vm: ListViewModel = viewModel(factory = AppViewModels.Factory),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.list_title), fontWeight = FontWeight.Bold) },
                actions = { SortMenu(filters.sort, vm::setSort) },
                scrollBehavior = scroll,
                windowInsets = WindowInsets(0),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_subscription)) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        },
    ) { padding ->
        when {
            state.error -> ErrorState(vm::load, Modifier.padding(padding))
            state.loading -> LoadingState(Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.totalCount > 0) {
                    item { SummaryCard(state) }
                    item {
                        SearchAndFilters(
                            filters = filters,
                            onQuery = vm::setQuery,
                            onStatus = vm::setStatus,
                            onCategory = vm::setCategory,
                        )
                    }
                }
                when {
                    state.totalCount == 0 -> item {
                        EmptyState(
                            Icons.Outlined.Inbox,
                            stringResource(R.string.empty_title),
                            stringResource(R.string.empty_body),
                        )
                    }
                    state.items.isEmpty() -> item {
                        EmptyState(Icons.Outlined.SearchOff, stringResource(R.string.empty_search), null) {
                            TextButton(onClick = vm::clearFilters) { Text(stringResource(R.string.clear_filters)) }
                        }
                    }
                    else -> items(state.items, key = { it.id }) { sub ->
                        SubscriptionRow(sub, state.tags, onClick = { onOpen(sub.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(current: SortOrder, onSelect: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.action_sort))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf(
                SortOrder.DATE to R.string.sort_by_date,
                SortOrder.PRICE to R.string.sort_by_price,
                SortOrder.NAME to R.string.sort_by_name,
            ).forEach { (order, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    leadingIcon = { if (order == current) Icon(Icons.Filled.Check, null) else Spacer(Modifier.size(24.dp)) },
                    onClick = {
                        onSelect(order)
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(state: ListUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(stringResource(R.string.summary_monthly), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
            Text(
                formatMoney(state.monthlyTotal, state.currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            if (state.otherCurrencyCount > 0) {
                Text(
                    stringResource(R.string.other_currency_note, state.otherCurrencyCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryStat(state.activeCount.toString(), stringResource(R.string.summary_active), Modifier.weight(1f))
                SummaryStat(state.dueSoonCount.toString(), stringResource(R.string.summary_due), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary, maxLines = 2)
    }
}

@Composable
private fun SearchAndFilters(
    filters: ListFilters,
    onQuery: (String) -> Unit,
    onStatus: (SubscriptionStatus?) -> Unit,
    onCategory: (Category?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = filters.query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (filters.query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Filled.Clear, stringResource(R.string.action_clear_search))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filters.status == null, onClick = { onStatus(null) }, label = { Text(stringResource(R.string.filter_all)) })
            SubscriptionStatus.entries.forEach { s ->
                FilterChip(
                    selected = filters.status == s,
                    onClick = { onStatus(if (filters.status == s) null else s) },
                    label = { Text(stringResource(s.label)) },
                )
            }
            CategoryFilter(filters.category, onCategory)
        }
    }
}

@Composable
private fun CategoryFilter(current: Category?, onSelect: (Category?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = current != null,
            onClick = { open = true },
            label = { Text(if (current == null) stringResource(R.string.filter_category) else stringResource(current.label)) },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.filter_all_categories)) }, onClick = {
                onSelect(null)
                open = false
            })
            Category.entries.forEach { c ->
                DropdownMenuItem(text = { Text(stringResource(c.label)) }, onClick = {
                    onSelect(c)
                    open = false
                })
            }
        }
    }
}

@Composable
private fun SubscriptionRow(sub: Subscription, tags: Map<Long, Tag>, onClick: () -> Unit) {
    val active = sub.status == SubscriptionStatus.ACTIVE
    val days = BillingMath.daysUntil(sub.nextPaymentDate)
    val indicator = when {
        !active -> MaterialTheme.colorScheme.outlineVariant
        else -> when (BillingMath.renewalLevel(sub.nextPaymentDate)) {
            RenewalLevel.SOON -> SoonRed
            RenewalLevel.THIS_MONTH -> MonthOrange
            RenewalLevel.LATER -> Color.Transparent
        }
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Vạch báo hiệu gia hạn: đỏ ≤ 7 ngày, cam ≤ 30 ngày
            Box(Modifier.width(5.dp).fillMaxHeight().background(indicator))
            Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        sub.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        stringResource(sub.cycle.label) + " · " + stringResource(sub.category.label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (active) stringResource(R.string.next_payment, formatDate(sub.nextPaymentDate)) + " · " + dueText(days)
                        else stringResource(sub.status.label),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (active && days <= 7) SoonRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val chipTags = sub.tagIds.mapNotNull { tags[it] }
                    if (chipTags.isNotEmpty() || sub.platforms.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            chipTags.forEach { ColorChip(it.name, Color(it.color)) }
                            sub.platforms.forEach { ColorChip(stringResource(it.label), MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    formatMoney(sub.amount, sub.currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun dueText(days: Long): String = when (days) {
    0L -> stringResource(R.string.due_today)
    1L -> stringResource(R.string.due_tomorrow)
    else -> pluralStringResource(R.plurals.due_in_days, days.toInt(), days.toInt())
}
