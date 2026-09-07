package com.hrt.monitor.util

import android.content.Context
import android.widget.Toast
import com.hrt.monitor.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

object Fmt {

    fun date(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))

    fun monthDay(millis: Long): String =
        SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(millis))

    fun yearMonth(millis: Long): String =
        SimpleDateFormat("yy-MM", Locale.getDefault()).format(Date(millis))

    /** 今天 / 昨天 / N 天前 */
    fun relativeDay(context: Context, millis: Long): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val day = 24L * 3600_000L
        return when {
            millis >= todayStart -> context.getString(R.string.today)
            millis >= todayStart - day -> context.getString(R.string.yesterday)
            else -> context.getString(R.string.days_ago, ((todayStart - millis) / day).toInt() + 1)
        }
    }

    /** 本地日期（午夜）→ UTC 毫秒（DatePicker 使用 UTC） */
    fun localDateToUtcMillis(localMillis: Long): Long {
        val local = Calendar.getInstance()
        local.timeInMillis = localMillis
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        utc.set(
            local.get(Calendar.YEAR), local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH), 0, 0, 0
        )
        return utc.timeInMillis
    }

    /** DatePicker 返回的 UTC 毫秒 → 本地日期（午夜）毫秒 */
    fun utcMillisToLocalMidnight(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.timeInMillis = utcMillis
        val local = Calendar.getInstance()
        local.set(
            utc.get(Calendar.YEAR), utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0
        )
        local.set(Calendar.MILLISECOND, 0)
        return local.timeInMillis
    }

    /** 时长的 年/月/日 分解（要求 end >= start） */
    data class DurationParts(val years: Int, val months: Int, val days: Int)

    fun durationParts(startMillis: Long, endMillis: Long): DurationParts {
        val s = Calendar.getInstance().apply { timeInMillis = startMillis }
        val e = Calendar.getInstance().apply { timeInMillis = endMillis }
        var y = e.get(Calendar.YEAR) - s.get(Calendar.YEAR)
        var m = e.get(Calendar.MONTH) - s.get(Calendar.MONTH)
        var d = e.get(Calendar.DAY_OF_MONTH) - s.get(Calendar.DAY_OF_MONTH)
        if (d < 0) {
            m--
            val prev = Calendar.getInstance().apply {
                timeInMillis = endMillis
                add(Calendar.MONTH, -1)
            }
            d += prev.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (m < 0) {
            y--
            m += 12
        }
        if (y < 0) {
            y = 0
            m = 0
            d = 0
        }
        return DurationParts(y, m, d)
    }

    /** 指定时刻的周岁年龄；生日未设置或时刻早于生日时返回 -1 */
    fun ageAt(birthMillis: Long, atMillis: Long): Int {
        if (birthMillis <= 0 || atMillis < birthMillis) return -1
        val b = Calendar.getInstance().apply { timeInMillis = birthMillis }
        val a = Calendar.getInstance().apply { timeInMillis = atMillis }
        var age = a.get(Calendar.YEAR) - b.get(Calendar.YEAR)
        if (a.get(Calendar.MONTH) < b.get(Calendar.MONTH) ||
            (a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
                a.get(Calendar.DAY_OF_MONTH) < b.get(Calendar.DAY_OF_MONTH))
        ) {
            age--
        }
        return age
    }
}
