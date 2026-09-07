package com.hrt.monitor.util

import com.hrt.monitor.data.AppData
import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.Units

object Csv {

    private fun escape(s: String): String = "\"" + s.replace("\"", "\"\"") + "\""

    /** 化验记录 CSV：每个激素保留用户录入时的数值与单位 */
    fun buildRecords(d: AppData): String {
        val sb = StringBuilder()
        sb.append("日期,备注,")
        Hormone.entries.forEach { h ->
            sb.append(h.short).append("值,").append(h.short).append("单位,")
        }
        sb.append("\n")
        d.records.forEach { r ->
            sb.append(Fmt.date(r.dateMillis)).append(",").append(escape(r.note)).append(",")
            Hormone.entries.forEach { h ->
                val m = r.measurementOf(h)
                if (m != null) {
                    sb.append(Units.format(m.value)).append(",").append(m.unit).append(",")
                } else {
                    sb.append(",,")
                }
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    /** 生活事件 CSV */
    fun buildEvents(d: AppData): String {
        val sb = StringBuilder()
        sb.append("日期,类型,备注\n")
        d.events.forEach { e ->
            sb.append(Fmt.date(e.dateMillis)).append(",")
                .append(e.type.key).append(",")
                .append(escape(e.note)).append("\n")
        }
        return sb.toString()
    }
}
