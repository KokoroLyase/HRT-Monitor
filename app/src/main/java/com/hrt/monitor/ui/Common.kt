package com.hrt.monitor.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hrt.monitor.R
import com.hrt.monitor.data.Prefs
import com.hrt.monitor.data.RefRange
import com.hrt.monitor.data.Status
import com.hrt.monitor.util.Fmt

/** 相对目标范围的状态徽章 */
@Composable
fun StatusChip(status: Status, target: RefRange?) {
    val (text, container, content) = when (status) {
        Status.LOW -> Triple(
            stringResource(R.string.low),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        Status.IN -> Triple(
            stringResource(R.string.in_range),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        Status.HIGH -> Triple(
            stringResource(R.string.high),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        Status.UNKNOWN -> Triple(
            stringResource(R.string.unknown),
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = container, shape = RoundedCornerShape(50)) {
            Text(
                text,
                Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelMedium,
                color = content
            )
        }
        if (target != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                target.displayLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun durationString(context: android.content.Context, d: Fmt.DurationParts): String {
    val sb = StringBuilder()
    if (d.years > 0) sb.append(context.getString(R.string.dur_years, d.years))
    if (d.months > 0) sb.append(context.getString(R.string.dur_months, d.months))
    if (d.days > 0) sb.append(context.getString(R.string.dur_days, d.days))
    if (sb.isEmpty()) sb.append(context.getString(R.string.dur_days, 0))
    return sb.toString()
}

/** 记录对应的元信息：血检时年龄 · HRT 已进行时长（无资料时为 null） */
@Composable
fun recordMetaLine(dateMillis: Long): String? {
    val context = LocalContext.current
    val parts = mutableListOf<String>()
    if (Prefs.birthday > 0) {
        val age = Fmt.ageAt(Prefs.birthday, dateMillis)
        if (age >= 0) parts.add(context.getString(R.string.age_at, age))
    }
    if (Prefs.hrtStart > 0 && dateMillis >= Prefs.hrtStart) {
        val d = Fmt.durationParts(Prefs.hrtStart, dateMillis)
        parts.add(context.getString(R.string.hrt_for, durationString(context, d)))
    }
    return if (parts.isEmpty()) null else parts.joinToString(" · ")
}
