@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nhom4.subtrack.ui.edit

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhom4.subtrack.R
import com.nhom4.subtrack.data.BillingCycle
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Platform
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.ui.AppViewModels
import com.nhom4.subtrack.ui.EmptyState
import com.nhom4.subtrack.ui.LoadingState
import com.nhom4.subtrack.ui.SectionHeader
import com.nhom4.subtrack.ui.SelectField
import com.nhom4.subtrack.ui.formatDate
import com.nhom4.subtrack.ui.formatMoney
import com.nhom4.subtrack.ui.label
import com.nhom4.subtrack.ui.organize.TagDialog
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

private val REMIND_OPTIONS: List<Int?> = listOf(null, 0, 1, 3, 7, 30)

@Composable
fun EditScreen(
    subscriptionId: Long,
    onDone: () -> Unit,
    vm: EditViewModel = viewModel(key = "edit-$subscriptionId", factory = AppViewModels.edit(subscriptionId)),
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is EditEvent.Message -> scope.launch { snackbar.showSnackbar(context.getString(event.text)) }
                EditEvent.Done -> onDone()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.isNew) R.string.edit_title_add else R.string.edit_title_edit)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (!vm.isNew && !vm.notFound) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete))
                        }
                    }
                    TextButton(onClick = vm::save, enabled = !vm.saving && !vm.loading && !vm.notFound) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { padding ->
        when {
            vm.loading -> LoadingState(Modifier.padding(padding))
            vm.notFound -> EmptyState(Icons.Outlined.EventBusy, stringResource(R.string.not_found), null, Modifier.padding(padding))
            else -> EditForm(vm, Modifier.padding(padding))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, vm.form.name)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun EditForm(vm: EditViewModel, modifier: Modifier) {
    val f = vm.form
    val e = vm.errors
    val context = LocalContext.current
    val tags by vm.tags.collectAsStateWithLifecycle()
    var pickDate by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        SectionHeader(stringResource(R.string.section_details))
        FormCard {
            OutlinedTextField(
                value = f.name,
                onValueChange = { v -> vm.update { it.copy(name = v) } },
                label = { Text(stringResource(R.string.field_name)) },
                placeholder = { Text(stringResource(R.string.field_name_placeholder)) },
                isError = e.name != null,
                supportingText = e.name?.let { { Text(stringResource(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = f.amount,
                    onValueChange = { v -> vm.update { it.copy(amount = v) } },
                    label = { Text(stringResource(R.string.field_amount)) },
                    placeholder = {
                        Text(stringResource(if (f.currency == Currency.VND) R.string.field_amount_placeholder_vnd else R.string.field_amount_placeholder_other))
                    },
                    isError = e.amount != null,
                    supportingText = e.amount?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1.4f),
                )
                SelectField(
                    label = stringResource(R.string.field_currency),
                    value = f.currency,
                    options = Currency.entries,
                    optionLabel = { it.code },
                    onSelect = { c -> vm.update { it.copy(currency = c) } },
                    modifier = Modifier.weight(1f),
                )
            }
            SelectField(
                label = stringResource(R.string.field_cycle),
                value = f.cycle,
                options = BillingCycle.entries,
                optionLabel = { stringResource(it.label) },
                onSelect = { c -> vm.update { it.copy(cycle = c) } },
                modifier = Modifier.fillMaxWidth(),
            )
            vm.monthlyPreview()?.let {
                Text(
                    stringResource(R.string.monthly_equivalent, formatMoney(it, f.currency)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.field_next_date) + ": " + formatDate(f.nextPaymentDate))
            }
            SelectField(
                label = stringResource(R.string.field_category),
                value = f.category,
                options = Category.entries,
                optionLabel = { stringResource(it.label) },
                onSelect = { c -> vm.update { it.copy(category = c) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.field_status), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SubscriptionStatus.entries.forEachIndexed { i, s ->
                    SegmentedButton(
                        selected = f.status == s,
                        onClick = { vm.update { it.copy(status = s) } },
                        shape = SegmentedButtonDefaults.itemShape(i, SubscriptionStatus.entries.size),
                        label = { Text(stringResource(s.label), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                    )
                }
            }
        }

        SectionHeader(stringResource(R.string.section_reminder))
        FormCard {
            SelectField(
                label = stringResource(R.string.field_remind),
                value = f.remindDaysBefore,
                options = REMIND_OPTIONS,
                optionLabel = { remindLabel(it) },
                onSelect = { d -> vm.update { it.copy(remindDaysBefore = d) } },
                modifier = Modifier.fillMaxWidth(),
            )
            if (!vm.remindersEnabled) {
                Text(stringResource(R.string.reminders_off_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            OutlinedButton(
                onClick = { addToCalendar(context, f) },
                enabled = f.name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.CalendarMonth, null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.add_to_calendar))
            }
        }

        SectionHeader(stringResource(R.string.section_organization))
        FormCard {
            Text(stringResource(R.string.field_tags), style = MaterialTheme.typography.labelLarge)
            if (tags.isEmpty()) {
                Text(stringResource(R.string.no_tags_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    val selected = tag.id in f.tagIds
                    FilterChip(
                        selected = selected,
                        onClick = { vm.toggleTag(tag.id) },
                        label = { Text(tag.name) },
                        leadingIcon = if (selected) ({ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }) else null,
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(tag.color).copy(alpha = 0.25f)),
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { newTag = true },
                    label = { Text(stringResource(R.string.new_tag)) },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) },
                )
            }
            Text(stringResource(R.string.field_platforms), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Platform.entries.forEach { p ->
                    val selected = p in f.platforms
                    FilterChip(
                        selected = selected,
                        onClick = { vm.togglePlatform(p) },
                        label = { Text(stringResource(p.label)) },
                        leadingIcon = if (selected) ({ Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) }) else null,
                    )
                }
            }
        }

        SectionHeader(stringResource(R.string.section_more))
        FormCard {
            OutlinedTextField(
                value = f.url,
                onValueChange = { v -> vm.update { it.copy(url = v) } },
                label = { Text(stringResource(R.string.field_url)) },
                placeholder = { Text(stringResource(R.string.field_url_placeholder)) },
                isError = e.url != null,
                supportingText = e.url?.let { { Text(stringResource(it)) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
            if (f.url.isNotBlank() && e.url == null) {
                TextButton(onClick = { openUrl(context, f.url.trim()) }) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.open_url))
                }
            }
            OutlinedTextField(
                value = f.notes,
                onValueChange = { v -> vm.update { it.copy(notes = v) } },
                label = { Text(stringResource(R.string.field_notes)) },
                placeholder = { Text(stringResource(R.string.field_notes_placeholder)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = f.nextPaymentDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        vm.update { it.copy(nextPaymentDate = date) }
                    }
                    pickDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = state, title = { Text(stringResource(R.string.pick_date), Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }

    if (newTag) {
        TagDialog(
            initialName = "",
            initialColor = null,
            title = stringResource(R.string.tag_dialog_add),
            onDismiss = { newTag = false },
            onConfirm = { name, color, done -> vm.createTag(name, color) { ok -> done(ok); if (ok) newTag = false } },
        )
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.Start) {
            content()
        }
    }
}

@Composable
private fun remindLabel(days: Int?): String = when (days) {
    null -> stringResource(R.string.remind_none)
    0 -> stringResource(R.string.remind_same_day)
    else -> pluralStringResource(R.plurals.remind_days_before, days, days)
}

private fun addToCalendar(context: Context, f: EditForm) {
    val start = f.nextPaymentDate.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val rule = when (f.cycle) {
        BillingCycle.WEEKLY -> "FREQ=WEEKLY"
        BillingCycle.MONTHLY -> "FREQ=MONTHLY"
        BillingCycle.QUARTERLY -> "FREQ=MONTHLY;INTERVAL=3"
        BillingCycle.YEARLY -> "FREQ=YEARLY"
    }
    val amountText = f.amount.ifBlank { "0" } + " " + f.currency.code
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, context.getString(R.string.calendar_event_title, f.name.trim()))
        .putExtra(CalendarContract.Events.DESCRIPTION, context.getString(R.string.calendar_event_desc, amountText, context.getString(f.cycle.label)))
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
        .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
        .putExtra(CalendarContract.Events.RRULE, rule)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, R.string.no_calendar_app, android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, R.string.no_browser, android.widget.Toast.LENGTH_SHORT).show()
    }
}
