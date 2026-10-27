@file:OptIn(ExperimentalMaterial3Api::class)

package com.nhom4.subtrack.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhom4.subtrack.BuildConfigInfo
import com.nhom4.subtrack.R
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.reminder.Reminders
import com.nhom4.subtrack.ui.AppViewModels
import com.nhom4.subtrack.ui.SectionHeader
import com.nhom4.subtrack.ui.SelectField
import com.nhom4.subtrack.ui.label
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel(factory = AppViewModels.Factory)) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }
    val shareTitle = stringResource(R.string.share_chooser)
    val shareSubject = stringResource(R.string.share_report_subject)
    val exportEmpty = stringResource(R.string.export_empty)
    val permissionDenied = stringResource(R.string.notif_permission_denied)

    LaunchedEffect(vm) {
        vm.events.collect { e ->
            when (e) {
                is SettingsEvent.Message -> scope.launch { snackbar.showSnackbar(e.text) }
                is SettingsEvent.Share -> context.startActivity(
                    Intent.createChooser(
                        Intent(Intent.ACTION_SEND).setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, shareSubject)
                            .putExtra(Intent.EXTRA_TEXT, e.text),
                        shareTitle,
                    )
                )
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) vm.exportCsv(uri)
    }
    // Sau khi người dùng trả lời hộp thoại xin quyền: bật nhắc hạn nếu được cấp
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.setReminders(true) else scope.launch { snackbar.showSnackbar(permissionDenied) }
    }
    fun enableReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.hasPermission(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else vm.setReminders(true)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) }, windowInsets = WindowInsets(0)) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(stringResource(R.string.settings_general))
            Group {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Payments, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.size(16.dp))
                        Column {
                            Text(stringResource(R.string.setting_currency), style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.setting_currency_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.size(12.dp))
                    SelectField(
                        label = stringResource(R.string.setting_currency),
                        value = settings.defaultCurrency,
                        options = Currency.entries,
                        optionLabel = { stringResource(it.label) },
                        onSelect = vm::setCurrency,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                HorizontalDivider()
                SettingRow(
                    Icons.Outlined.NotificationsActive,
                    stringResource(R.string.setting_reminders),
                    stringResource(R.string.setting_reminders_desc),
                    onClick = { if (settings.remindersEnabled) vm.setReminders(false) else enableReminders() },
                ) {
                    Switch(checked = settings.remindersEnabled, onCheckedChange = { on -> if (on) enableReminders() else vm.setReminders(false) })
                }
                HorizontalDivider()
                SettingRow(Icons.Outlined.Campaign, stringResource(R.string.test_notification), null, onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.hasPermission(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else vm.sendTestNotification()
                })
            }

            SectionHeader(stringResource(R.string.settings_data))
            Group {
                SettingRow(Icons.Outlined.FileDownload, stringResource(R.string.export_csv), stringResource(R.string.export_csv_desc), onClick = {
                    if (vm.subscriptionCount == 0) scope.launch { snackbar.showSnackbar(exportEmpty) }
                    else exportLauncher.launch(context.getString(R.string.export_file_name))
                })
                HorizontalDivider()
                SettingRow(Icons.Outlined.Share, stringResource(R.string.share_report), stringResource(R.string.share_report_desc), onClick = vm::shareReport)
                HorizontalDivider()
                SettingRow(Icons.Outlined.LibraryAdd, stringResource(R.string.add_demo_data), stringResource(R.string.add_demo_data_desc), onClick = vm::addDemoData)
                HorizontalDivider()
                SettingRow(
                    Icons.Outlined.DeleteForever, stringResource(R.string.clear_data), null,
                    tint = MaterialTheme.colorScheme.error, onClick = { confirmClear = true },
                )
            }

            SectionHeader(stringResource(R.string.settings_about))
            Group {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.size(16.dp))
                    Column {
                        Text(stringResource(R.string.app_name) + " · " + stringResource(R.string.about_version, BuildConfigInfo.versionName(context)), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.about_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_confirm_title)) },
            text = { Text(stringResource(R.string.clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearAll() }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)) {
        Column { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.size(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (trailing != null) {
            Spacer(Modifier.size(8.dp))
            trailing()
        }
    }
}
