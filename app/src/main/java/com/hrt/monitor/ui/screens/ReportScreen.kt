package com.hrt.monitor.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrt.monitor.R
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.Prefs
import com.hrt.monitor.util.AiClient
import com.hrt.monitor.util.ReportBuilder
import com.hrt.monitor.util.toast
import kotlinx.coroutines.launch

@Composable
fun ReportScreen(onGoSettings: () -> Unit) {
    val data by AppRepo.data.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    var localReport by remember { mutableStateOf<String?>(null) }
    var aiReport by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    fun shareText(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
    }

    fun copyText(text: String) {
        clipboard.setText(AnnotatedString(text))
        context.toast(context.getString(R.string.copied))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.report_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Row(Modifier.padding(12.dp)) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.disclaimer_banner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        Text(stringResource(R.string.report_local), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.report_local_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { localReport = ReportBuilder.buildLocal(context, data) },
            enabled = data.records.isNotEmpty()
        ) {
            Text(stringResource(R.string.report_generate))
        }
        localReport?.let { text ->
            Spacer(Modifier.height(8.dp))
            ReportCard(text, onCopy = { copyText(text) }, onShare = { shareText(text) })
        }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.report_ai), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.report_ai_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        if (!Prefs.aiConfigured()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.report_ai_not_configured), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onGoSettings) { Text(stringResource(R.string.report_ai_go_settings)) }
                }
            }
        } else {
            Button(
                onClick = {
                    if (data.records.isEmpty()) {
                        context.toast(context.getString(R.string.report_no_data))
                        return@Button
                    }
                    scope.launch {
                        aiLoading = true
                        aiError = null
                        try {
                            aiReport = AiClient.chat(
                                baseUrl = Prefs.aiBaseUrl,
                                apiKey = Prefs.aiKey,
                                model = Prefs.aiModel,
                                system = context.getString(R.string.ai_system_prompt),
                                user = ReportBuilder.buildAiUserPrompt(context, data)
                            )
                        } catch (e: Exception) {
                            aiError = e.message ?: "error"
                        }
                        aiLoading = false
                    }
                },
                enabled = !aiLoading && data.records.isNotEmpty()
            ) {
                Text(
                    if (aiLoading) stringResource(R.string.report_ai_loading)
                    else stringResource(R.string.report_ai_btn)
                )
            }
            aiError?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.report_ai_failed, msg),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            aiReport?.let { text ->
                Spacer(Modifier.height(8.dp))
                ReportCard(text, onCopy = { copyText(text) }, onShare = { shareText(text) })
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.report_ai_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun ReportCard(text: String, onCopy: () -> Unit, onShare: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCopy) { Text(stringResource(R.string.copy)) }
                TextButton(onClick = onShare) { Text(stringResource(R.string.share)) }
            }
        }
    }
}
