package com.hrt.monitor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hrt.monitor.R
import com.hrt.monitor.data.Prefs
import com.hrt.monitor.util.Fmt
import com.hrt.monitor.util.toast

/**
 * 个人资料对话框：生日 + HRT 起始日期（均可留空）。
 * 首启引导（allowSkip = true）与设置页编辑共用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDialog(onClose: () -> Unit, allowSkip: Boolean) {
    val context = LocalContext.current
    var birthday by remember { mutableStateOf(Prefs.birthday) }
    var hrtStart by remember { mutableStateOf(Prefs.hrtStart) }
    var datePickerFor by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.profile_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.profile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                ProfileDateRow(stringResource(R.string.profile_birthday), birthday) {
                    datePickerFor = "birthday"
                }
                Spacer(Modifier.height(8.dp))
                ProfileDateRow(stringResource(R.string.profile_hrt_start), hrtStart) {
                    datePickerFor = "hrt"
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                Prefs.birthday = birthday
                Prefs.hrtStart = hrtStart
                Prefs.profileAsked = true
                context.toast(context.getString(R.string.profile_saved))
                onClose()
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = {
                if (allowSkip) Prefs.profileAsked = true
                onClose()
            }) { Text(stringResource(if (allowSkip) R.string.profile_skip else R.string.cancel)) }
        }
    )

    datePickerFor?.let { which ->
        val isBirthday = which == "birthday"
        val current = if (isBirthday) birthday else hrtStart
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = if (current > 0) Fmt.localDateToUtcMillis(current) else null
        )
        DatePickerDialog(
            onDismissRequest = { datePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { utc ->
                        val d = Fmt.utcMillisToLocalMidnight(utc)
                        if (isBirthday) birthday = d else hrtStart = d
                    }
                    datePickerFor = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { datePickerFor = null }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = dpState)
        }
    }
}

@Composable
private fun ProfileDateRow(label: String, value: Long, onClick: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (value > 0) Fmt.date(value) else stringResource(R.string.profile_not_set),
                    style = MaterialTheme.typography.titleSmall
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
