package com.hrt.monitor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.hrt.monitor.R
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.RefRange
import com.hrt.monitor.data.Units
import com.hrt.monitor.labelRes
import com.hrt.monitor.ui.bandColorOf
import com.hrt.monitor.ui.displayLabel
import com.hrt.monitor.ui.spanText
import com.hrt.monitor.util.toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeScreen(onClose: () -> Unit) {
    val data by AppRepo.data.collectAsState()
    val context = LocalContext.current
    var hormoneKey by rememberSaveable { mutableStateOf(Hormone.E2.key) }
    val hormone = Hormone.byKey(hormoneKey) ?: Hormone.E2
    var editing by remember { mutableStateOf<RefRange?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<RefRange?>(null) }

    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ranges_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                stringResource(R.string.ranges_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Hormone.entries.forEach { h ->
                    FilterChip(
                        selected = h.key == hormoneKey,
                        onClick = { hormoneKey = h.key },
                        label = { Text(h.short) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            val list = data.ranges.filter { it.hormone == hormone }
            if (list.isEmpty()) {
                Text(
                    stringResource(R.string.range_no_ranges),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.id }) { r ->
                    RangeRow(
                        range = r,
                        isTarget = data.targetOf(hormone)?.id == r.id,
                        onToggleVisible = { v -> AppRepo.setRangeVisible(r.id, v) },
                        onToggleTarget = {
                            AppRepo.setTarget(hormone, if (data.targetOf(hormone)?.id == r.id) null else r.id)
                        },
                        onEdit = { editing = r },
                        onDelete = { deleting = r }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { showNew = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.range_add))
            }
        }
    }

    if (showNew) {
        RangeEditorDialog(
            hormone = hormone,
            range = null,
            initialIsTarget = false,
            onDismiss = { showNew = false }
        )
    }
    editing?.let { r ->
        RangeEditorDialog(
            hormone = hormone,
            range = r,
            initialIsTarget = data.targetOf(hormone)?.id == r.id,
            onDismiss = { editing = null }
        )
    }
    deleting?.let { r ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.range_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    AppRepo.deleteRange(r.id)
                    context.toast(context.getString(R.string.range_deleted))
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun RangeRow(
    range: RefRange,
    isTarget: Boolean,
    onToggleVisible: (Boolean) -> Unit,
    onToggleTarget: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onToggleTarget)) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(12.dp).background(bandColorOf(range), CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        range.displayLabel().ifEmpty { stringResource(R.string.range_custom) },
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (isTarget) {
                        Text("★", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                    }
                }
                Text(
                    range.spanText(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(if (range.builtin) R.string.range_builtin else R.string.range_custom),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = range.visible, onCheckedChange = onToggleVisible)
            if (!range.builtin) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun RangeEditorDialog(
    hormone: Hormone,
    range: RefRange?,
    initialIsTarget: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(range?.label ?: "") }
    var minText by remember { mutableStateOf(range?.let { Units.format(it.min) } ?: "") }
    var maxText by remember { mutableStateOf(range?.let { Units.format(it.max) } ?: "") }
    var unit by remember { mutableStateOf(range?.unit ?: Units.defaultUnit(hormone)) }
    var isTarget by remember { mutableStateOf(initialIsTarget) }
    var showChart by remember { mutableStateOf(range?.visible ?: true) }
    var unitMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (range == null) R.string.range_add else R.string.range_edit)) },
        text = {
            Column {
                Text(
                    stringResource(hormone.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.range_name)) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { minText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.range_min)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = maxText,
                        onValueChange = { maxText = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.range_max)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box {
                    OutlinedButton(onClick = { unitMenu = true }) {
                        Text("${stringResource(R.string.range_unit)}：$unit")
                    }
                    DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                        Units.byHormone.getValue(hormone).forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.label) },
                                onClick = { unit = u.label; unitMenu = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { isTarget = !isTarget },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isTarget, onCheckedChange = { isTarget = it })
                    Column {
                        Text(stringResource(R.string.range_target), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.range_target_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { showChart = !showChart },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = showChart, onCheckedChange = { showChart = it })
                    Text(stringResource(R.string.range_show_chart), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val mn = minText.trim().toDoubleOrNull()
                val mx = maxText.trim().toDoubleOrNull()
                if (mn == null || mx == null || !mn.isFinite() || !mx.isFinite()) {
                    context.toast(context.getString(R.string.range_invalid_number))
                    return@TextButton
                }
                if (mn >= mx) {
                    context.toast(context.getString(R.string.range_invalid))
                    return@TextButton
                }
                val r = RefRange(
                    id = range?.id ?: AppRepo.newId(),
                    hormone = hormone,
                    label = name.trim().ifEmpty { null },
                    min = mn,
                    max = mx,
                    unit = unit,
                    builtin = false,
                    visible = showChart
                )
                AppRepo.upsertRange(r)
                if (isTarget) {
                    AppRepo.setTarget(hormone, r.id)
                } else if (range != null && initialIsTarget) {
                    AppRepo.setTarget(hormone, null)
                }
                context.toast(context.getString(R.string.range_saved))
                onDismiss()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
