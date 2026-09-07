package com.hrt.monitor.data

import java.util.Locale

/** 单位定义：label + 换算到基准单位的系数（基准值 = 录入值 × factor） */
data class UnitDef(val label: String, val factorToBase: Double)

object Units {

    /** 各激素支持的单位。列表第一个为录入默认单位。 */
    val byHormone: Map<Hormone, List<UnitDef>> = mapOf(
        Hormone.E2 to listOf(
            UnitDef("pg/mL", 3.671),
            UnitDef("pmol/L", 1.0)
        ),
        Hormone.T to listOf(
            UnitDef("ng/mL", 1.0),
            UnitDef("ng/dL", 0.01),
            UnitDef("nmol/L", 0.28846)
        ),
        Hormone.LH to listOf(
            UnitDef("IU/L", 1.0),
            UnitDef("mIU/mL", 1.0)
        ),
        Hormone.FSH to listOf(
            UnitDef("IU/L", 1.0),
            UnitDef("mIU/mL", 1.0)
        ),
        Hormone.PRL to listOf(
            UnitDef("ng/mL", 1.0),
            UnitDef("µg/L", 1.0),
            UnitDef("mIU/L", 0.04717)
        ),
        Hormone.P4 to listOf(
            UnitDef("nmol/L", 1.0),
            UnitDef("ng/mL", 3.18)
        )
    )

    /** 基准单位：统一存储与图表比较所用的单位 */
    fun baseUnit(h: Hormone): String =
        byHormone.getValue(h).first { it.factorToBase == 1.0 }.label

    fun defaultUnit(h: Hormone): String = byHormone.getValue(h).first().label

    fun factor(h: Hormone, unit: String): Double =
        byHormone.getValue(h).firstOrNull { it.label == unit }?.factorToBase ?: 1.0

    /** 录入值 → 基准值 */
    fun toBase(h: Hormone, value: Double, unit: String): Double = value * factor(h, unit)

    /** 基准值 → 指定单位显示值 */
    fun fromBase(h: Hormone, base: Double, unit: String): Double = base / factor(h, unit)

    /** 智能格式化：按数量级保留合适的小数位，去除尾部零 */
    fun format(v: Double): String {
        if (v.isNaN() || v.isInfinite()) return "-"
        val abs = kotlin.math.abs(v)
        return when {
            abs >= 1000 -> String.format(Locale.US, "%.0f", v)
            abs >= 0.1 -> {
                val df = java.text.DecimalFormat("0.###", java.text.DecimalFormatSymbols(Locale.US))
                df.format(v)
            }
            else -> String.format(Locale.US, "%.3f", v)
        }
    }
}
