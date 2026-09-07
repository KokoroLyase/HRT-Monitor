package com.hrt.monitor.util

import android.content.Context
import com.hrt.monitor.R
import com.hrt.monitor.data.AppData
import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.Status
import com.hrt.monitor.data.Units
import com.hrt.monitor.data.statusOf
import com.hrt.monitor.labelRes

/** 本地统计报告与 AI 提示词生成 */
object ReportBuilder {

    private fun statusText(context: Context, status: Status): String = when (status) {
        Status.LOW -> context.getString(R.string.report_status_low)
        Status.IN -> context.getString(R.string.report_status_in)
        Status.HIGH -> context.getString(R.string.report_status_high)
        Status.UNKNOWN -> context.getString(R.string.report_status_no_target)
    }

    private fun targetText(context: Context, data: AppData, h: Hormone): String? {
        val t = data.targetOf(h) ?: return null
        val name = t.label
            ?: t.labelRes?.let { res ->
                val id = context.resources.getIdentifier(res, "string", context.packageName)
                if (id != 0) context.getString(id) else res
            }
            ?: ""
        val span = if (t.thresholdOnly) {
            "≥ ${Units.format(t.min)} ${t.unit}"
        } else {
            "${Units.format(t.min)} – ${Units.format(t.max)} ${t.unit}"
        }
        return "$name（$span）"
    }

    /** 生成纯文本的本地统计报告 */
    fun buildLocal(context: Context, data: AppData): String {
        val sb = StringBuilder()
        sb.append(context.getString(R.string.report_header)).append("\n")
        sb.append(context.getString(R.string.report_generated_at, Fmt.date(System.currentTimeMillis()))).append("\n")
        sb.append(context.getString(R.string.report_record_count, data.records.size)).append("\n")
        sb.append(context.getString(R.string.report_event_count, data.events.size)).append("\n\n")

        for (h in Hormone.entries) {
            val entries = data.records.mapNotNull { r ->
                r.measurementOf(h)?.let { m -> Triple(r, m, Units.toBase(h, m.value, m.unit)) }
            }.sortedBy { it.first.dateMillis }
            if (entries.isEmpty()) continue

            val unit = Units.baseUnit(h)
            val name = context.getString(h.labelRes())
            sb.append(context.getString(R.string.report_section_hormone, name, unit)).append("\n")

            val values = entries.map { it.third }
            val latest = entries.last()
            val target = data.targetOf(h)
            val status = target?.let { statusText(context, statusOf(latest.third, it)) }
                ?: context.getString(R.string.report_status_no_target)
            sb.append(
                context.getString(
                    R.string.report_line_stats,
                    entries.size,
                    Fmt.date(latest.first.dateMillis),
                    Units.format(Units.fromBase(h, latest.third, unit)),
                    unit,
                    status
                )
            ).append("\n")

            val min = values.min()
            val max = values.max()
            val avg = values.average()
            val ratio = target?.let { t ->
                val inCount = values.count { statusOf(it, t) == Status.IN }
                "${inCount * 100 / values.size}%（$inCount/${values.size}）"
            } ?: context.getString(R.string.report_no_value)
            sb.append(
                context.getString(
                    R.string.report_line_range,
                    Units.format(Units.fromBase(h, min, unit)),
                    Units.format(Units.fromBase(h, max, unit)),
                    Units.format(Units.fromBase(h, avg, unit)),
                    ratio
                )
            ).append("\n")

            targetText(context, data, h)?.let {
                sb.append("  ").append(context.getString(R.string.target)).append("：").append(it).append("\n")
            }
            sb.append("\n")
        }

        if (data.events.isNotEmpty()) {
            sb.append(context.getString(R.string.report_recent_events)).append("\n")
            data.events.take(5).forEach { e ->
                sb.append("  ").append(Fmt.date(e.dateMillis))
                    .append(" ").append(context.getString(e.type.labelRes()))
                if (e.note.isNotBlank()) sb.append("：").append(e.note)
                sb.append("\n")
            }
            sb.append("\n")
        }

        sb.append(context.getString(R.string.report_local_footer)).append("\n")
        sb.append(context.getString(R.string.disclaimer_banner))
        return sb.toString()
    }

    /** 生成发送给 AI 的数据摘要（统一使用基准单位） */
    fun buildAiUserPrompt(context: Context, data: AppData): String {
        val sb = StringBuilder()
        sb.append("以下是我的性激素化验数据摘要（共 ").append(data.records.size).append(" 条记录）：\n")
        for (h in Hormone.entries) {
            val entries = data.records.mapNotNull { r ->
                r.measurementOf(h)?.let { m -> Triple(r, m, Units.toBase(h, m.value, m.unit)) }
            }.sortedBy { it.first.dateMillis }
            if (entries.isEmpty()) continue

            val unit = Units.baseUnit(h)
            val values = entries.map { it.third }
            val latest = entries.last()
            val target = data.targetOf(h)
            val targetSpan = target?.let {
                if (it.thresholdOnly) "≥ ${Units.format(it.minBase())} $unit"
                else "${Units.format(it.minBase())} – ${Units.format(it.maxBase())} $unit"
            } ?: "未设置"
            val status = target?.let { statusText(context, statusOf(latest.third, it)) } ?: "-"

            sb.append("- ").append(context.getString(h.labelRes()))
                .append("：共 ").append(entries.size).append(" 次；最近 ")
                .append(Fmt.date(latest.first.dateMillis)).append(" 为 ")
                .append(Units.format(latest.third)).append(" ").append(unit)
                .append("（目标范围 ").append(targetSpan).append("，判定：").append(status).append("）；")
                .append("历史范围 ").append(Units.format(values.min())).append(" – ")
                .append(Units.format(values.max())).append(" ").append(unit)
                .append("，平均 ").append(Units.format(values.average())).append(" ").append(unit).append("。\n")
        }
        if (data.events.isNotEmpty()) {
            sb.append("近期事件：\n")
            data.events.take(10).forEach { e ->
                sb.append("- ").append(Fmt.date(e.dateMillis))
                    .append(" ").append(context.getString(e.type.labelRes()))
                if (e.note.isNotBlank()) sb.append("：").append(e.note)
                sb.append("\n")
            }
        }
        sb.append("\n请基于以上数据给出解读：各项指标的含义、变化趋势、与目标/参考范围的关系、需要关注的异常，以及建议与医生沟通的要点。")
        return sb.toString()
    }
}
