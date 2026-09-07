package com.hrt.monitor.data

import java.io.Serializable

/** 六项性激素指标 */
enum class Hormone(val key: String) {
    E2("e2"),
    T("t"),
    LH("lh"),
    FSH("fsh"),
    PRL("prl"),
    P4("p4");

    val short: String get() = key.uppercase()

    companion object {
        fun byKey(key: String): Hormone? = entries.firstOrNull { it.key == key }
    }
}

/** 单次检测结果（保留用户录入时的数值与单位，显示/图表时再换算） */
data class Measurement(val hormone: Hormone, val value: Double, val unit: String) : Serializable

/** 一次化验记录：日期 + 可选备注 + 若干项检测值 */
data class LabRecord(
    val id: String,
    val dateMillis: Long,
    val note: String,
    val items: List<Measurement>
) : Serializable {
    fun measurementOf(h: Hormone): Measurement? = items.firstOrNull { it.hormone == h }
    fun baseValueOf(h: Hormone): Double? =
        measurementOf(h)?.let { Units.toBase(h, it.value, it.unit) }
}

/** 生活事件类型 */
enum class EventType(val key: String) {
    START_HRT("start_hrt"),
    MED_CHANGE("med_change"),
    DOCTOR_CHANGE("doctor_change"),
    SURGERY("surgery"),
    OTHER("other");

    companion object {
        fun byKey(key: String): EventType? = entries.firstOrNull { it.key == key }
    }
}

/** 生活事件时间线条目 */
data class LifeEvent(
    val id: String,
    val dateMillis: Long,
    val type: EventType,
    val note: String
) : Serializable

/**
 * 参考范围。内置范围通过 labelRes（资源名）本地化；自定义范围使用 label 文本。
 * thresholdOnly 表示只有下限的单边提示线（如 PRL 显著升高提示），图表中以虚线呈现。
 */
data class RefRange(
    val id: String,
    val hormone: Hormone,
    val labelRes: String? = null,
    val label: String? = null,
    val min: Double,
    val max: Double,
    val unit: String,
    val builtin: Boolean = false,
    val visible: Boolean = true,
    val thresholdOnly: Boolean = false
) : Serializable {
    fun minBase(): Double = Units.toBase(hormone, min, unit)
    fun maxBase(): Double = Units.toBase(hormone, max, unit)
}

/** 应用设置（随数据一起备份） */
data class Settings(
    val targets: Map<String, String> = emptyMap() // hormone.key -> 目标范围 id
) : Serializable

/** 全部应用数据 */
data class AppData(
    val records: List<LabRecord> = emptyList(),
    val events: List<LifeEvent> = emptyList(),
    val ranges: List<RefRange> = emptyList(),
    val settings: Settings = Settings()
) : Serializable {
    fun rangeById(id: String?): RefRange? = id?.let { rid -> ranges.firstOrNull { it.id == rid } }
    fun targetOf(h: Hormone): RefRange? = rangeById(settings.targets[h.key])
}

/** 相对参考范围的判定结果 */
enum class Status { LOW, IN, HIGH, UNKNOWN }

fun statusOf(baseValue: Double?, range: RefRange?): Status {
    if (baseValue == null || range == null) return Status.UNKNOWN
    val lo = range.minBase()
    val hi = range.maxBase()
    return when {
        range.thresholdOnly -> if (baseValue >= lo) Status.HIGH else Status.IN
        baseValue < lo -> Status.LOW
        baseValue > hi -> Status.HIGH
        else -> Status.IN
    }
}
