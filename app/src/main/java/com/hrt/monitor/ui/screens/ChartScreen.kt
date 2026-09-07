package com.hrt.monitor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrt.monitor.R
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.RefRange
import com.hrt.monitor.data.Status
import com.hrt.monitor.data.Units
import com.hrt.monitor.data.statusOf
import com.hrt.monitor.ui.StatusChip
import com.hrt.monitor.ui.bandColorOf
import com.hrt.monitor.ui.displayLabel
import com.hrt.monitor.ui.spanText
import com.hrt.monitor.util.Fmt
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private val rangeOptions = listOf(
    Long.MAX_VALUE to R.string.chart_range_all,
    365L to R.string.chart_range_1y,
    183L to R.string.chart_range_6m,
    92L to R.string.chart_range_3m
)

@Composable
fun ChartScreen(onOpenRanges: () -> Unit) {
    val data by AppRepo.data.collectAsState()
    var hormoneKey by rememberSaveable { mutableStateOf(Hormone.E2.key) }
    val hormone = Hormone.byKey(hormoneKey) ?: Hormone.E2
    var rangeDays by rememberSaveable { mutableStateOf(Long.MAX_VALUE) }
    var displayUnit by rememberSaveable { mutableStateOf("") }
    var selectedIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    var unitMenu by remember { mutableStateOf(false) }

    LaunchedEffect(hormoneKey) {
        displayUnit = Units.baseUnit(hormone)
        selectedIndex = null
    }
    val unit = displayUnit.ifEmpty { Units.baseUnit(hormone) }

    val cutoff = if (rangeDays == Long.MAX_VALUE) {
        Long.MIN_VALUE
    } else {
        System.currentTimeMillis() - rangeDays * 24L * 3600_000L
    }
    val points = remember(data.records, cutoff, hormoneKey) {
        data.records
            .filter { it.dateMillis >= cutoff }
            .mapNotNull { r -> r.baseValueOf(hormone)?.let { v -> r.dateMillis to v } }
            .sortedBy { it.first }
    }
    val ranges = data.ranges.filter { it.hormone == hormone && it.visible }
    val target = data.targetOf(hormone)

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.chart_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

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

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            rangeOptions.forEach { (days, res) ->
                FilterChip(
                    selected = rangeDays == days,
                    onClick = { rangeDays = days; selectedIndex = null },
                    label = { Text(stringResource(res)) }
                )
            }
            Box {
                OutlinedButton(
                    onClick = { unitMenu = true },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(
                        "${stringResource(R.string.chart_unit)}：$unit ▾",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                DropdownMenu(expanded = unitMenu, onDismissRequest = { unitMenu = false }) {
                    Units.byHormone.getValue(hormone).forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u.label) },
                            onClick = { displayUnit = u.label; unitMenu = false }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (points.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.chart_no_data),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Text(
                stringResource(R.string.chart_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            HormoneChart(
                hormone = hormone,
                points = points,
                ranges = ranges,
                target = target,
                displayUnit = unit,
                selectedIndex = selectedIndex,
                onSelect = { selectedIndex = it },
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            Spacer(Modifier.height(8.dp))

            selectedIndex?.let { idx ->
                val (d, v) = points[idx]
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(
                                R.string.chart_value_at,
                                Fmt.date(d),
                                Units.format(Units.fromBase(hormone, v, unit)),
                                unit
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        StatusChip(statusOf(v, target), null)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (ranges.isEmpty()) {
                Text(
                    stringResource(R.string.chart_no_ranges),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(R.string.chart_legend),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ranges.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Box(
                            Modifier.size(10.dp).background(bandColorOf(r), CircleShape)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "${r.displayLabel()}  ${r.spanText()}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        if (r.id == target?.id) {
                            Text("★", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            StatsRow(hormone, points.map { it.second }, target, unit)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatsRow(hormone: Hormone, values: List<Double>, target: RefRange?, unit: String) {
    if (values.isEmpty()) return
    val min = values.min()
    val max = values.max()
    val avg = values.average()
    val inCount = target?.let { t -> values.count { statusOf(it, t) == Status.IN } }

    Column {
        Text(
            stringResource(R.string.chart_stats_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row {
            StatCell(stringResource(R.string.stat_count), "${values.size}", Modifier.weight(1f))
            StatCell(stringResource(R.string.stat_min), Units.format(Units.fromBase(hormone, min, unit)), Modifier.weight(1f))
            StatCell(stringResource(R.string.stat_max), Units.format(Units.fromBase(hormone, max, unit)), Modifier.weight(1f))
            StatCell(stringResource(R.string.stat_avg), Units.format(Units.fromBase(hormone, avg, unit)), Modifier.weight(1f))
        }
        if (inCount != null) {
            Spacer(Modifier.height(6.dp))
            StatCell(
                stringResource(R.string.stat_in_target),
                "${inCount * 100 / values.size}%（$inCount/${values.size}）",
                Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// —— 图表几何工具 ——

private fun xOf(date: Long, minDate: Long, maxDate: Long, plotL: Float, plotR: Float, w: Float): Float {
    if (maxDate <= minDate) return plotL + (w - plotL - plotR) / 2f
    return plotL + (date - minDate).toFloat() / (maxDate - minDate).toFloat() * (w - plotL - plotR)
}

private fun yOf(v: Double, yMin: Double, yMax: Double, plotT: Float, plotB: Float, h: Float): Float {
    val ratio = ((yMax - v) / (yMax - yMin)).toFloat()
    return plotT + ratio * (h - plotT - plotB)
}

private fun computeYRange(values: List<Double>, ranges: List<RefRange>): Pair<Double, Double> {
    var lo = values.min()
    var hi = values.max()
    if (lo == hi) {
        lo -= 1.0
        hi += 1.0
    }
    for (r in ranges) {
        if (r.thresholdOnly) {
            val rmin = r.minBase()
            if (rmin > hi && rmin <= hi * 1.35) hi = rmin
        } else {
            if (r.minBase() < lo) lo = r.minBase()
            if (r.maxBase() > hi) hi = r.maxBase()
        }
    }
    val pad = (hi - lo) * 0.08
    return (lo - pad) to (hi + pad)
}

private fun niceStep(raw: Double): Double {
    if (raw <= 0) return 1.0
    val mag = 10.0.pow(floor(log10(raw)))
    val norm = raw / mag
    return when {
        norm <= 1.0 -> 1.0
        norm <= 2.0 -> 2.0
        norm <= 5.0 -> 5.0
        else -> 10.0
    } * mag
}

@Composable
private fun HormoneChart(
    hormone: Hormone,
    points: List<Pair<Long, Double>>,
    ranges: List<RefRange>,
    target: RefRange?,
    displayUnit: String,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current
    val labelPx = with(density) { 11.sp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(points) {
            detectTapGestures { offset ->
                if (points.isEmpty()) return@detectTapGestures
                if (points.size == 1) {
                    onSelect(0)
                    return@detectTapGestures
                }
                val w = size.width.toFloat()
                val plotL = 56.dp.toPx()
                val plotR = 12.dp.toPx()
                val minDate = points.first().first
                val maxDate = points.last().first
                var best = -1
                var bestDist = Float.MAX_VALUE
                for (i in points.indices) {
                    val x = xOf(points[i].first, minDate, maxDate, plotL, plotR, w)
                    val d = abs(x - offset.x)
                    if (d < bestDist) {
                        bestDist = d
                        best = i
                    }
                }
                onSelect(if (bestDist <= 28.dp.toPx()) best else null)
            }
        }
    ) {
        val w = size.width
        val h = size.height
        val plotT = 12.dp.toPx()
        val plotB = 24.dp.toPx()
        val plotL = 56.dp.toPx()
        val plotR = 12.dp.toPx()
        val yRange = computeYRange(points.map { it.second }, ranges)
        val minDate = points.first().first
        val maxDate = points.last().first
        val spanDays = (maxDate - minDate) / 86_400_000.0

        // 1. 参考范围色带 / 阈值虚线
        ranges.forEach { r ->
            val color = bandColorOf(r)
            if (r.thresholdOnly) {
                val y = yOf(r.minBase(), yRange.first, yRange.second, plotT, plotB, h)
                if (y >= plotT && y <= h - plotB) {
                    drawLine(
                        color = color,
                        start = Offset(plotL, y),
                        end = Offset(w - plotR, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                }
            } else {
                val top = yOf(r.maxBase(), yRange.first, yRange.second, plotT, plotB, h)
                val bottom = yOf(r.minBase(), yRange.first, yRange.second, plotT, plotB, h)
                val alpha = if (r.id == target?.id) 0.22f else 0.11f
                drawRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(plotL, top),
                    size = Size(w - plotL - plotR, (bottom - top).coerceAtLeast(0f))
                )
            }
        }

        // 2. 网格线与坐标标签
        val textPaint = android.graphics.Paint().apply {
            color = labelColor.toArgb()
            textSize = labelPx
            isAntiAlias = true
        }
        val step = niceStep((yRange.second - yRange.first) / 4.0)
        var tick = ceil(yRange.first / step) * step
        textPaint.textAlign = android.graphics.Paint.Align.RIGHT
        while (tick <= yRange.second + step * 0.001) {
            val y = yOf(tick, yRange.first, yRange.second, plotT, plotB, h)
            drawLine(
                color = gridColor,
                start = Offset(plotL, y),
                end = Offset(w - plotR, y),
                strokeWidth = 1f
            )
            val label = Units.format(Units.fromBase(hormone, tick, displayUnit))
            drawContext.canvas.nativeCanvas.drawText(label, plotL - 6.dp.toPx(), y + labelPx / 3f, textPaint)
            tick += step
        }

        if (points.size > 1) {
            textPaint.textAlign = android.graphics.Paint.Align.CENTER
            val ticks = 4
            for (k in 0..ticks) {
                val date = minDate + (maxDate - minDate) * k / ticks
                val x = xOf(date, minDate, maxDate, plotL, plotR, w)
                drawLine(
                    color = gridColor,
                    start = Offset(x, plotT),
                    end = Offset(x, h - plotB),
                    strokeWidth = 1f
                )
                val label = if (spanDays > 400) Fmt.yearMonth(date) else Fmt.monthDay(date)
                drawContext.canvas.nativeCanvas.drawText(label, x, h - plotB + labelPx * 1.15f, textPaint)
            }
        }

        // 3. 折线
        if (points.size > 1) {
            val path = Path()
            points.forEachIndexed { i, p ->
                val x = xOf(p.first, minDate, maxDate, plotL, plotR, w)
                val y = yOf(p.second, yRange.first, yRange.second, plotT, plotB, h)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // 4. 数据点
        points.forEachIndexed { i, p ->
            val x = xOf(p.first, minDate, maxDate, plotL, plotR, w)
            val y = yOf(p.second, yRange.first, yRange.second, plotT, plotB, h)
            if (i == selectedIndex) {
                drawCircle(color = surfaceColor, radius = 7.dp.toPx(), center = Offset(x, y))
                drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = Offset(x, y))
            } else {
                drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
