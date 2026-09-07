package com.hrt.monitor.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.hrt.monitor.data.EventType
import com.hrt.monitor.data.RefRange
import com.hrt.monitor.data.Units

fun EventType.icon(): ImageVector = when (this) {
    EventType.START_HRT -> Icons.Filled.Flag
    EventType.MED_CHANGE -> Icons.Filled.Medication
    EventType.DOCTOR_CHANGE -> Icons.Filled.Person
    EventType.SURGERY -> Icons.Filled.MedicalServices
    EventType.OTHER -> Icons.Filled.EventNote
}

/** 参考范围的显示名称（内置范围按当前语言本地化） */
@Composable
fun RefRange.displayLabel(): String {
    val custom = label
    if (custom != null) return custom
    val res = labelRes ?: return ""
    val context = LocalContext.current
    val id = context.resources.getIdentifier(res, "string", context.packageName)
    return if (id != 0) stringResource(id) else res
}

/** 范围的数值区间文本（按存储单位） */
fun RefRange.spanText(): String = if (thresholdOnly) {
    "≥ ${Units.format(min)} $unit"
} else {
    "${Units.format(min)} – ${Units.format(max)} $unit"
}
