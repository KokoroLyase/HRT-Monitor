package com.hrt.monitor.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrt.monitor.R
import com.hrt.monitor.data.AppData
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.LabRecord
import com.hrt.monitor.data.Units
import com.hrt.monitor.data.statusOf
import com.hrt.monitor.labelRes
import com.hrt.monitor.ui.StatusChip
import com.hrt.monitor.ui.recordMetaLine
import com.hrt.monitor.util.Fmt

@Composable
fun HomeScreen(onOpenHistory: () -> Unit) {
    val data by AppRepo.data.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Hormone.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { h ->
                    HormoneCard(h, data, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        if (data.records.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.no_records_yet), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.no_records_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.recent_records),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.all_records)) }
            }
            data.records.take(5).forEach { r ->
                RecordRow(r)
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(72.dp))
    }
}

@Composable
private fun HormoneCard(h: Hormone, data: AppData, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val latestRecord = data.records.firstOrNull { it.measurementOf(h) != null }
    val m = latestRecord?.measurementOf(h)
    val base = latestRecord?.baseValueOf(h)
    val target = data.targetOf(h)
    val status = statusOf(base, target)

    ElevatedCard(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(
                stringResource(h.labelRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            if (m != null && latestRecord != null) {
                Text(
                    "${Units.format(m.value)} ${m.unit}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    Fmt.relativeDay(context, latestRecord.dateMillis) + " · " + Fmt.date(latestRecord.dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.not_tested),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            StatusChip(status, target)
        }
    }
}

/** 首页最近记录行 */
@Composable
fun RecordRow(r: LabRecord) {
    val context = LocalContext.current
    val summary = r.items.joinToString(" · ") {
        "${it.hormone.short} ${Units.format(it.value)} ${it.unit}"
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Fmt.date(r.dateMillis),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    Fmt.relativeDay(context, r.dateMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(summary, style = MaterialTheme.typography.bodyMedium)
            if (r.note.isNotBlank()) {
                Text(
                    r.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            recordMetaLine(r.dateMillis)?.let { meta ->
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
