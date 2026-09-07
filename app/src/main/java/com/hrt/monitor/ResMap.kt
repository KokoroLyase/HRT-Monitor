package com.hrt.monitor

import com.hrt.monitor.data.EventType
import com.hrt.monitor.data.Hormone
import com.hrt.monitor.data.Status

/** 枚举 → 字符串资源的映射（放根包避免依赖 UI 层） */
fun Hormone.labelRes(): Int = when (this) {
    Hormone.E2 -> R.string.hormone_e2
    Hormone.T -> R.string.hormone_t
    Hormone.LH -> R.string.hormone_lh
    Hormone.FSH -> R.string.hormone_fsh
    Hormone.PRL -> R.string.hormone_prl
    Hormone.P4 -> R.string.hormone_p4
}

fun EventType.labelRes(): Int = when (this) {
    EventType.START_HRT -> R.string.event_type_start_hrt
    EventType.MED_CHANGE -> R.string.event_type_med_change
    EventType.DOCTOR_CHANGE -> R.string.event_type_doctor_change
    EventType.SURGERY -> R.string.event_type_surgery
    EventType.OTHER -> R.string.event_type_other
}

fun Status.labelRes(): Int = when (this) {
    Status.LOW -> R.string.low
    Status.IN -> R.string.in_range
    Status.HIGH -> R.string.high
    Status.UNKNOWN -> R.string.unknown
}
