package com.hrt.monitor.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hrt.monitor.R
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.LabRecord
import com.hrt.monitor.data.Measurement
import com.hrt.monitor.data.Units
import com.hrt.monitor.labelRes
import com.hrt.monitor.ui.recordMetaLine
import com.hrt.monitor.util.Fmt
import com.hrt.monitor.util.toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(record: LabRecord, onClose: () -> Unit) {
    val context = LocalContext.current
    val isNew = record.id.isEmpty()

    var dateMillis by remember {
        mutableStateOf(if (record.dateMillis > 0) record.dateMillis else System.currentTimeMillis())
    }
    var note by remember { mutableStateOf(record.note) }
    val inputs = remember {
        mutableStateMapOf<Hormone, String>().apply {
            record.items.forEach { m -> put(m.hormone, Units.format(m.value)) }
        }
    }
    val units = remember {
        mutableStateMapOf<Hormone, String>().apply {
            record.items.forEach { m -> put(m.hormone, m.unit) }
        }
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var unitMenuFor by remember { mutableStateOf<Hormone?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    fun save() {
        val items = mutableListOf<Measurement>()
        for (h in Hormone.entries) {
            val text = inputs[h]?.trim().orEmpty()
            if (text.isEmpty()) continue
            val v = text.toDoubleOrNull()
            if (v == null || !v.isFinite() || v <= 0.0) {
                context.toast(context.getString(R.string.invalid_value))
                return
            }
            items.add(Measurement(h, v, units[h] ?: Units.defaultUnit(h)))
        }
        if (items.isEmpty()) {
            context.toast(context.getString(R.string.record_need_one))
            return
        }
        AppRepo.upsertRecord(
            LabRecord(
                id = if (isNew) AppRepo.newId() else record.id,
                dateMillis = dateMillis,
                note = note.trim(),
                items = items
            )
        )
        context.toast(context.getString(R.string.record_saved))
        onClose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.record_new else R.string.record_edit)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(Modifier.fillMaxWidth().clickable { showDatePicker = true }) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            stringResource(R.string.record_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(Fmt.date(dateMillis), style = MaterialTheme.typography.titleMedium)
                        recordMetaLine(dateMillis)?.let { meta ->
                            Spacer(Modifier.height(2.dp))
                            Text(
                                meta,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.record_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Hormone.entries.forEach { h ->
                HormoneInputCard(
                    h = h,
                    value = inputs[h] ?: "",
                    onValueChange = { inputs[h] = it },
                    unit = units[h] ?: Units.defaultUnit(h),
                    unitMenuOpen = unitMenuFor == h,
                    onToggleMenu = { unitMenuFor = if (unitMenuFor == h) null else h },
                    onUnitSelected = { units[h] = it; unitMenuFor = null }
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.record_note)) },
                placeholder = { Text(stringResource(R.string.record_note_hint)) },
                minLines = 2
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = Fmt.localDateToUtcMillis(dateMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { dateMillis = Fmt.utcMillisToLocalMidnight(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = dpState)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_record_confirm)) },
            text = { Text(stringResource(R.string.delete_record_hint)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    AppRepo.deleteRecord(record.id)
                    context.toast(context.getString(R.string.record_deleted))
                    onClose()
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun HormoneInputCard(
    h: Hormone,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    unitMenuOpen: Boolean,
    onToggleMenu: () -> Unit,
    onUnitSelected: (String) -> Unit
) {
    val baseValue = value.trim().toDoubleOrNull()?.let { Units.toBase(h, it, unit) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 8.dp)) {
            Text(
                stringResource(h.labelRes()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.record_unit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Spacer(Modifier.width(8.dp))
                Box {
                    OutlinedButton(onClick = onToggleMenu) { Text("$unit ▾") }
                    DropdownMenu(expanded = unitMenuOpen, onDismissRequest = onToggleMenu) {
                        Units.byHormone.getValue(h).forEach { u ->
                            DropdownMenuItem(
                                text = { Text(u.label) },
                                onClick = { onUnitSelected(u.label) }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (baseValue != null) {
                    stringResource(R.string.record_equals, Units.format(baseValue), Units.baseUnit(h))
                } else {
                    stringResource(R.string.record_empty_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
