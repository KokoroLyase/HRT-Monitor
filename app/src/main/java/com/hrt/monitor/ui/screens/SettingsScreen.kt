package com.hrt.monitor.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hrt.monitor.R
import com.hrt.monitor.data.AppData
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.JsonIO
import com.hrt.monitor.data.Prefs
import com.hrt.monitor.ui.ProfileDialog
import com.hrt.monitor.ui.UiState
import com.hrt.monitor.util.AiClient
import com.hrt.monitor.util.Csv
import com.hrt.monitor.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(onOpenRanges: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAi by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var aiBaseUrl by remember { mutableStateOf(Prefs.aiBaseUrl.ifEmpty { Prefs.DEFAULT_BASE_URL }) }
    var aiKey by remember { mutableStateOf(Prefs.aiKey) }
    var aiModel by remember { mutableStateOf(Prefs.aiModel.ifEmpty { Prefs.DEFAULT_MODEL }) }
    var aiTesting by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<AppData?>(null) }

    fun dateStr(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun saveAi() {
        Prefs.aiBaseUrl = aiBaseUrl.trim()
        Prefs.aiKey = aiKey.trim()
        Prefs.aiModel = aiModel.trim()
        context.toast(context.getString(R.string.ai_saved))
    }

    fun testAi() {
        scope.launch {
            aiTesting = true
            try {
                AiClient.chat(
                    baseUrl = aiBaseUrl,
                    apiKey = aiKey,
                    model = aiModel,
                    system = "你是连接测试助手。",
                    user = "请仅回复：OK"
                )
                context.toast(context.getString(R.string.ai_test_ok))
            } catch (e: Exception) {
                context.toast(context.getString(R.string.ai_test_failed, e.message ?: "?"))
            }
            aiTesting = false
        }
    }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = JsonIO.stringify(AppRepo.data.value)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: throw Exception("open failed")
                }
                context.toast(context.getString(R.string.data_export_done, uri.lastPathSegment ?: "backup.json"))
            } catch (e: Exception) {
                context.toast(context.getString(R.string.data_export_failed, e.message ?: "?"))
            }
        }
    }

    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = Csv.buildRecords(AppRepo.data.value)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } ?: throw Exception("open failed")
                }
                context.toast(context.getString(R.string.data_export_done, uri.lastPathSegment ?: "records.csv"))
            } catch (e: Exception) {
                context.toast(context.getString(R.string.data_export_failed, e.message ?: "?"))
            }
        }
    }

    val importJson = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val text = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: throw Exception("open failed")
                }
                val parsed = JsonIO.parse(text)
                    ?: throw Exception(context.getString(R.string.import_invalid))
                pendingImport = parsed
                showImportDialog = true
            } catch (e: Exception) {
                context.toast(context.getString(R.string.data_import_failed, e.message ?: "?"))
            }
        }
    }

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        SettingsItem(
            icon = Icons.Filled.Tune,
            title = stringResource(R.string.set_ranges),
            desc = stringResource(R.string.set_ranges_desc),
            onClick = onOpenRanges
        )
        Spacer(Modifier.height(12.dp))
        SettingsItem(
            icon = Icons.Filled.Person,
            title = stringResource(R.string.set_profile),
            desc = stringResource(R.string.set_profile_desc),
            onClick = { showProfile = true }
        )
        Spacer(Modifier.height(12.dp))

        // —— AI 智能解读 ——
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable { showAi = !showAi },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.set_ai), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.set_ai_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        if (showAi) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
                if (showAi) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = aiBaseUrl,
                        onValueChange = { aiBaseUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ai_base_url)) },
                        placeholder = { Text(stringResource(R.string.ai_base_url_hint)) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiKey,
                        onValueChange = { aiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ai_api_key)) },
                        placeholder = { Text(stringResource(R.string.ai_api_key_hint)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiModel,
                        onValueChange = { aiModel = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.ai_model)) },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { testAi() }, enabled = !aiTesting) {
                            Text(
                                if (aiTesting) stringResource(R.string.ai_config_loading)
                                else stringResource(R.string.ai_test)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { saveAi() }) { Text(stringResource(R.string.ai_save)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // —— 外观 ——
        Text(
            stringResource(R.string.set_appearance),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(stringResource(R.string.set_theme), style = MaterialTheme.typography.titleSmall)
                RadioRow(stringResource(R.string.theme_system), Prefs.theme == Prefs.THEME_SYSTEM) {
                    Prefs.theme = Prefs.THEME_SYSTEM
                    UiState.themeMode.intValue = Prefs.THEME_SYSTEM
                }
                RadioRow(stringResource(R.string.theme_light), Prefs.theme == Prefs.THEME_LIGHT) {
                    Prefs.theme = Prefs.THEME_LIGHT
                    UiState.themeMode.intValue = Prefs.THEME_LIGHT
                }
                RadioRow(stringResource(R.string.theme_dark), Prefs.theme == Prefs.THEME_DARK) {
                    Prefs.theme = Prefs.THEME_DARK
                    UiState.themeMode.intValue = Prefs.THEME_DARK
                }
                HorizontalDivider()
                Text(stringResource(R.string.set_language), style = MaterialTheme.typography.titleSmall)
                RadioRow(stringResource(R.string.lang_system), Prefs.language == Prefs.LANG_SYSTEM) {
                    Prefs.language = Prefs.LANG_SYSTEM
                    UiState.language.intValue = Prefs.LANG_SYSTEM
                    (context as? Activity)?.recreate()
                }
                RadioRow(stringResource(R.string.lang_zh_cn), Prefs.language == Prefs.LANG_ZH_CN) {
                    Prefs.language = Prefs.LANG_ZH_CN
                    UiState.language.intValue = Prefs.LANG_ZH_CN
                    (context as? Activity)?.recreate()
                }
                RadioRow(stringResource(R.string.lang_zh_tw), Prefs.language == Prefs.LANG_ZH_TW) {
                    Prefs.language = Prefs.LANG_ZH_TW
                    UiState.language.intValue = Prefs.LANG_ZH_TW
                    (context as? Activity)?.recreate()
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // —— 数据管理 ——
        Text(
            stringResource(R.string.set_data),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                ActionRow(Icons.Filled.FileDownload, stringResource(R.string.data_export_json)) {
                    exportJson.launch("hrt-monitor-backup-${dateStr()}.json")
                }
                ActionRow(Icons.Filled.TableChart, stringResource(R.string.data_export_csv)) {
                    exportCsv.launch("hrt-monitor-records-${dateStr()}.csv")
                }
                ActionRow(Icons.Filled.FileUpload, stringResource(R.string.data_import)) {
                    importJson.launch(arrayOf("application/json", "application/octet-stream", "text/*"))
                }
                HorizontalDivider()
                ActionRow(Icons.Filled.DeleteForever, stringResource(R.string.data_clear), danger = true) {
                    confirmClear = true
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        // —— 关于 ——
        Text(
            stringResource(R.string.set_about),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    stringResource(R.string.about_version, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.about_developer),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.about_contact),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.about_privacy_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.about_privacy_desc), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(stringResource(R.string.about_disclaimer_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.disclaimer_full), style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    stringResource(R.string.about_build),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_confirm)) },
            text = { Text(stringResource(R.string.clear_confirm_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    AppRepo.clearAll()
                    context.toast(context.getString(R.string.data_cleared))
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showImportDialog && pendingImport != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.data_import_title)) },
            text = { Text(stringResource(R.string.data_import_desc)) },
            confirmButton = {
                TextButton(onClick = {
                    val r = AppRepo.import(pendingImport!!, replace = false)
                    context.toast(context.getString(R.string.data_import_done, r.records, r.events, r.ranges))
                    showImportDialog = false
                    pendingImport = null
                }) { Text(stringResource(R.string.import_merge)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val r = AppRepo.import(pendingImport!!, replace = true)
                    context.toast(context.getString(R.string.data_import_done, r.records, r.events, r.ranges))
                    showImportDialog = false
                    pendingImport = null
                }) { Text(stringResource(R.string.import_replace)) }
            }
        )
    }

    if (showProfile) {
        ProfileDialog(onClose = { showProfile = false }, allowSkip = false)
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, desc: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
