@file:OptIn(ExperimentalMaterial3Api::class)

package com.nhom4.subtrack.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhom4.subtrack.R
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.domain.BillingMath
import com.nhom4.subtrack.ui.AppViewModels
import com.nhom4.subtrack.ui.EmptyState
import com.nhom4.subtrack.ui.LoadingState
import com.nhom4.subtrack.ui.SectionHeader
import com.nhom4.subtrack.ui.SelectField
import com.nhom4.subtrack.ui.formatMoney
import com.nhom4.subtrack.ui.formatPercent
import com.nhom4.subtrack.ui.label

private val SLICE_COLORS = listOf(
    Color(0xFF6C4CF1), Color(0xFF1FA99C), Color(0xFFFF7A59),
    Color(0xFFF2A900), Color(0xFF0090FF), Color(0xFF8B8D98),
)

@Composable
fun ReportsScreen(vm: ReportsViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.reports_title), fontWeight = FontWeight.Bold) }, windowInsets = WindowInsets(0))
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SelectField(
                    label = stringResource(R.string.report_currency),
                    value = state.currency,
                    options = Currency.entries,
                    optionLabel = { stringResource(it.label) },
                    onSelect = vm::setCurrency,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (state.excludedCount > 0) {
                item {
                    Text(
                        stringResource(R.string.reports_currency_note, state.currency.code, state.excludedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.activeCount == 0) {
                item { EmptyState(Icons.Outlined.PieChart, stringResource(R.string.reports_empty), null) }
                return@LazyColumn
            }
            item { TotalsGrid(state) }
            item {
                Text(stringResource(R.string.report_rule), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { BreakdownCard(state) }
            item { SectionHeader(stringResource(R.string.by_category_title)) }
            item { CategoryCard(state) }
            item { SectionHeader(stringResource(R.string.top_title)) }
            item { TopCard(state) }
        }
    }
}

@Composable
private fun TotalsGrid(state: ReportsUiState) {
    val cells = listOf(
        Triple(R.string.report_weekly, state.weekly, Color(0xFF0090FF)),
        Triple(R.string.report_monthly, state.monthly, Color(0xFF30A46C)),
        Triple(R.string.report_quarterly, state.quarterly, Color(0xFFF76B15)),
        Triple(R.string.report_yearly, state.yearly, Color(0xFFE5484D)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value, color) ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(vertical = 14.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(label), color = color, fontWeight = FontWeight.SemiBold)
                        Text(
                            formatMoney(value, state.currency),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WhiteCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) { content() }
    }
}

@Composable
private fun BreakdownCard(state: ReportsUiState) {
    val chartDesc = stringResource(R.string.chart_description)
    val othersLabel = stringResource(R.string.others)
    WhiteCard {
        Text(stringResource(R.string.breakdown_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(200.dp).semantics { contentDescription = chartDesc }) {
                val stroke = size.minDimension * 0.22f
                val diameter = size.minDimension - stroke
                var start = -90f
                state.slices.forEachIndexed { i, s ->
                    val sweep = (s.share / 100.0 * 360.0).toFloat()
                    drawArc(
                        color = SLICE_COLORS[i % SLICE_COLORS.size],
                        startAngle = start,
                        sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke),
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.report_monthly), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(state.monthly, state.currency), fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        Spacer(Modifier.height(16.dp))
        state.slices.forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(SLICE_COLORS[i % SLICE_COLORS.size]))
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.label ?: othersLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.percent_value, formatPercent(s.share)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.per_month, formatMoney(s.monthly, state.currency)), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CategoryCard(state: ReportsUiState) {
    WhiteCard {
        state.categories.forEachIndexed { i, c ->
            if (i > 0) Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(c.category.label), Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Text(formatMoney(c.monthly, state.currency), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (c.share / 100).toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(c.category.color),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
        }
    }
}

@Composable
private fun TopCard(state: ReportsUiState) {
    WhiteCard {
        state.top.forEachIndexed { i, sub ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Column(Modifier.weight(1f)) {
                    Text(sub.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(
                        formatMoney(sub.amount, sub.currency) + " · " + stringResource(sub.cycle.label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.per_month, formatMoney(BillingMath.monthlyCost(sub.amount, sub.cycle), sub.currency)))
            }
        }
    }
}
