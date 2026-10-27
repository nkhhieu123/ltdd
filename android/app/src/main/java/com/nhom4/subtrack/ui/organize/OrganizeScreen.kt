@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nhom4.subtrack.ui.organize

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhom4.subtrack.R
import com.nhom4.subtrack.data.Tag
import com.nhom4.subtrack.ui.AppViewModels
import com.nhom4.subtrack.ui.EmptyState
import com.nhom4.subtrack.ui.LoadingState
import com.nhom4.subtrack.ui.formatMoney
import com.nhom4.subtrack.ui.label

val TAG_COLORS = listOf(
    0xFFE5484D, 0xFFF76B15, 0xFFF2A900, 0xFF30A46C, 0xFF12A594,
    0xFF0090FF, 0xFF6C4CF1, 0xFFD6409F, 0xFF8B8D98,
)

@Composable
fun OrganizeScreen(vm: OrganizeViewModel = viewModel(factory = AppViewModels.Factory)) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<Tag?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Tag?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.organize_title), fontWeight = FontWeight.Bold) },
                    windowInsets = WindowInsets(0),
                )
                PrimaryTabRow(selectedTabIndex = tab) {
                    listOf(R.string.organize_tab_tags, R.string.organize_tab_platforms, R.string.organize_tab_categories)
                        .forEachIndexed { i, label ->
                            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(stringResource(label), maxLines = 1) })
                        }
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                FloatingActionButton(onClick = { creating = true }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.tag_dialog_add))
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (tab) {
                0 -> if (state.tags.isEmpty()) {
                    item { EmptyState(Icons.Outlined.Label, stringResource(R.string.tags_empty), null) }
                } else items(state.tags, key = { it.tag.id }) { row ->
                    RowCard(
                        dot = Color(row.tag.color),
                        title = row.tag.name,
                        subtitle = pluralStringResource(R.plurals.count_subscriptions, row.count, row.count),
                    ) {
                        IconButton(onClick = { editing = row.tag }) { Icon(Icons.Outlined.Edit, stringResource(R.string.action_edit)) }
                        IconButton(onClick = { deleting = row.tag }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete)) }
                    }
                }
                1 -> {
                    item { Hint(stringResource(R.string.platforms_hint)) }
                    if (state.platforms.isEmpty()) {
                        item { EmptyState(Icons.Outlined.Devices, stringResource(R.string.organize_nothing), null) }
                    } else items(state.platforms, key = { it.platform.key }) { row ->
                        RowCard(null, stringResource(row.platform.label), pluralStringResource(R.plurals.count_subscriptions, row.count, row.count))
                    }
                }
                else -> {
                    item { Hint(stringResource(R.string.categories_hint)) }
                    if (state.categories.isEmpty()) {
                        item { EmptyState(Icons.Outlined.Category, stringResource(R.string.organize_nothing), null) }
                    } else items(state.categories, key = { it.category.key }) { row ->
                        RowCard(
                            dot = Color(row.category.color),
                            title = stringResource(row.category.label),
                            subtitle = pluralStringResource(R.plurals.count_subscriptions, row.count, row.count),
                        ) {
                            Text(
                                stringResource(R.string.per_month, formatMoney(row.monthly, state.currency)),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        val tag = editing
        TagDialog(
            initialName = tag?.name.orEmpty(),
            initialColor = tag?.color,
            title = stringResource(if (tag == null) R.string.tag_dialog_add else R.string.tag_dialog_edit),
            onDismiss = { creating = false; editing = null },
            onConfirm = { name, color, done ->
                vm.saveTag(Tag(id = tag?.id ?: 0, name = name.trim(), color = color)) { ok ->
                    done(ok)
                    if (ok) { creating = false; editing = null }
                }
            },
        )
    }

    deleting?.let { tag ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.delete_tag_title)) },
            text = { Text(stringResource(R.string.delete_tag_body, tag.name)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteTag(tag.id); deleting = null }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun RowCard(dot: Color?, title: String, subtitle: String, trailing: @Composable () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (dot != null) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.size(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing()
        }
    }
}

/** Hộp thoại tạo/sửa tag. [onConfirm] nhận callback báo thành công hay trùng tên. */
@Composable
fun TagDialog(
    initialName: String,
    initialColor: Long?,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Long, done: (Boolean) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableLongStateOf(initialColor ?: TAG_COLORS.first()) }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40); error = null },
                    label = { Text(stringResource(R.string.tag_name)) },
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.tag_color), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TAG_COLORS.forEach { c ->
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(c))
                                .then(if (c == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .clickable { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (c == color) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) error = R.string.err_tag_name_required
                else onConfirm(name, color) { ok -> if (!ok) error = R.string.err_tag_exists }
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
